package br.com.hbobenicio.mysimpleagent.llm.chatcompletions;

import br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads.stream.LLMChatCompletionsStreamChoiceDeltaToolCall;

import java.util.List;
import java.util.Optional;

public record ChatResponse(
        String reasoning,
        String message,
        List<LLMChatCompletionsStreamChoiceDeltaToolCall> toolCalls
) {
    public boolean hasToolCalls() {
        return Optional.ofNullable(this.toolCalls)
                .map(List::size)
                .filter(n -> n > 0)
                .isPresent();
    }
}
