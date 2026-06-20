package mysimpleagent.llm.models.payloads;

import java.util.List;

public record LLMModelListResponse(
        List<LLMModelListItem> data,
        String object
) {}