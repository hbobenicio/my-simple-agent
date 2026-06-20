package mysimpleagent.llm.chatcompletions.payloads.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompletionUsageCompletionTokenDetails(
        @JsonProperty("reasoning_tokens")
        Integer reasoningTokens
) {}
