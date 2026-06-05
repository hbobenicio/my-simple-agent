package mysimpleagent.llm.chatcompletions.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMTimings(
        @JsonProperty("cache_n") int cacheN,
        @JsonProperty("prompt_n") int promptN,
        @JsonProperty("prompt_ms") double promptMs,
        @JsonProperty("prompt_per_token_ms") double promptPerTokenMs,
        @JsonProperty("prompt_per_second") double promptPerSecond,
        @JsonProperty("predicted_n") int predictedN,
        @JsonProperty("predicted_ms") double predictedMs,
        @JsonProperty("predicted_per_token_ms") double predictedPerTokenMs,
        @JsonProperty("predicted_per_second") double predictedPerSecond
) {
}
