package br.com.hbobenicio.mysimpleagent.tools;

import br.com.hbobenicio.mysimpleagent.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class ToolsLoader {
    private static final Logger logger = LoggerFactory.getLogger(ToolsLoader.class.getSimpleName());

    private final ObjectMapper objectMapper;

    public ToolsLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Object> loadToolsFromResources() {
        var toolsResourcePath = "/tools.json";
        String toolsString = ResourceUtils.loadResourceAsString(toolsResourcePath);

        List<Object> tools = this.objectMapper.readValue(toolsString, new TypeReference<>(){});
        logger.atDebug()
                .addKeyValue("toolsCount", tools.size())
                .addKeyValue("resourcePath", toolsResourcePath)
                .log("tool specs: loaded");

        return tools;
    }
}
