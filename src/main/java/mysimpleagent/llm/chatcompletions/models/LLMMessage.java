package mysimpleagent.llm.chatcompletions.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LLMMessage(
        String role,
        String content,
        @JsonProperty("reasoning_content") String reasoningContent,
        @JsonProperty("tool_calls") List<LLMChatCompletionTool> toolCalls
) {
}
