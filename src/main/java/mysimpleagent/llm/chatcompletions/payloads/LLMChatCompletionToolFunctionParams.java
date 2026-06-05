package mysimpleagent.llm.chatcompletions.payloads;

import java.util.Map;

public record LLMChatCompletionToolFunctionParams(
        String type,
        Map<String, Object> properties
){}
