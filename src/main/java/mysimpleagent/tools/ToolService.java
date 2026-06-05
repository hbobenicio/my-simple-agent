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
        for (var tool: toolCalls) {
            logger.atInfo()
                    .addKeyValue("id", tool.id())
                    .addKeyValue("type", tool.type())
                    .addKeyValue("name", tool.function().name())
                    .addKeyValue("args", tool.function().arguments())
                    .log("calling tool...");

            var func = tool.function();
            String toolRespContent = this.toolset.getTool(func.name()).call(func.arguments());
            toolCallsCount++;
            logger.atInfo()
                    .addKeyValue("name", tool.function().name())
                    .addKeyValue("toolCallsCount", toolCallsCount)
                    .addKeyValue("toolResponse", toolRespContent)
                    .log("tool called successfully");

            var toolMsg = new LLMChatCompletionMessage.LLMChatCompletionMessageTool(tool.id(), toolRespContent);
            messages.add(toolMsg);
        }
    }
}
