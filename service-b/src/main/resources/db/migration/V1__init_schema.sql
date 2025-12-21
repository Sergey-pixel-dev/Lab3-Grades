-- Create students table
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create courses table
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create grades table
CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id),
    course_id BIGINT NOT NULL REFERENCES courses(id),
    grade INTEGER NOT NULL CHECK (grade >= 0 AND grade <= 100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance (though we'll ignore them in inefficient code)
CREATE INDEX idx_grades_student_id ON grades(student_id);
CREATE INDEX idx_grades_course_id ON grades(course_id);
CREATE INDEX idx_courses_name ON courses(name);
