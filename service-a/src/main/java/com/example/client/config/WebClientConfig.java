package com.example.client.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${service-b.base-url}")
    private String serviceBBaseUrl;

    @Value("${service-b.timeout.connection}")
    private int connectionTimeout;

    @Value("${service-b.timeout.response}")
    private int responseTimeout;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(responseTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(connectionTimeout, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(serviceBBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleErrors())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info(">>> [WebClient] Request: {} {}", clientRequest.method(), clientRequest.url());
            log.debug(">>> [WebClient] Headers: {}", clientRequest.headers());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("<<< [WebClient] Response: Status {}", clientResponse.statusCode());
            log.debug("<<< [WebClient] Headers: {}", clientResponse.headers().asHttpHeaders());
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                log.error("!!! [WebClient] Error response: {}", clientResponse.statusCode());
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("!!! [WebClient] Error body: {}", errorBody);
                            return Mono.just(clientResponse);
                        });
            }
            return Mono.just(clientResponse);
        });
    }
}
