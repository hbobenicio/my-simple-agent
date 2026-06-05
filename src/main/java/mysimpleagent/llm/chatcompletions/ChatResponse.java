package mysimpleagent.llm.chatcompletions;

import mysimpleagent.llm.chatcompletions.stream.LLMChatCompletionsStreamChoiceDeltaToolCall;

import java.util.List;

public record ChatResponse(
        String reasoning,
        String message,
        List<LLMChatCompletionsStreamChoiceDeltaToolCall> toolCalls
) {
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
