package mysimpleagent.llm;

import mysimpleagent.Config;
import mysimpleagent.http.HttpValidator;
import mysimpleagent.llm.chatcompletions.ChatResponse;
import mysimpleagent.llm.chatcompletions.LLMChatCompletionsStreamResponseParser;
import mysimpleagent.llm.chatcompletions.TokenUsagePrinter;
import mysimpleagent.llm.chatcompletions.models.*;
import mysimpleagent.llm.chatcompletions.stream.ChatCompletionUsage;
import mysimpleagent.llm.chatcompletions.stream.LLMChatCompletionsStreamChoiceDeltaToolCall;
import mysimpleagent.llm.chatcompletions.stream.LLMChatCompletionsStreamChoiceDeltaToolCallFunction;
import mysimpleagent.llm.chatcompletions.stream.ChatCompletionStreamResponseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class.getSimpleName());

    /// dump is the file logger that dumps the chat completions streaming events for further analysis/troubleshooting.
    private static final Logger dump = LoggerFactory.getLogger("DUMP");

    private static final AttributedStyle REASONING_STYLE =
            AttributedStyle.DEFAULT.foreground(86, 95, 137);

    private static final AttributedStyle ASSISTANT_MESSAGE_STYLE =
            AttributedStyle.DEFAULT.foreground(255, 255, 255);

    private final HttpClient llmClient;
    private final ObjectMapper objectMapper;
    private final Config config;
    private final List<Object> tools;
    private final LLMChatCompletionsStreamResponseParser respParser;
    private final Terminal terminal;

    public LLMService(
            HttpClient llmClient,
            ObjectMapper objectMapper,
            Config config,
            Terminal terminal,
            List<Object> tools,
            LLMChatCompletionsStreamResponseParser respParser
    ) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.config = config;
        this.terminal = terminal;
        this.tools = tools;
        this.respParser = respParser;
    }

    public List<ChatCompletionMessageParam> newConversation() {
        var messages = new ArrayList<ChatCompletionMessageParam>();

        String systemPrompt = getSystemPrompt();
        var systemMessage = new ChatCompletionMessageParam.ChatCompletionSystemMessageParam(systemPrompt);
        messages.add(systemMessage);

        return messages;
    }

    public ChatResponse chat(List<ChatCompletionMessageParam> messages, String userPrompt) throws IOException, InterruptedException {
        messages.add(new ChatCompletionMessageParam.ChatCompletionUserMessageParam(userPrompt));
        return doChat(messages);
    }

    private ChatResponse doChat(List<ChatCompletionMessageParam> messages) throws IOException, InterruptedException {

        var payload = new ChatCompletionRequestPayload(
                getModelName(),
                true,  // stream
                new ChatCompletionStreamOptions(null, true),
                messages,
                this.tools
        );

        // HTTP Chat Completions Request
        HttpRequest request = createChatRequest(payload);

        logger.atDebug().log("blocking on sse request...");
        HttpResponse<Stream<String>> response = llmClient.send(request, HttpResponse.BodyHandlers.ofLines());
        HttpValidator.throwIfNotOk(response);

        var reasoningStringBuilder =  new StringBuilder();
        var messageStringBuilder = new StringBuilder();
        var toolArgsBuilder = new StringBuilder();
        LLMChatCompletionsStreamChoiceDeltaToolCall currentToolCall = null;
        List<LLMChatCompletionsStreamChoiceDeltaToolCall> toolCalls = new ArrayList<>();

        String currentRawStyle = "\u001B[" + REASONING_STYLE.toAnsi() + "m";
        String resetRawStyle = "\u001B[" + AttributedStyle.DEFAULT.toAnsi() + "m";
        // ensure current terminal style state is reset by the end of the streaming
        try {
            try (Stream<String> chunkStream = response.body()) {
                var firstReasoning = true;
                var firstContent = true;
                var firstToolCall = true;
                for (String chunkLine : (Iterable<String>) chunkStream::iterator) {
                    assert chunkLine != null;
                    dump.atInfo().log(chunkLine);
                    chunkLine = chunkLine.trim();

                    if (chunkLine.equals("data: [DONE]")) {
                        //                    break;
                        //NOTE we need to stop, but how to warn and drain the rest of the stream if there is more data to come?
                        continue;
                    }
                    ChatCompletionStreamResponseEvent event = respParser.parseEvent(chunkLine);
                    if (event == null) {
                        continue;
                    }

                    var choices = event.choices();
                    if (choices == null) {
                        logger.atWarn().log("streaming chunk response has no choices");
                        continue;
                    }
                    if (choices.isEmpty()) {
                        // the last chunk is the token usage info and it has no choices
                        if (event.usage() == null) {
                            logger.atWarn().log("streaming chunk response has no usage");
                        } else {
                            var tokenUsagePrinter = new TokenUsagePrinter(this.terminal);
                            tokenUsagePrinter.println(event.usage());
                        }
                        continue;
                    }
                    if (choices.size() > 1) {
                        logger.atWarn()
                                .addKeyValue("choicesSize", choices.size())
                                .log("more than one choice. only the first one will be considered");
                        // or should I break it?
                        continue;
                    }

                    var choice = choices.getFirst();
                    if (choice.finishReason() != null) {
                        // The model decided to finish the conversation

                        if (currentToolCall != null) {
                            // the model finished the response stream. Now we can resolve the final tool call args string

                            String previousToolFinalArgs = toolArgsBuilder.toString();
                            toolArgsBuilder.setLength(0);

                            var finalToolCall = new LLMChatCompletionsStreamChoiceDeltaToolCall(currentToolCall.index(), currentToolCall.id(), currentToolCall.type(), new LLMChatCompletionsStreamChoiceDeltaToolCallFunction(currentToolCall.function().name(), previousToolFinalArgs));
                            toolCalls.add(finalToolCall);
                        }

                        //TODO refactor this and improve this mess (maybe with a chunk parsing state machine...)

                        this.terminal.writer().printf("%n%n");
                        this.terminal.flush();
                        logger.atDebug()
                                .addKeyValue("finishReason", choice.finishReason())
                                .log("finished");

                        continue;
                    }
                    var delta = choice.delta();
                    if (delta.reasoningContent() != null) {
                        String reasoningChunk = delta.reasoningContent();
                        if (firstReasoning) {
                            firstReasoning = false;
//                            this.terminal.writer().print("REASONING: ");
                            this.terminal.writer().print(currentRawStyle);
                            this.terminal.flush();
                            reasoningChunk = reasoningChunk.stripLeading();
                        }
                        this.terminal.writer().print(reasoningChunk);
                        this.terminal.flush();
                        reasoningStringBuilder.append(reasoningChunk);
                    } else if (delta.content() != null) {
                        String content = delta.content();
                        if (firstContent) {
                            firstContent = false;
//                            this.terminal.writer().print("\nMESSAGE: ");
                            currentRawStyle = "\u001B[" + ASSISTANT_MESSAGE_STYLE.toAnsi() + "m";
                            this.terminal.writer().println();
                            this.terminal.writer().print(currentRawStyle);
                            this.terminal.flush();
                            content = content.stripLeading();
                        }
                        this.terminal.writer().print(content);
                        this.terminal.flush();
                        messageStringBuilder.append(content);
                    } else if (delta.toolCalls() != null) {
                        if (delta.toolCalls().size() > 1) {
                            logger.atWarn()
                                    .addKeyValue("toolCallsSize", delta.toolCalls().size())
                                    .log("unexpected toolCalls size");
                        }
                        var toolCall = delta.toolCalls().getFirst();
                        assert toolCall.type().equals("function");

                        // when id is not null, the server is starting the streaming of a new tool call
                        // if id is null, the server is streaming the arguments of it
                        if (toolCall.id() != null) {
                            if (firstToolCall) {
                                currentToolCall = toolCall;
                                firstToolCall = false;
//                                this.terminal.writer().println("TOOL CALLS:");
                                this.terminal.flush();
                            } else {
                                String previousToolFinalArgs = toolArgsBuilder.toString();
                                toolArgsBuilder.setLength(0);

                                var finalToolCall = new LLMChatCompletionsStreamChoiceDeltaToolCall(currentToolCall.index(), currentToolCall.id(), currentToolCall.type(), new LLMChatCompletionsStreamChoiceDeltaToolCallFunction(currentToolCall.function().name(), previousToolFinalArgs));
                                toolCalls.add(finalToolCall);
                                currentToolCall = toolCall;
                            }

                            this.terminal.writer().printf("[%s] \uD83D\uDD28 %s ", toolCall.id(), toolCall.function().name());
                            this.terminal.flush();
                        } else {
                            this.terminal.writer().print(toolCall.function().arguments());
                            this.terminal.flush();
                            toolArgsBuilder.append(toolCall.function().arguments());
                        }
                    }
                }
            }
            String finalReasoning = reasoningStringBuilder.toString();
            String finalMessage = messageStringBuilder.toString();
            // add the tool calls to the chat
            for (var toolCall : toolCalls) {
                //TODO improve this. Models are duplicated... deltas and chat message payloads
                List<LLMChatCompletionTool> chatToolCalls = new ArrayList<>();
                var chatToolCallMsg = new LLMChatCompletionTool(toolCall.id(), toolCall.type(), new LLMChatCompletionToolFunction(
                        toolCall.function().name(),
                        null,  // description... needed?
                        toolCall.function().arguments()
                ));
                chatToolCalls.add(chatToolCallMsg);
                var assistantMsg = new ChatCompletionMessageParam.ChatCompletionAssistantMessageParam(null, chatToolCalls);
                messages.add(assistantMsg);
            }
            return new ChatResponse(finalReasoning, finalMessage, toolCalls);
        } finally {
            // Ensure we reset the state of the terminal in any case
            this.terminal.writer().print(resetRawStyle);
            this.terminal.flush();
        }
    }

    private HttpRequest createChatRequest(ChatCompletionRequestPayload payload) {
        var uri = URI.create(this.config.getLlmBaseUrl() + "/chat/completions");

        String payloadStr = this.objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(payload);
        logger.atDebug()
                .addKeyValue("payload", payloadStr)
                .log("payload");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadStr))
                .timeout(Duration.ofMinutes(1))
                .build();
        logger.atDebug()
                .addKeyValue("method", request.method())
                .addKeyValue("url", uri)
                .log("request");

        return request;
    }

    public ChatResponse chatToolsBack(List<ChatCompletionMessageParam> messages) throws IOException, InterruptedException {
        // tool calls were already added to the chat conversation
        return doChat(messages);
    }

    public String getModelName() {
        return this.config.getLlmModelName();
    }

    public String getSystemPrompt() {
        return this.config.getLlmSystemPrompt();
    }
}
