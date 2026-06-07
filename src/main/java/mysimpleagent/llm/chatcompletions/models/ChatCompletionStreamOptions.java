package mysimpleagent.llm.chatcompletions.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatCompletionStreamOptions(
        @JsonProperty("include_obfuscation")
        Boolean includeObfuscation,

        @JsonProperty("include_usage")
        Boolean include_usage
) {
}
