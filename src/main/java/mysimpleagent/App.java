package mysimpleagent;

import mysimpleagent.llm.LLMService;
import mysimpleagent.llm.chatcompletions.ChatResponse;
import mysimpleagent.llm.chatcompletions.LLMChatCompletionsStreamResponseParser;
import mysimpleagent.llm.chatcompletions.payloads.LLMChatCompletionMessage;
import mysimpleagent.tools.ToolService;
import mysimpleagent.tools.ToolsLoader;
import mysimpleagent.tools.Toolset;
import mysimpleagent.tools.functions.GetCurrentWeather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class.getSimpleName());

    static void main() {
        logger.atInfo().log("initializing...");

        var config = Config.loadFromEnv();

        // Jackson's JSON encoder/decoder
        var objectMapper = new ObjectMapper();

        // Tool Specs
        var toolsLoader = new ToolsLoader(objectMapper);
        List<Object> tools = toolsLoader.loadToolsFromResources();

        // Tools
        var getCurrentWeather = new GetCurrentWeather(objectMapper);
        var toolset =  new Toolset(
                getCurrentWeather
        );
        var toolService = new ToolService(toolset);

        var respParser = new LLMChatCompletionsStreamResponseParser(objectMapper);

        //NOTE HTTP2 streaming is different from HTTP Transfer-Encoding: chunk
        //     which is commonly used by some inference engines
        HttpClient llmClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        var llmService = new LLMService(llmClient, objectMapper, config, tools, respParser);
        List<LLMChatCompletionMessage> messages = llmService.newConversation();

        // Main loop
        while (true) {
            //TODO improve exception handling
            var input = new Scanner(System.in);
            System.out.print(">>> ");

            // is this really needed?
            System.out.flush();
            System.err.flush();

            final String prompt;
            try {
                prompt = input.nextLine().trim().toLowerCase();
            } catch (NoSuchElementException e) {
                logger.atDebug().setCause(e).log("EOF: no more lines do read");
                break;
            }

            if (prompt.isEmpty()) {
                continue;
            }

            if (prompt.equals("/exit") || prompt.equals("/quit")) {
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
                System.out.print("Tool Calls are pending. Allow? [Y/n] ");
                String answer = input.nextLine().trim().toLowerCase();
                boolean proceed = answer.isEmpty() || answer.equals("y") || answer.equals("yes");
                if (!proceed) {
                    break;
                }

                try {
                    toolService.callTools(messages, chatResponse.toolCalls());
                    chatResponse = llmService.chatToolsBack(messages);
                } catch (Exception e) {
                    //TODO improve this
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
