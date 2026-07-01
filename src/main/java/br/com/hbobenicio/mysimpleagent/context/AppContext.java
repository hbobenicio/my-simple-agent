package br.com.hbobenicio.mysimpleagent.context;

import br.com.hbobenicio.mysimpleagent.config.Config;
import org.jline.terminal.Terminal;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;

/// The application context that holds references to most used objects
/// which should live throughout the program lifetime.
///
/// This should be kept minimum because
public class AppContext {
    private ExecutorService executor;
    private Config config;
    private ObjectMapper jsonObjectMapper;
    private ObjectMapper yamlObjectMapper;
    private Terminal terminal;
    private List<String> llmModelList;
    private String selectedModelName;

    public AppContext() {}

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public ObjectMapper getJsonObjectMapper() {
        return jsonObjectMapper;
    }

    public void setJsonObjectMapper(ObjectMapper jsonObjectMapper) {
        this.jsonObjectMapper = jsonObjectMapper;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public List<String> getLlmModelList() {
        return llmModelList;
    }

    public void setLlmModelList(List<String> llmModelList) {
        this.llmModelList = llmModelList;
    }

    public String getSelectedModelName() {
        return selectedModelName;
    }

    public void setSelectedModelName(String selectedModelName) {
        this.selectedModelName = selectedModelName;
    }

    public ObjectMapper getYamlObjectMapper() {
        return yamlObjectMapper;
    }

    public void setYamlObjectMapper(ObjectMapper yamlObjectMapper) {
        this.yamlObjectMapper = yamlObjectMapper;
    }
}
