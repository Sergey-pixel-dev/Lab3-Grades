package com.example.server.controller;

import com.example.server.dto.AverageGradeResponse;
import com.example.server.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@Slf4j
public class GradeController {
    private final GradeService gradeService;

    @GetMapping("/average/{courseName}")
    public Mono<ResponseEntity<AverageGradeResponse>> getAverageGrade(
            @PathVariable String courseName) {
        log.info("Received request for average grade of course: {}", courseName);

        return Mono.fromCallable(() -> gradeService.calculateAverageGradeByCourse(courseName))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("Successfully calculated average for course: {}", courseName))
                .doOnError(error -> log.error("Error calculating average for course: {}", courseName, error))
                .onErrorResume(error -> {
                    log.error("Failed to process request for course: {}", courseName, error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("Service B is running"));
    }
}
