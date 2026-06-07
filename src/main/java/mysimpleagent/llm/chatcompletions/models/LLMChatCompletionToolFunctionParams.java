package mysimpleagent.llm.chatcompletions.models;

import java.util.Map;

public record LLMChatCompletionToolFunctionParams(
        String type,
        Map<String, Object> properties
){}
