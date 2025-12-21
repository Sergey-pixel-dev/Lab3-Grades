package com.example.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeStats {
    private Long studentId;
    private String studentName;
    private Double averageGrade;
    private Long gradeCount;
}
