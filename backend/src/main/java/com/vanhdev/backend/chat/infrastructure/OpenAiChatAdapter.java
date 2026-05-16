package com.vanhdev.backend.chat.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vanhdev.backend.shared.config.AiProperties;
import com.vanhdev.backend.shared.exception.LlmCompletionException;
import com.vanhdev.backend.shared.exception.ProviderRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Calls OpenAI ChatCompletion endpoint.
 * temperature is read from config and pinned to 0 by default.
 * RAG systems must not hallucinate — creativity is noise, not value.
 */
@Component
public class OpenAiChatAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatAdapter.class);

    private final RestClient restClient;
    private final String chatModel;
    private final int maxTokens;
    private final double temperature;

    public OpenAiChatAdapter(AiProperties props) {
        this.chatModel = props.chatModel();
        this.maxTokens = props.chatMaxTokens();
        this.temperature = props.chatTemperature();
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.openaiApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public CompletionResult complete(List<Message> messages) {
        var apiMessages = messages.stream()
                .map(m -> new ApiMessage(m.role(), m.content()))
                .toList();

        ChatCompletionRequest request = new ChatCompletionRequest(chatModel, apiMessages, maxTokens, temperature);

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        if (resp.getStatusCode().value() == 429) {
                            throw new ProviderRateLimitException("OpenAI");
                        }
                        throw new LlmCompletionException("OpenAI client error: HTTP " + resp.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new LlmCompletionException("OpenAI server error: HTTP " + resp.getStatusCode().value());
                    })
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LlmCompletionException("OpenAI returned empty response");
            }

            String content = response.choices().get(0).message().content();
            int promptTokens = response.usage() != null ? response.usage().promptTokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completionTokens() : 0;

            return new CompletionResult(content, promptTokens, completionTokens);

        } catch (ProviderRateLimitException | LlmCompletionException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmCompletionException("Unexpected error calling OpenAI: " + e.getMessage(), e);
        }
    }

    // --- OpenAI API wire types (private, not exposed to application layer) ---

    record ApiMessage(String role, String content) {}

    record ChatCompletionRequest(
            String model,
            List<ApiMessage> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature
    ) {}

    record ChatCompletionResponse(
            List<Choice> choices,
            Usage usage
    ) {}

    record Choice(ApiMessage message) {}

    record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens
    ) {}
}