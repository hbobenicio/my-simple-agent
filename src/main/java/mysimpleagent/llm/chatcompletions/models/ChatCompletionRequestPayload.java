package mysimpleagent.llm.chatcompletions.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// The Chat Completions POST Body Request Payload
///
/// @param model  Required. The Model ID used to generate the response
/// @param stream Optional. If set to true, the model response data will be streamed to the client as it is generated using
///               SSE (Server-Sent Events)
/// @param streamOptions Optional.
/// @param messages Required. A list of messages comprising the conversation so far.
public record ChatCompletionRequestPayload(
        String model,
        boolean stream,

        @JsonProperty("stream_options")
        ChatCompletionStreamOptions streamOptions,

        List<ChatCompletionMessageParam> messages,
//        List<LLMChatCompletionTool> tools
        List<Object> tools
){
}
