package com.intelligent.ecommerce.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorService {

    private final RestClient ollamaRestClient;

    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null/blank");
        }

        OllamaEmbedResponse response = ollamaRestClient.post()
                .uri("/api/embed")
                .body(OllamaEmbedRequest.builder()
                        .model("nomic-embed-text:latest")
                        .input(text)
                        .build())
                .retrieve()
                .body(OllamaEmbedResponse.class);

        if (response == null || response.getEmbeddings() == null || response.getEmbeddings().isEmpty()) {
            throw new IllegalStateException("Empty embedding response from Ollama");
        }

        List<Double> vector = response.getEmbeddings().get(0);
        if (vector == null || vector.isEmpty()) {
            throw new IllegalStateException("Embedding vector is empty");
        }

        return vector;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    static class OllamaEmbedRequest {
        private String model;
        private String input;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    static class OllamaEmbedResponse {
        private String model;

        @JsonProperty("embeddings")
        private List<List<Double>> embeddings;
    }
}
