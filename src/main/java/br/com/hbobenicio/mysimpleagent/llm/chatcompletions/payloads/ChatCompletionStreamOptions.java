package br.com.hbobenicio.mysimpleagent.llm.chatcompletions.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionStreamOptions(

        @JsonProperty("include_obfuscation")
        Boolean includeObfuscation,

        @JsonProperty("include_usage")
        Boolean include_usage
) {
}
