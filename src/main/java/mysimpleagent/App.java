package mysimpleagent;

import mysimpleagent.acp.AcpServer;
import mysimpleagent.config.Config;
import mysimpleagent.context.AppContext;
import mysimpleagent.repl.Repl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class.getSimpleName());

    private static final AppContext CONTEXT = new AppContext();

    public static AppContext getContext() {
        return CONTEXT;
    }

    static void main(String[] args) {
        try {
            run(args);
        } catch (Throwable e) {
            // unhandled exceptions
            var rc = 1;
            logger.atError()
                    .addKeyValue("rc", rc)
                    .setCause(e)
                    .log("program exited with error");
            System.exit(rc);
        }
    }

    private static void run(String[] args) {
        // CLI Parsing
        logger.atDebug().log("cli args parsing..");
        if (args.length == 0) {
            usagePrint();
            return;
        }
        if (args.length > 1) {
            badArgs();
        }
        var cmd = args[0];
        logger.atDebug()
                .addKeyValue("cmd", cmd)
                .log("cli args has been parsed successfully");

        switch (cmd) {
            case "help", "-h", "--help", "-help" -> usagePrint();
            case "acp" -> runAcp();
            case "repl" -> runRepl();
            default -> badArgs();
        }
    }

    private static void runAcp() {
        commonStart();
        try (ExecutorService executor = createVirtualThreadExecutorService()) {
            CONTEXT.setExecutor(executor);
            executor.execute(App::acpMainTask);
        } finally {
            CONTEXT.setExecutor(null);
        }
    }

    private static void runRepl() {
        commonStart();
        try (ExecutorService executor = createVirtualThreadExecutorService()) {
            CONTEXT.setExecutor(executor);
            executor.execute(App::replMainTask);
        } finally {
            CONTEXT.setExecutor(null);
        }
    }

    private static void commonStart() {
        Config config = Config.loadFromEnv();
        CONTEXT.setConfig(config);

        // Jackson's JSON encoder/decoder
        var objectMapper = new ObjectMapper();
        CONTEXT.setObjectMapper(objectMapper);
    }

    private static ExecutorService createVirtualThreadExecutorService() {
        ThreadFactory vthreadFactory = Thread.ofVirtual()
                .name("vthread-", 0)
                .factory();
        return Executors.newThreadPerTaskExecutor(vthreadFactory);
    }

    private static void acpMainTask() {
        var acpServer = new AcpServer();
        acpServer.run();
    }

    private static void replMainTask() {
        var repl = new Repl();
        repl.run();
    }

    private static void usagePrint() {
        System.out.println("Usage: java [JAVA_OPTS] -jar my-simple-agent.jar <CMD>");
        System.out.println();
        System.out.println("CMD:  help | repl | acp - Root command");
    }

    private static void badArgs() {
        logger.atError().log("bad args");
        usagePrint();
        System.exit(1);
    }
}
