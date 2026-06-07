package mysimpleagent.llm.chatcompletions.models;

public record LLMChatCompletionTool(
        String id,
        String type,
        LLMChatCompletionToolFunction function
){}
