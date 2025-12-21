package com.example.client.controller;

import com.example.client.dto.AverageGradeResponse;
import com.example.client.service.GradeClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@Slf4j
public class GradeController {
    private final GradeClientService gradeClientService;

    @GetMapping("/grades/average/{courseName}")
    public Mono<ResponseEntity<AverageGradeResponse>> getAverageGrade(
            @PathVariable String courseName) {
        log.info("Client received request for average grade of course: {}", courseName);

        return gradeClientService.getAverageGrade(courseName)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error processing request for course: {}", courseName, error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Service A (Client)"
        )));
    }

    @GetMapping("/health/service-b")
    public Mono<ResponseEntity<Map<String, String>>> checkServiceBHealth() {
        return gradeClientService.checkServiceBHealth()
                .map(response -> ResponseEntity.ok(Map.of(
                        "status", "UP",
                        "service-b", response
                )))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "status", "DOWN",
                                "error", error.getMessage()
                        ))));
    }
}
