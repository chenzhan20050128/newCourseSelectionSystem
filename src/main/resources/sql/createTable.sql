USE ncss;

DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS course_sessions;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS instructors;

CREATE TABLE instructors (
    instructor_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    instructor_name   VARCHAR(64) NOT NULL,
    college           VARCHAR(64) NOT NULL,
    password          VARCHAR(128) NOT NULL
);

CREATE TABLE students (
    student_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_name  VARCHAR(64) NOT NULL,
    birth_date    DATE        NOT NULL,
    college       VARCHAR(64) NOT NULL,
    phone         VARCHAR(32) NOT NULL,
    password      VARCHAR(128) NOT NULL
);

CREATE TABLE courses (
    course_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_name   VARCHAR(128) NOT NULL,
    credits       INT NOT NULL,
    description   VARCHAR(255),
    college       VARCHAR(64) NOT NULL,
    instructor_id BIGINT NOT NULL,
    campus        VARCHAR(64) NOT NULL,
    classroom     VARCHAR(64) NOT NULL,
    start_week    INT NOT NULL,
    end_week      INT NOT NULL,
    CONSTRAINT fk_course_instructor FOREIGN KEY (instructor_id) REFERENCES instructors (instructor_id)
);

CREATE TABLE course_sessions (
    session_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id     BIGINT NOT NULL,
    weekday       VARCHAR(16) NOT NULL,
    start_period  INT NOT NULL,
    end_period    INT NOT NULL,
    CONSTRAINT fk_session_course FOREIGN KEY (course_id) REFERENCES courses (course_id)
);

CREATE TABLE enrollments (
    enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id    BIGINT NOT NULL,
    course_id     BIGINT NOT NULL,
    enrolled_at   DATETIME NOT NULL,
    final_grade   DECIMAL(5,2),
    status        VARCHAR(16) NOT NULL,
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES students (student_id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses (course_id)
);

