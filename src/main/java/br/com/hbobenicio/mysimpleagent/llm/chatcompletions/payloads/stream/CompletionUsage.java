package br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/// Usage statistics for the completion request.
///
/// [Reference](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create#(resource)%20chat.completions%20%3E%20(model)%20chat_completion%20%3E%20(schema)%20%3E%20(property)%20usage)
///
/// @param completionTokens        Number of tokens in the generated completion.
/// @param promptTokens            Number of tokens in the prompt
/// @param totalTokens             Total number of tokens used in the request (prompt + completion).
/// @param completionTokensDetails (**OPTIONAL**) Breakdown of tokens used in a completion.
/// @param promptTokensDetails     (**OPTIONAL**) Breakdown of tokens used in the prompt.
///
public record CompletionUsage(
        @JsonProperty("prompt_tokens")
        Integer promptTokens,

        @JsonProperty("completion_tokens")
        Integer completionTokens,

        @JsonProperty("total_tokens")
        Integer totalTokens,

        @JsonProperty("completion_tokens_details")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        CompletionUsageCompletionTokenDetails completionTokensDetails,

        @JsonProperty("prompt_tokens_details")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        CompletionUsagePromptTokensDetails promptTokensDetails
) {
}
