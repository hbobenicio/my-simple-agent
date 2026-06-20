package mysimpleagent.llm.chatcompletions.payloads;

public record LLMChatCompletionTool(
        String id,
        String type,
        LLMChatCompletionToolFunction function
){}
