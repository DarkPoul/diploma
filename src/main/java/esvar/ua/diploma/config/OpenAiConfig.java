package esvar.ua.diploma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    @Bean
    public WebClient openAiWebClient(OpenAiProperties properties) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .build();
    }
}
