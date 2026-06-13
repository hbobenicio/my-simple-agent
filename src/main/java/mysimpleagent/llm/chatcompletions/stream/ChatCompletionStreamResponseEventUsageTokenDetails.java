package mysimpleagent.llm.chatcompletions.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatCompletionStreamResponseEventUsageTokenDetails(
        @JsonProperty("reasoning_tokens")
        Integer reasoningTokens
) {}
