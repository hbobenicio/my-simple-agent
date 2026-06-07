package mysimpleagent;

import mysimpleagent.llm.LLMService;
import mysimpleagent.llm.chatcompletions.ChatResponse;
import mysimpleagent.llm.chatcompletions.LLMChatCompletionsStreamResponseParser;
import mysimpleagent.llm.chatcompletions.models.ChatCompletionMessageParam;
import mysimpleagent.tools.ToolService;
import mysimpleagent.tools.ToolsLoader;
import mysimpleagent.tools.Toolset;
import mysimpleagent.tools.functions.ToolBash;
import mysimpleagent.tools.functions.ToolRead;
import mysimpleagent.tools.functions.ToolWrite;
import org.jline.prompt.ConfirmResult;
import org.jline.prompt.Prompter;
import org.jline.prompt.PrompterFactory;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class.getSimpleName());

    static void main() {
        logger.atDebug().log("initializing..");

        Config config = Config.loadFromEnv();

        // Jackson's JSON encoder/decoder
        var objectMapper = new ObjectMapper();

        // Tool Specs
        var toolsLoader = new ToolsLoader(objectMapper);
        List<Object> tools = toolsLoader.loadToolsFromResources();

        // Tools
        var toolWrite = new ToolWrite(objectMapper);
        var toolRead = new ToolRead(objectMapper);
        var toolBash = new ToolBash(objectMapper);
        var toolset = new Toolset(toolWrite, toolRead, toolBash);
        var toolService = new ToolService(toolset);

        var respParser = new LLMChatCompletionsStreamResponseParser(objectMapper);

        //NOTE HTTP2 streaming is different from HTTP Transfer-Encoding: chunk
        //     which is commonly used by some inference engines
        HttpClient llmClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        //TODO check if we need a shutdown hook to close the terminal and restore user terminal
        //     in case of a signal has been triggered.
        try (Terminal terminal = terminalCreate()) {
            logger.atDebug()
                    .addKeyValue("class", terminal.getClass().getSimpleName())
                    .addKeyValue("type", terminal.getType())
                    .log("terminal provider autoconfigured");

            //NOTE LineReader is not thread-safe
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            Prompter prompter = PrompterFactory.create(terminal);

            var llmService = new LLMService(llmClient, objectMapper, config, terminal, tools, respParser);

            List<ChatCompletionMessageParam> messages = llmService.newConversation();

            // Main loop
            while (true) {
                //TODO improve exception handling
                final String prompt;
                try {
                    prompt = reader.readLine(">>> ").trim();
                } catch (UserInterruptException e) {
                    // user pressed ctrl+c
                    logger.atDebug().setCause(e).log("ctrl-c captured");
                    continue;
                } catch (EndOfFileException e) {
                    logger.atDebug().setCause(e).log("ctrl-d captured");
                    break;
                }

                if (prompt.isEmpty()) {
                    continue;
                }

                if (prompt.equals("/exit") || prompt.equals("/quit") || prompt.equals("/q")) {
                    break;
                }

                if (prompt.equals("/new") || prompt.equals("/clear")) {
                    messages = llmService.newConversation();
                    continue;
                }

                ChatResponse chatResponse;
                try {
                    chatResponse = llmService.chat(messages, prompt);
                } catch (Exception e) {
                    logger.atError().setCause(e).log("chat failed");
                    continue;
                }

                //TODO implement loop limit
                while (chatResponse.hasToolCalls()) {
                    ConfirmResult confirmResult = promptToConfirmToolExecution(prompter);
                    if (!confirmResult.isConfirmed()) {
                        break;
                    }

                    try {
                        toolService.callTools(messages, chatResponse.toolCalls());
                        chatResponse = llmService.chatToolsBack(messages);
                    } catch (Exception e) {
                        //TODO improve this?
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            //TODO improve this?
            throw new RuntimeException(e);
        }
    }

    private static ConfirmResult promptToConfirmToolExecution(Prompter prompter) throws IOException {
        var promptName = "confirm";
        var promptResults = prompter.prompt(Collections.emptyList(), prompter.newBuilder()
                .createConfirmPrompt()
                .name(promptName)
                .message("Confirm the execution of all the tools above?")
                .defaultValue(true)
                .addPrompt()
                .build());
        return (ConfirmResult) promptResults.get(promptName);
    }

    private static Terminal terminalCreate() {
        try {
            return TerminalBuilder.builder().system(true).build();
        } catch (IOException e) {
            logger.atError().setCause(e).log("terminal creation failed");
            System.exit(1);
            return null;
        }
    }
}
