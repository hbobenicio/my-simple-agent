package mysimpleagent.tools.functions;

import mysimpleagent.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class ToolBash implements Tool {

    /// Arguments of the `bash` tool
    ///
    /// @param command Bash command to execute
    /// @param timeout Timeout in seconds (optional, no default timeout)
    private record Args(
            String command,
            Long timeout
    ) {
    }

    private static final Logger logger = LoggerFactory.getLogger(ToolBash.class.getSimpleName());

    private final ObjectMapper objectMapper;

    public ToolBash(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "bash";
    }

    //TODO refactor this to accept and Object instead.
    @Override
    public String call(String argsString) {
        logger.atInfo()
                .addKeyValue("tool", getName())
                .addKeyValue("args", argsString)
                .log("tool executing...");
        Args args = objectMapper.readValue(argsString, Args.class);

        // args.command.split(" ") ?
        var processBuilder = new ProcessBuilder("bash", "-c", args.command);
        var sb = new StringBuilder();
        try {
            // Start the process
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String stdout = sb.toString();

            sb.setLength(0);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String stderr = sb.toString();

            // Wait for the process to complete and get the exit code
            int exitCode = process.waitFor();
            logger.atInfo()
                    .addKeyValue("tool", getName())
                    .addKeyValue("exitCode", exitCode)
                    .log("tool: exited");

            sb.setLength(0);
            sb.append("command ");
            sb.append(exitCode == 0 ? "succeeded" : "failed");
            sb.append(" with exit code ");
            sb.append(exitCode);
            sb.append(". The process stdout and stderr follows inside xml tags:\n");

            sb.append("<STDOUT>\n");
            sb.append(stdout);
            sb.append("</STDOUT>\n");

            sb.append("<STDERR>\n");
            sb.append(stderr);
            sb.append("</STDERR>\n");

            return sb.toString();

        } catch (IOException | InterruptedException e) {
            logger.atError()
                    .addKeyValue("tool", getName())
                    .setCause(e)
                    .log("tool: failed");
            return "command failed with uknown error";
        }
    }
}
