package com.example.client.service;

import com.example.client.dto.AverageGradeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeClientService {
    private final WebClient webClient;

    @Value("${service-b.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${service-b.retry.backoff.initial}")
    private long initialBackoff;

    @Value("${service-b.retry.backoff.max}")
    private long maxBackoff;

    @Value("${service-b.retry.backoff.multiplier}")
    private double backoffMultiplier;

    public Mono<AverageGradeResponse> getAverageGrade(String courseName) {
        log.info("Requesting average grade for course: {}", courseName);

        return webClient.get()
                .uri("/api/grades/average/{courseName}", courseName)
                .retrieve()
                .bodyToMono(AverageGradeResponse.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(initialBackoff))
                        .maxBackoff(Duration.ofMillis(maxBackoff))
                        .filter(this::shouldRetry)
                        .doBeforeRetry(retrySignal -> {
                            log.warn("Retrying request for course: {} (attempt {}/{}). Reason: {}",
                                    courseName,
                                    retrySignal.totalRetries() + 1,
                                    maxRetryAttempts,
                                    retrySignal.failure().getMessage());
                        })
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Max retry attempts ({}) exceeded for course: {}",
                                    maxRetryAttempts, courseName);
                            return new RuntimeException(
                                    "Failed to fetch average grade after " + maxRetryAttempts + " attempts",
                                    retrySignal.failure()
                            );
                        }))
                .doOnSuccess(response -> {
                    log.info("Successfully received response for course: {}. Processing time: {} ms",
                            courseName, response.getProcessingTimeMs());
                })
                .doOnError(error -> {
                    log.error("Failed to fetch average grade for course: {}", courseName, error);
                });
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientRequestException) {
            log.warn("Network error occurred, will retry: {}", throwable.getMessage());
            return true;
        }

        if (throwable instanceof WebClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            boolean shouldRetry = statusCode >= 500;
            if (shouldRetry) {
                log.warn("Server error (status {}), will retry", statusCode);
            } else {
                log.error("Client error (status {}), will not retry", statusCode);
            }
            return shouldRetry;
        }

        log.error("Non-retryable error occurred: {}", throwable.getClass().getSimpleName());
        return false;
    }

    public Mono<String> checkServiceBHealth() {
        return webClient.get()
                .uri("/api/grades/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> log.info("Service B health check: OK"))
                .doOnError(error -> log.error("Service B health check: FAILED", error));
    }
}
