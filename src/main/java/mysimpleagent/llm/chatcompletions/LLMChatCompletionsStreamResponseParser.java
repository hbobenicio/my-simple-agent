package mysimpleagent.llm.chatcompletions;

import mysimpleagent.llm.chatcompletions.stream.ChatCompletionStreamResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class LLMChatCompletionsStreamResponseParser {
    private static final Logger logger = LoggerFactory.getLogger(LLMChatCompletionsStreamResponseParser.class.getSimpleName());

    private final ObjectMapper objectMapper;

    public LLMChatCompletionsStreamResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChatCompletionStreamResponseEvent parseEvent(String streamLine) {
        streamLine = streamLine.trim();
        if (streamLine.isEmpty()) {
            return null;
        }

        var prefix = "data: ";
        if (!streamLine.startsWith(prefix)) {
            logger.atError()
                    .addKeyValue("line", streamLine)
                    .log("bad chat completions response stream line");
            return null;
        }
        String chunkString = streamLine.substring(prefix.length());

        return this.objectMapper.readValue(chunkString, ChatCompletionStreamResponseEvent.class);
    }
}
