package mysimpleagent.config;

import mysimpleagent.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class.getSimpleName());

    private String llmBaseUrl;

    private String llmModelName;

    private String llmSystemPrompt;

    private String llmApiKey;

    public static Config loadFromEnv() {
        logger.atDebug().log("loading configs from env..");

        var cfg = new Config();

        cfg.llmBaseUrl = load("LLM_BASE_URL", "http://localhost:12434/v1", false);
        cfg.llmModelName = load("LLM_MODEL_NAME", null, false);
        cfg.llmApiKey = load("LLM_API_KEY", null, true);

        //TODO This is not a config. It's an application scoped resource that should be loaded at startup
        //     Move this to another place
        cfg.llmSystemPrompt = ResourceUtils.loadResourceAsString("/system.prompt.txt");

        return cfg;
    }

    private static String load(String key, String defaultValue, boolean redacted) {
        Optional<String> maybeValue = Optional.ofNullable(System.getProperty(key))
                .filter(s -> !s.isBlank())
                .map(String::trim);

        maybeValue.ifPresent(value -> logValueLoadedEvent(key, value, redacted));

        return maybeValue.orElse(defaultValue);
    }

    private static void logValueLoadedEvent(String key, String value, boolean redacted) {
        var logBuilder = logger.atDebug().addKeyValue("key", key);

        if (redacted) {
            logBuilder = logBuilder.addKeyValue("value", "<REDACTED>");
        } else {
            logBuilder = logBuilder.addKeyValue("value", value);
        }

        logBuilder.log("value loaded");
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

    public Optional<String> getLlmApiKey() {
        return Optional.ofNullable(llmApiKey);
    }
}
