package br.com.hbobenicio.mysimpleagent.tools;

import java.util.HashMap;
import java.util.Map;

public class Toolset {
    private final Map<String, Tool> tools = new HashMap<>();

    public Toolset(Tool... tools) {
        for (var tool: tools) {
            this.tools.put(tool.getName(), tool);
        }
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public Map<String, Tool> getTools() {
        return tools;
    }
}
