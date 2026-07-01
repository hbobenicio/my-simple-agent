package br.com.hbobenicio.mysimpleagent.llm;

import java.util.Map;

public record ToolCall(
        String name, Map<String, Object> arguments
) {}
