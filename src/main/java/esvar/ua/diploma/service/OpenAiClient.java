package esvar.ua.diploma.service;

import esvar.ua.diploma.config.OpenAiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final OpenAiProperties properties;

    public OpenAiClient(WebClient openAiWebClient, OpenAiProperties properties) {
        this.webClient = openAiWebClient;
        this.properties = properties;
    }

    public Mono<String> generateText(String systemPrompt, String userPrompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(properties.getModel(), List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        ));

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 429,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new WebClientResponseException(
                                        "OpenAI rate limit", response.statusCode().value(),
                                        response.statusCode().toString(),
                                        response.headers().asHttpHeaders(),
                                        body.getBytes(StandardCharsets.UTF_8), null))))
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new WebClientResponseException(
                                        "OpenAI error", response.statusCode().value(),
                                        response.statusCode().toString(),
                                        response.headers().asHttpHeaders(),
                                        body.getBytes(StandardCharsets.UTF_8), null))))
                .bodyToMono(ChatCompletionResponse.class)
                .map(response -> response.getChoices().isEmpty()
                        ? ""
                        : response.getChoices().get(0).getMessage().content())
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryable)
                        .transientErrors(true));
    }

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientResponseException wce && wce.getStatusCode().value() == 429;
    }

    public record Message(String role, String content) { }

    public record ChatCompletionRequest(String model, List<Message> messages, double temperature) {
        public ChatCompletionRequest(String model, List<Message> messages) {
            this(model, messages, 0.7);
        }
    }

    public static class ChatCompletionResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }
    }

    public static class Choice {
        private Message message;

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }
}
