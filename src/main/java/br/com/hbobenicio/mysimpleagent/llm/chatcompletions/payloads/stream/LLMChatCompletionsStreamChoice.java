package br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMChatCompletionsStreamChoice(
        Long index,
        LLMChatCompletionsStreamChoiceDelta delta,

        @JsonProperty("finish_reason")
        String finishReason,

        Object logprobs
) {
}
