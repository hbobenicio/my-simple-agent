package mysimpleagent.llm.chatcompletions.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import mysimpleagent.llm.ToolCall;

import java.util.List;

public record LLMMessage(
        String role,
        String content,
        @JsonProperty("reasoning_content") String reasoningContent,
        @JsonProperty("tool_calls") List<LLMChatCompletionTool> toolCalls
) {
}
