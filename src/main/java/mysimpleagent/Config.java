package mysimpleagent;

import mysimpleagent.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class.getSimpleName());

    private String llmBaseUrl;
    private String llmModelName;
    private String llmSystemPrompt;

    public static Config loadFromEnv() {
        var cfg = new Config();
        cfg.llmBaseUrl = Optional.ofNullable(System.getenv("LLM_BASE_URL"))
                .map(String::trim)
                .orElse("http://localhost:12434/v1");
        logger.atDebug()
                .addKeyValue("LLM_BASE_URL", cfg.llmBaseUrl)
                .log("value loaded");

        cfg.llmModelName = Optional.ofNullable(System.getenv("LLM_MODEL_NAME"))
                .map(String::trim)
                .orElse("qwen/qwen3-4b");
        logger.atDebug()
                .addKeyValue("LLM_MODEL_NAME", cfg.llmModelName)
                .log("value loaded");

        //TODO make this a template. render it with env context info
        cfg.llmSystemPrompt = ResourceUtils.loadResourceAsString("/system.prompt.txt");
        logger.atDebug()
                .addKeyValue("systemPromptSize", cfg.llmSystemPrompt.length())
                .log("system prompt loaded");
        return cfg;
    }

    public String getLlmBaseUrl() {
        return llmBaseUrl;
    }

    public String getLlmModelName() {
        return llmModelName;
    }

    public String getLlmSystemPrompt() {
        return llmSystemPrompt;
    }
}
