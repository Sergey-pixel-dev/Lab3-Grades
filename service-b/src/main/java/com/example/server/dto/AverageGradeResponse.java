package com.example.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AverageGradeResponse {
    private String courseName;
    private Double averageGrade;
    private Integer totalStudents;
    private Integer totalGrades;
    private List<StudentGradeInfo> topStudents;
    private Long processingTimeMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentGradeInfo {
        private Long studentId;
        private String studentName;
        private Double averageGrade;
        private Integer gradeCount;
    }
}
