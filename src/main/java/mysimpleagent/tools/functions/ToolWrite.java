package mysimpleagent.tools.functions;

import mysimpleagent.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ToolWrite implements Tool {

    private record Args(String path, String content) {}

    private static final Logger logger = LoggerFactory.getLogger(ToolWrite.class.getSimpleName());

    private final ObjectMapper objectMapper;

    public ToolWrite(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "write";
    }

    @Override
    public String call(String argsString) {
        logger.atInfo()
                .addKeyValue("tool", getName())
                .addKeyValue("args", argsString)
                .log("tool executing...");
        Args args = objectMapper.readValue(argsString, Args.class);
        try {
            Files.writeString(Path.of(args.path), args.content);
            return "success";
        } catch (IOException e) {
            logger.atError()
                    .setCause(e)
                    .log("writing file failed");
            // should the LLM know all the root causes of it???
            return "error: " + e.toString();
        }
    }
}
