package mysimpleagent.llm;

import mysimpleagent.Config;
import mysimpleagent.http.HttpValidator;
import mysimpleagent.llm.chatcompletions.ChatResponse;
import mysimpleagent.llm.chatcompletions.LLMChatCompletionsStreamResponseParser;
import mysimpleagent.llm.chatcompletions.models.*;
import mysimpleagent.llm.chatcompletions.stream.LLMChatCompletionsStreamChoiceDeltaToolCall;
import mysimpleagent.llm.chatcompletions.stream.LLMChatCompletionsStreamChoiceDeltaToolCallFunction;
import mysimpleagent.llm.chatcompletions.stream.ChatCompletionStreamResponseEvent;
import org.jline.terminal.Terminal;
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
    private static final Logger dump = LoggerFactory.getLogger("DUMP");

    private final HttpClient llmClient;
    private final ObjectMapper objectMapper;
    private final Config config;
    private final List<Object> tools;
    private final LLMChatCompletionsStreamResponseParser respParser;
    private final Terminal terminal;

    public LLMService(HttpClient llmClient, ObjectMapper objectMapper, Config config, Terminal terminal, List<Object> tools, LLMChatCompletionsStreamResponseParser respParser) {
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
                new ChatCompletionStreamOptions(null, true),  //NOTE LM-Studio doesnt support this ATM
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

        try (Stream<String> chunkStream = response.body()) {
            var firstReasoning = true;
            var firstContent = true;
            var firstToolCall = true;
            for (String chunkLine: (Iterable<String>) chunkStream::iterator) {
                dump.atInfo().log(chunkLine);
                ChatCompletionStreamResponseEvent event = respParser.parseEvent(chunkLine);
                if (event == null) {
                    continue;
                }

                var choices = event.choices();
                if (choices == null || choices.isEmpty()) {
                    logger.atWarn().log("streaming chunk response has no choices");
                    continue;
                }
                if (choices.size() != 1) {
                    logger.atError()
                            .addKeyValue("choicesSize", choices.size())
                            .log("unexpected choices size");
                    // or should I break it?
                    continue;
                }

                var choice = choices.getFirst();
                if (choice.finishReason() != null) {
                    // The model decided to finish the conversation

                    if (currentToolCall != null) {
                        String previousToolFinalArgs = toolArgsBuilder.toString();
                        toolArgsBuilder.setLength(0);

                        var finalToolCall = new LLMChatCompletionsStreamChoiceDeltaToolCall(currentToolCall.index(), currentToolCall.id(), currentToolCall.type(), new LLMChatCompletionsStreamChoiceDeltaToolCallFunction(currentToolCall.function().name(), previousToolFinalArgs));
                        toolCalls.add(finalToolCall);
                    }

                    this.terminal.writer().printf("%n%n");
                    this.terminal.flush();
                    logger.atInfo()
                            .addKeyValue("finishReason", choice.finishReason())
                            .log("finished");
                    break;
                }
                var delta = choice.delta();
                if (delta.reasoningContent() != null) {
                    if (firstReasoning) {
                        firstReasoning = false;
                        this.terminal.writer().print("REASONING: ");
                        this.terminal.flush();
                    }
                    this.terminal.writer().print(delta.reasoningContent());
                    this.terminal.flush();
                    reasoningStringBuilder.append(delta.reasoningContent());
                } else if (delta.content() != null) {
                    if (firstContent) {
                        firstContent = false;
                        this.terminal.writer().print("\nMESSAGE: ");
                        this.terminal.flush();
                    }
                    this.terminal.writer().print(delta.content());
                    this.terminal.flush();
                    messageStringBuilder.append(delta.content());
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
                            this.terminal.writer().println("TOOL CALLS:");
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
        // add the tool calls to the chat history
        for (var toolCall: toolCalls) {
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
        // tool calls were already being added to the chat conversation history
        return doChat(messages);
    }

    public String getModelName() {
        return this.config.getLlmModelName();
    }

    public String getSystemPrompt() {
        return this.config.getLlmSystemPrompt();
    }
}
