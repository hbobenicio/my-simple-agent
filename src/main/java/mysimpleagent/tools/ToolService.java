package mysimpleagent.tools;

import mysimpleagent.llm.chatcompletions.payloads.LLMChatCompletionMessage;
import mysimpleagent.llm.chatcompletions.stream.LLMChatCompletionsStreamChoiceDeltaToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ToolService {
    private static final Logger logger = LoggerFactory.getLogger(ToolService.class.getSimpleName());

    private final Toolset toolset;

    public ToolService(Toolset toolset) {
        this.toolset = toolset;
    }

    public void callTools(
            List<LLMChatCompletionMessage> messages,
            List<LLMChatCompletionsStreamChoiceDeltaToolCall> toolCalls
    ) throws Exception {
        //TODO implement tool calls limit
        var toolCallsCount = 0;
        for (var toolCall: toolCalls) {
            logger.atDebug()
                    .addKeyValue("id", toolCall.id())
                    .addKeyValue("type", toolCall.type())
                    .addKeyValue("name", toolCall.function().name())
                    .addKeyValue("args", toolCall.function().arguments())
                    .log("calling tool...");

            var func = toolCall.function();
            Tool tool = this.toolset.getTool(func.name());
            String toolRespContent = tool.call(func.arguments());
            toolCallsCount++;
            logger.atDebug()
                    .addKeyValue("name", toolCall.function().name())
                    .addKeyValue("toolCallsCount", toolCallsCount)
                    .addKeyValue("toolResponse", toolRespContent)
                    .log("tool called successfully");

            var toolMsg = new LLMChatCompletionMessage.LLMChatCompletionMessageTool(toolCall.id(), toolRespContent);
            messages.add(toolMsg);
        }
    }
}
