package br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

/// Breakdown of tokens used in the prompt.
///
/// @param audioTokens [OPTIONAL] Audio input tokens present in the prompt.
/// @param cachedTokens [OPTIONAL] Cached tokens present in the prompt.
///
/// [OpenAI Developers Chat Completions Reference](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create#(resource)%20chat.completions%20%3E%20(model)%20chat_completion%20%3E%20(schema)%20%3E%20(property)%20usage%20%2B%20(resource)%20completions%20%3E%20(model)%20completion_usage%20%3E%20(schema)%20%3E%20(property)%20prompt_tokens_details)
public record CompletionUsagePromptTokensDetails(
        @JsonProperty("audio_tokens")
        Long audioTokens,

        @JsonProperty("cached_tokens")
        Long cachedTokens
) {}
