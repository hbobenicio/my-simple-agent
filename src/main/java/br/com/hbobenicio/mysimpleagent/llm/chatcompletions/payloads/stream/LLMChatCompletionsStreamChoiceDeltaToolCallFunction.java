package br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads.stream;

public record LLMChatCompletionsStreamChoiceDeltaToolCallFunction (
        String name,
        String arguments
){}
