package br.com.hbobenicio.mysimpleagent.llm.models.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMModelListItem(
        String id,
        String object,

        @JsonProperty("owned_by")
        String ownedBy
) {}
