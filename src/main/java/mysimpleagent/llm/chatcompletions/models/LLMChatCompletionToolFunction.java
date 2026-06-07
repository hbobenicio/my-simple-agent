package mysimpleagent.llm.chatcompletions.models;

public record LLMChatCompletionToolFunction(
        String name,
        String description,
//        LLMChatCompletionToolFunctionParams parameters
        String arguments
){}
