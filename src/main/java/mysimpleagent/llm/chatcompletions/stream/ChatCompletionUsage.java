package mysimpleagent.llm.chatcompletions.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

// Example of a payload:
// {"prompt_tokens":11,"completion_tokens":293,"total_tokens":304,"completion_tokens_details":{"reasoning_tokens":266}}

public record ChatCompletionUsage(
    @JsonProperty("prompt_tokens")
    Integer promptTokens,

    @JsonProperty("completion_tokens")
    Integer completionTokens,

    @JsonProperty("total_tokens")
    Integer totalTokens,

    @JsonProperty("completion_tokens_details")
    ChatCompletionStreamResponseEventUsageTokenDetails completionTokensDetails
) {
}

