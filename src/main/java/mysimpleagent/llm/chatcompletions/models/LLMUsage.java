package mysimpleagent.llm.chatcompletions.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMUsage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens,
        @JsonProperty("prompt_tokens_details") LLMPromptTokensDetails promptTokensDetails
) {
}
