package mysimpleagent.llm.chatcompletions.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LLMChatCompletionResponse(
        String id,
        String object,
        long created,
        String model,
        @JsonProperty("system_fingerprint") String systemFingerprint,
        List<LLMChoice> choices,
        LLMUsage usage,
        LLMTimings timings
) {
}
