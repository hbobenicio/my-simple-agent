package mysimpleagent.llm.chatcompletions.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "role",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = LLMChatCompletionMessage.LLMChatCompletionMessageTool.class,
                name = "tool"
        )
})
public class LLMChatCompletionMessage {

    public String role;

    public String content;

    public LLMChatCompletionMessage(String role) {
        this.role = role;
    }

    public LLMChatCompletionMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static class LLMChatCompletionMessageTool extends LLMChatCompletionMessage {
        @JsonProperty("tool_call_id")
        public String toolCallId;

        public String content;

        public LLMChatCompletionMessageTool(String toolCallId, String content) {
            super("tool");
            this.toolCallId = toolCallId;
            this.content = content;
        }
    }
}
