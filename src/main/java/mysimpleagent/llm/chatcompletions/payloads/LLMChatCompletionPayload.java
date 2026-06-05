package mysimpleagent.llm.chatcompletions.payloads;

import java.util.List;
import java.util.Map;

public record LLMChatCompletionPayload(
        String model,
        boolean stream,
        List<LLMChatCompletionMessage> messages,
//        List<LLMChatCompletionTool> tools
        List<Object> tools
){}
