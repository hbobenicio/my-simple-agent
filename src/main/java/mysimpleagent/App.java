package mysimpleagent;

import mysimpleagent.acp.AcpServer;
import mysimpleagent.context.AppContext;
import mysimpleagent.llm.LLMService;
import mysimpleagent.llm.chatcompletions.ChatResponse;
import mysimpleagent.llm.chatcompletions.LLMChatCompletionsStreamResponseParser;
import mysimpleagent.llm.chatcompletions.payloads.ChatCompletionMessageParam;
import mysimpleagent.skills.SkillsService;
import mysimpleagent.tools.ToolService;
import mysimpleagent.tools.ToolsLoader;
import mysimpleagent.tools.Toolset;
import mysimpleagent.tools.functions.ToolBash;
import mysimpleagent.tools.functions.ToolRead;
import mysimpleagent.tools.functions.ToolWrite;
import org.jline.prompt.*;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class.getSimpleName());

    private static final AppContext CONTEXT = new AppContext();

    static void main(String[] args) {
        // CLI Parsing
        if (args.length == 0) {
            usagePrint();
            return;
        }
        if (args.length > 1) {
            badArgs();
        }

        var cmd = args[0];

        logger.atDebug().log("initializing..");
        try {
            switch (cmd) {
                case "help" -> usagePrint();
                case "acp" -> runAcp();
                case "repl" -> runRepl();
                default -> badArgs();
            }
        } catch (Exception e) {
            var rc = 1;
            logger.atError()
                    .addKeyValue("rc", rc)
                    .setCause(e)
                    .log("program exited with error");
            System.exit(rc);
        }
    }

    private static void runAcp() {
        new AcpServer().run();
    }

    private static void badArgs() {
        logger.atError().log("bad args");
        usagePrint();
        System.exit(1);
    }

    private static void runRepl() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("vthread-", 0)
                .factory();
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory)) {
            CONTEXT.setExecutor(executor);
            executor.execute(App::mainTask);
        } finally {
            CONTEXT.setExecutor(null);
        }
    }

    private static void mainTask() {
        Config config = Config.loadFromEnv();
        CONTEXT.setConfig(config);

        // Jackson's JSON encoder/decoder
        var objectMapper = new ObjectMapper();
        CONTEXT.setObjectMapper(objectMapper);

        var repl = new Repl();
        repl.run();

        try (Terminal terminal = terminalCreate()) {
            CONTEXT.setTerminal(terminal);
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
            var toolsLoader = new ToolsLoader(objectMapper);
            List<Object> tools = toolsLoader.loadToolsFromResources();

            // Tools Implementors
            var toolWrite = new ToolWrite(objectMapper);
            var toolRead = new ToolRead(objectMapper);
            var toolBash = new ToolBash(objectMapper);
            var toolset = new Toolset(toolWrite, toolRead, toolBash);
            var toolService = new ToolService(toolset);

            // Skills
            var skillsService = new SkillsService();
            skillsService.loadAll();

            var respStreamParser = new LLMChatCompletionsStreamResponseParser(objectMapper);

            HttpClient llmHttpClient = llmHttpClientCreate();

            var llmService = new LLMService(llmHttpClient, tools, respStreamParser);
            selectModel(llmService, config, prompter);

            List<ChatCompletionMessageParam> messages = llmService.newConversation();

            // Main Agentic Loop
            while (true) {
                var ps1 = String.format("[%s] >>> ", CONTEXT.getSelectedModelName());

                //TODO improve exception handling
                final String prompt;
                try {
                    prompt = reader.readLine(ps1).trim();
                } catch (UserInterruptException e) {
                    // user pressed ctrl+c
                    logger.atDebug().setCause(e).log("ctrl-c captured");
                    continue;
                } catch (EndOfFileException e) {
                    logger.atDebug().setCause(e).log("ctrl-d captured");
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
                    selectModel(llmService, config, prompter);
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
            CONTEXT.setTerminal(null);

            // Note: The try-with-resources block automatically calls terminal.close(),
            // but explicitly emitting exit_ca_mode handles clean UI restoration.
            System.out.print("\033[?1049l"); // Standard fallback ANSI escape to leave alt screen
            System.out.flush();
        }
    }

    private static void selectModel(LLMService llmService, Config config, Prompter prompter) throws IOException, InterruptedException {
        List<String> llmModelList = llmService.modelsList();
        CONTEXT.setLlmModelList(llmModelList);

        if (config.getLlmModelName().isEmpty()) {
            String selectedModelName = promptForModelName(prompter, llmModelList);
            CONTEXT.setSelectedModelName(selectedModelName);
        } else {
            CONTEXT.setSelectedModelName(config.getLlmModelName().get());
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

    private static void usagePrint() {
        System.out.println("Usage: java [JAVA_OPTS] -jar my-simple-agent.jar <CMD>");
        System.out.println();
        System.out.println("CMD:  help | repl | acp - Root command");
    }

    public static AppContext getContext() {
        return CONTEXT;
    }
}
