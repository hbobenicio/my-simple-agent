package br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads;

public record LLMChatCompletionToolFunction(
        String name,
        String description,
//        LLMChatCompletionToolFunctionParams parameters
        String arguments
){}
