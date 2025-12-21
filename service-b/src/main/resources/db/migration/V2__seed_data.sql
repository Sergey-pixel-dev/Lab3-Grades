-- Insert courses
INSERT INTO courses (name, description) VALUES
('Mathematics', 'Advanced mathematics course'),
('Physics', 'General physics course'),
('Computer Science', 'Introduction to programming'),
('Chemistry', 'Organic chemistry basics'),
('Biology', 'Molecular biology'),
('English', 'English literature and composition'),
('History', 'World history'),
('Philosophy', 'Introduction to philosophy'),
('Economics', 'Microeconomics fundamentals'),
('Statistics', 'Statistical analysis and probability');

-- Function to generate random grades
-- This will create 100,000+ grade records for performance testing
DO $$
DECLARE
    student_count INTEGER := 1000;  -- 1000 students
    course_count INTEGER;
    student_id_val BIGINT;
    course_id_val BIGINT;
    i INTEGER;
    j INTEGER;
    k INTEGER;
    grades_per_student INTEGER;
BEGIN
    -- Get actual course count
    SELECT COUNT(*) INTO course_count FROM courses;

    -- Create students
    FOR i IN 1..student_count LOOP
        INSERT INTO students (name, email)
        VALUES (
            'Student ' || i,
            'student' || i || '@university.edu'
        ) RETURNING id INTO student_id_val;

        -- Each student takes 80-120 courses (with repetitions for retakes)
        grades_per_student := 80 + floor(random() * 41)::INTEGER;

        FOR j IN 1..grades_per_student LOOP
            -- Random course
            SELECT id INTO course_id_val
            FROM courses
            ORDER BY random()
            LIMIT 1;

            -- Insert grade
            INSERT INTO grades (student_id, course_id, grade)
            VALUES (
                student_id_val,
                course_id_val,
                (random() * 100)::INTEGER
            );
        END LOOP;

        -- Log progress every 100 students
        IF i % 100 = 0 THEN
            RAISE NOTICE 'Created % students with grades', i;
        END IF;
    END LOOP;

    RAISE NOTICE 'Data seeding completed!';
    RAISE NOTICE 'Total students: %', student_count;
    RAISE NOTICE 'Total grades: %', (SELECT COUNT(*) FROM grades);
END $$;
