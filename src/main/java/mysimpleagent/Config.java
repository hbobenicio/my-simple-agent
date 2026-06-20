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
        logger.atDebug().log("loading configs from env..");

        var cfg = new Config();
        cfg.llmBaseUrl = Optional.ofNullable(System.getenv("LLM_BASE_URL"))
                .map(String::trim)
                .orElse("http://localhost:12434/v1");
        logger.atDebug()
                .addKeyValue("LLM_BASE_URL", cfg.llmBaseUrl)
                .log("value loaded");

        String maybeModelName = System.getenv("LLM_MODEL_NAME");
        if (maybeModelName != null) {
            cfg.llmModelName = maybeModelName.trim();
            logger.atDebug()
                    .addKeyValue("LLM_MODEL_NAME", cfg.llmModelName)
                    .log("value loaded");
        }

        //TODO This is not a config. It's a resource that should be loaded at startup and should be
        //     saved in the global application scope (or be restricted to the LLMService)
        cfg.llmSystemPrompt = ResourceUtils.loadResourceAsString("/system.prompt.txt");
        logger.atDebug()
                .addKeyValue("systemPromptSize", cfg.llmSystemPrompt.length())
                .log("system prompt loaded");

        return cfg;
    }

    public String getLlmBaseUrl() {
        return llmBaseUrl;
    }

    public Optional<String> getLlmModelName() {
        return Optional.ofNullable(llmModelName);
    }

    public void setLlmModelName(String llmModelName) {
        this.llmModelName = llmModelName;
    }

    public String getLlmSystemPrompt() {
        return llmSystemPrompt;
    }
}
