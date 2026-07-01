package br.com.hbobenicio.mysimpleagent.tools.functions;

import br.com.hbobenicio.mysimpleagent.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ToolRead implements Tool {

    private record Args(String path) {}

    private static final Logger logger = LoggerFactory.getLogger(ToolRead.class.getSimpleName());

    private final ObjectMapper objectMapper;

    public ToolRead(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "read";
    }

    @Override
    public String call(String argsString) {
        logger.atInfo()
                .addKeyValue("tool", getName())
                .addKeyValue("args", argsString)
                .log("tool executing...");
        Args args = objectMapper.readValue(argsString, Args.class);
        try {
            String fileContents = Files.readString(Path.of(args.path));
            return String.format("read tool success: the content of the file \"%s\" is\n%s", args.path, fileContents);
        } catch (IOException e) {
            logger.atError()
                    .addKeyValue("tool", getName())
                    .addKeyValue("args", argsString)
                    .setCause(e)
                    .log("tool read failed");
            // should the LLM know all the root causes of it???
            //TODO consider sending the full stacktrace back to the LLM
            return String.format("error: read tool failed for path \"%s\": %s", args.path, e.getMessage());
        }
    }
}
