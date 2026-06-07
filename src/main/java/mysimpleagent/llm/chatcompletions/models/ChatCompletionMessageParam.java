package mysimpleagent.llm.chatcompletions.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "role",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = ChatCompletionMessageParam.ChatCompletionDeveloperMessageParam.class,
                name = "developer"
        ),
        @JsonSubTypes.Type(
                value = ChatCompletionMessageParam.ChatCompletionSystemMessageParam.class,
                name = "system"
        ),
        @JsonSubTypes.Type(
                value = ChatCompletionMessageParam.ChatCompletionUserMessageParam.class,
                name = "user"
        ),
        @JsonSubTypes.Type(
                value = ChatCompletionMessageParam.ChatCompletionAssistantMessageParam.class,
                name = "assistant"
        ),
        @JsonSubTypes.Type(
                value = ChatCompletionMessageParam.ChatCompletionToolMessageParam.class,
                name = "tool"
        ),
        @JsonSubTypes.Type(
                value = ChatCompletionMessageParam.ChatCompletionFunctionMessageParam.class,
                name = "function"
        ),
})
public abstract class ChatCompletionMessageParam {

    public String role;

    public String content;

    public ChatCompletionMessageParam(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static class ChatCompletionDeveloperMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionDeveloperMessageParam(String content) {
            super("developer", content);
        }
    }

    public static class ChatCompletionSystemMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionSystemMessageParam(String content) {
            super("system", content);
        }
    }

    public static class ChatCompletionUserMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionUserMessageParam(String content) {
            super("user", content);
        }
    }

    public static class ChatCompletionAssistantMessageParam extends ChatCompletionMessageParam {
        public String name;
        public String refusal;

        @JsonProperty("tool_calls")
        public List<LLMChatCompletionTool> toolCalls;

        public ChatCompletionAssistantMessageParam(String content, List<LLMChatCompletionTool> toolCalls) {
            super("assistant", content);
            this.toolCalls = toolCalls;
        }
    }

    public static class ChatCompletionToolMessageParam extends ChatCompletionMessageParam {
        @JsonProperty("tool_call_id")
        public String toolCallId;

        public ChatCompletionToolMessageParam(String toolCallId, String content) {
            super("tool", content);
            this.toolCallId = toolCallId;
        }
    }

    public static class ChatCompletionFunctionMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionFunctionMessageParam(String content) {
            super("function", content);
        }
    }
}
