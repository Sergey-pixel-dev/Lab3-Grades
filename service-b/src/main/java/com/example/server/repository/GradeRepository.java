package com.example.server.repository;

import com.example.server.dto.StudentGradeStats;
import com.example.server.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentId(Long studentId);
    List<Grade> findByCourseId(Long courseId);

    @Query("SELECT new com.example.server.dto.StudentGradeStats(" +
           "s.id, s.name, AVG(g.grade), COUNT(g)) " +
           "FROM Grade g " +
           "JOIN g.student s " +
           "JOIN g.course c " +
           "WHERE c.name = :courseName " +
           "GROUP BY s.id, s.name " +
           "ORDER BY AVG(g.grade) DESC")
    List<StudentGradeStats> findStudentStatsByCourseName(@Param("courseName") String courseName);

    @Query("SELECT AVG(g.grade) FROM Grade g JOIN g.course c WHERE c.name = :courseName")
    Double findAverageGradeByCourseName(@Param("courseName") String courseName);

    @Query("SELECT COUNT(g) FROM Grade g JOIN g.course c WHERE c.name = :courseName")
    Long countByCourseName(@Param("courseName") String courseName);

    @Query("SELECT COUNT(DISTINCT g.student.id) FROM Grade g JOIN g.course c WHERE c.name = :courseName")
    Long countDistinctStudentsByCourseName(@Param("courseName") String courseName);
}
