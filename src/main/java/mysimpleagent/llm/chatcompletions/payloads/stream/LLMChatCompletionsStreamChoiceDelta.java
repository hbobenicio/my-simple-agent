package mysimpleagent.llm.chatcompletions.payloads.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LLMChatCompletionsStreamChoiceDelta(
        String role,
        String content,

        // This is not documented by OpenAI API Spec but LM Studio sends me like this
        @JsonProperty("reasoning_content")
        String reasoningContent,

        @JsonProperty("tool_calls")
        List<LLMChatCompletionsStreamChoiceDeltaToolCall> toolCalls,

        String refusal
) {
}
