package mysimpleagent.llm.chatcompletions.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMChoice(
        int index,
        LLMMessage message,
        @JsonProperty("finish_reason") String finishReason
) {
}
