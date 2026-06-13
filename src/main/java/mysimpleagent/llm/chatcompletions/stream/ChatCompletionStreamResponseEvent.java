package mysimpleagent.llm.chatcompletions.stream;

import java.util.List;

/// The Streaming Response Event from OpenAI Chat Completions API.
///
/// [Reference](https://developers.openai.com/api/reference/resources/chat/subresources/completions/streaming-events)
public record ChatCompletionStreamResponseEvent(
        // A unique identifier for the chat completion. Each chunk has the same ID.
        String id,

        // The object type, which is always chat.completion.chunk.
        String object,

        // The Unix timestamp (in seconds) of when the chat completion was created. Each chunk has the same timestamp.
        Long created,

        // The model to generate the completion.
        String model,

        List<LLMChatCompletionsStreamChoice> choices,

        ChatCompletionUsage usage
){}
