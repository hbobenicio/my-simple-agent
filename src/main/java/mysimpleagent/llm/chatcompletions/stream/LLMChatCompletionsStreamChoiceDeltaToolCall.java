package mysimpleagent.llm.chatcompletions.stream;

public record LLMChatCompletionsStreamChoiceDeltaToolCall(
    Long index,

    // The ID of the tool call.
    String id,

    // The type of the tool. Currently, only function is supported.
    String type,

    LLMChatCompletionsStreamChoiceDeltaToolCallFunction function
){}
