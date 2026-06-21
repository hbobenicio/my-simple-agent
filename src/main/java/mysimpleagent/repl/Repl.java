package mysimpleagent.repl;

import mysimpleagent.App;
import mysimpleagent.config.Config;
import mysimpleagent.llm.LLMService;
import mysimpleagent.llm.chatcompletions.ChatResponse;
import mysimpleagent.llm.chatcompletions.LLMChatCompletionsStreamResponseParser;
import mysimpleagent.llm.chatcompletions.payloads.ChatCompletionMessageParam;
import mysimpleagent.skills.SkillSpec;
import mysimpleagent.skills.SkillsService;
import mysimpleagent.tools.ToolService;
import mysimpleagent.tools.ToolsLoader;
import mysimpleagent.tools.Toolset;
import mysimpleagent.tools.functions.ToolBash;
import mysimpleagent.tools.functions.ToolRead;
import mysimpleagent.tools.functions.ToolWrite;
import org.jline.prompt.ConfirmResult;
import org.jline.prompt.ListBuilder;
import org.jline.prompt.Prompter;
import org.jline.prompt.PrompterFactory;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class Repl implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Repl.class.getSimpleName());

    public Repl() {}

    public void run() {
        try (Terminal terminal = terminalCreate()) {
            App.getContext().setTerminal(terminal);
            terminal.puts(InfoCmp.Capability.enter_ca_mode);
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();

            logger.atDebug()
                    .addKeyValue("class", terminal.getClass().getSimpleName())
                    .addKeyValue("type", terminal.getType())
                    .log("terminal provider autoconfigured");

            //NOTE LineReader is not thread-safe
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Prompter is the object capable of inquiring the user interactively for choices
            //
            // NOTE Prompter instances are thread-safe for reading configuration, but
            // prompt execution should be performed on a single thread due to terminal I/O constraints
            Prompter prompter = PrompterFactory.create(terminal);

            // Tool Specs
            var toolsLoader = new ToolsLoader(App.getContext().getObjectMapper());
            List<Object> tools = toolsLoader.loadToolsFromResources();

            // Tools Implementors
            var toolWrite = new ToolWrite(App.getContext().getObjectMapper());
            var toolRead = new ToolRead(App.getContext().getObjectMapper());
            var toolBash = new ToolBash(App.getContext().getObjectMapper());
            var toolset = new Toolset(toolWrite, toolRead, toolBash);
            var toolService = new ToolService(toolset);

            // Skills
            var skillsService = new SkillsService();
            List<SkillSpec> skillSpecs = skillsService.loadAll();

            var respStreamParser = new LLMChatCompletionsStreamResponseParser(App.getContext().getObjectMapper());

            HttpClient llmHttpClient = llmHttpClientCreate();

            var llmService = new LLMService(llmHttpClient, tools, respStreamParser);
            selectModel(llmService, App.getContext().getConfig(), prompter);

            List<ChatCompletionMessageParam> messages = llmService.newConversation();

            // Main Agentic Loop
            while (true) {
                var ps1 = String.format("[%s] >>> ", App.getContext().getSelectedModelName());

                //TODO improve exception handling
                final String prompt;
                try {
                    prompt = reader.readLine(ps1).trim();
                } catch (UserInterruptException e) {
                    // user pressed ctrl+c
                    // intentionally ignoring the exception
                    logger.atDebug().log("ctrl-c captured");
                    continue;
                } catch (EndOfFileException e) {
                    // user pressed ctrl+d
                    // intentionally ignoring the exception
                    logger.atDebug().log("ctrl-d captured. stopping the application..");
                    break;
                }

                //TODO improve slash commands parsing

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

                if (prompt.equals("/models")) {
                    selectModel(llmService, App.getContext().getConfig(), prompter);
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
                        //TODO check if we need to add something in the chat if the tool use is disallowed
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
        } catch (IOException | InterruptedException e) {
            //TODO improve this?
            throw new RuntimeException(e);
        } finally {
            App.getContext().setTerminal(null);

            // Note: The try-with-resources block automatically calls terminal.close(),
            // but explicitly emitting exit_ca_mode handles clean UI restoration.
            System.out.print("\033[?1049l"); // Standard fallback ANSI escape to leave alt screen
            System.out.flush();
        }
    }

    private static void selectModel(LLMService llmService, Config config, Prompter prompter) throws IOException, InterruptedException {
        List<String> llmModelList = llmService.modelsList();
        App.getContext().setLlmModelList(llmModelList);

        if (config.getLlmModelName().isEmpty()) {
            String selectedModelName = promptForModelName(prompter, llmModelList);
            App.getContext().setSelectedModelName(selectedModelName);
        } else {
            App.getContext().setSelectedModelName(config.getLlmModelName().get());
        }
    }

    private static String promptForModelName(Prompter prompter, List<String> llmModelList) throws IOException {
        var promptName = "modelChoice";
        ListBuilder b = prompter.newBuilder()
                .createListPrompt()
                .name(promptName)
                .message("Pick a model");
        for (var model : llmModelList) {
            b = b.newItem(model).text(model).add();
        }
        var promptResults = prompter.prompt(Collections.emptyList(), b.addPrompt().build());

//        return (ChoiseResult) promptResults.get(promptName);
        return promptResults.get(promptName).getResult();
    }

    private static HttpClient llmHttpClientCreate() {
        //NOTE HTTP2 streaming is different from HTTP Transfer-Encoding: chunk
        //     which is commonly used by some inference engines
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private static ConfirmResult promptToConfirmToolExecution(Prompter prompter) throws IOException {
        var promptName = "confirm";
        var promptResults = prompter.prompt(Collections.emptyList(), prompter.newBuilder()
                .createConfirmPrompt()
                .name(promptName)
                .message("Confirm the execution of the tool above?")
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
