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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final OpenAiProperties properties;
    private final Logger logger = LoggerFactory.getLogger(OpenAiClient.class);

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
                .retryWhen(rateLimitRetry());
    }

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientResponseException wce && wce.getStatusCode().value() == 429;
    }

    private Retry rateLimitRetry() {
        return Retry.from(companion -> companion.flatMap(retrySignal -> {
            Throwable failure = retrySignal.failure();
            if (!isRetryable(failure)) {
                return Mono.error(failure);
            }

            long attempt = retrySignal.totalRetries() + 1;
            if (attempt > 5) {
                return Mono.error(failure);
            }

            Duration delay = calculateBackoff(failure, attempt);
            logger.warn("OpenAI rate limit hit, retrying attempt {} in {} seconds", attempt, delay.toSeconds());
            return Mono.delay(delay);
        }));
    }

    private Duration calculateBackoff(Throwable throwable, long attempt) {
        if (throwable instanceof WebClientResponseException wce) {
            String retryAfter = wce.getHeaders().getFirst("Retry-After");
            Duration headerDelay = parseRetryAfter(retryAfter);
            if (headerDelay != null) {
                return headerDelay;
            }
        }

        long seconds = (long) Math.min(30, Math.pow(2, attempt));
        return Duration.ofSeconds(seconds);
    }

    private Duration parseRetryAfter(String retryAfter) {
        if (!StringUtils.hasText(retryAfter)) {
            return null;
        }

        try {
            long seconds = Long.parseLong(retryAfter.trim());
            if (seconds > 0) {
                return Duration.ofSeconds(seconds);
            }
        } catch (NumberFormatException ignored) {
            try {
                Instant retryTime = Instant.parse(retryAfter.trim());
                Duration until = Duration.between(Instant.now(), retryTime);
                return until.isNegative() ? Duration.ZERO : until;
            } catch (DateTimeParseException ignoredAgain) {
                logger.debug("Unable to parse Retry-After header value: {}", retryAfter);
            }
        }
        return null;
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
