package mysimpleagent.tools.functions;

import mysimpleagent.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ToolSkill implements Tool {

    /// Arguments of the `bash` tool
    ///
    /// @param path The SKILL.md file path
    private record Args(String path) {}

    private static final Logger logger = LoggerFactory.getLogger(ToolSkill.class.getSimpleName());

    private final ObjectMapper jsonObjectMapper;

    public ToolSkill(ObjectMapper jsonObjectMapper) {
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    public String getName() {
        return "skill";
    }

    @Override
    public String call(String argsString) {
        logger.atInfo()
                .addKeyValue("tool", getName())
                .addKeyValue("args", argsString)
                .log("tool executing...");
        ToolSkill.Args args = jsonObjectMapper.readValue(argsString, ToolSkill.Args.class);
        try {
            String fileContents = Files.readString(Path.of(args.path));
            //TODO consider truncating the frontmatter (it's already in the context...)
            return String.format("skill tool success: the content of the file \"%s\" is\n%s", args.path, fileContents);
        } catch (IOException e) {
            logger.atError()
                    .addKeyValue("tool", getName())
                    .addKeyValue("args", argsString)
                    .setCause(e)
                    .log("tool skill failed");
            // should the LLM know all the root causes of it???
            //TODO consider sending the full stacktrace back to the LLM
            return String.format("error: skill tool failed for path \"%s\": %s", args.path, e.getMessage());
        }
    }
}
