package mysimpleagent.llm.chatcompletions.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMPromptTokensDetails(
        @JsonProperty("cached_tokens") int cachedTokens
) {
}
