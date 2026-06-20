package mysimpleagent.llm.chatcompletions.payloads;

import com.fasterxml.jackson.annotation.*;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionDeveloperMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionDeveloperMessageParam(String content) {
            super("developer", content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionSystemMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionSystemMessageParam(String content) {
            super("system", content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionUserMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionUserMessageParam(String content) {
            super("user", content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionAssistantMessageParam extends ChatCompletionMessageParam {

        public String name;

        @JsonProperty("reasoning_content")
        public String reasoningContent;

        public String refusal;

        @JsonProperty("tool_calls")
        public List<LLMChatCompletionTool> toolCalls;

        public ChatCompletionAssistantMessageParam(String content, String reasoningContent, List<LLMChatCompletionTool> toolCalls) {
            super("assistant", content);
            this.reasoningContent = reasoningContent;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatCompletionFunctionMessageParam extends ChatCompletionMessageParam {
        public String name;

        public ChatCompletionFunctionMessageParam(String content) {
            super("function", content);
        }
    }
}
