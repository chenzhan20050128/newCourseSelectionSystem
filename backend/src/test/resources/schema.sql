-- H2数据库测试schema，移除了USE语句
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS course_sessions;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS instructors;

CREATE TABLE students (
    student_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_name  VARCHAR(64) NOT NULL,
    birth_date    DATE        NOT NULL,
    college       VARCHAR(64) NOT NULL,
    phone         VARCHAR(32) NOT NULL,
    password      VARCHAR(128) NOT NULL
);

CREATE TABLE instructors (
    instructor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    instructor_name VARCHAR(255) NOT NULL,
    college       VARCHAR(255) NOT NULL,
    password      VARCHAR(128) NOT NULL
);

CREATE TABLE courses (
    course_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_name   VARCHAR(255) NOT NULL,
    credits       INT,
    description   TEXT,
    college       VARCHAR(255),
    instructor_name VARCHAR(255),
    campus        VARCHAR(255),
    classroom     VARCHAR(255),
    start_week    INT,
    end_week      INT,
    capacity      INT,
    enrolled_count INT,
    type          VARCHAR(50)
);

CREATE TABLE course_sessions (
    session_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id     BIGINT NOT NULL,
    weekday       VARCHAR(16) NOT NULL,
    start_period  INT NOT NULL,
    end_period    INT NOT NULL
);

CREATE TABLE enrollments (
    enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id    BIGINT NOT NULL,
    course_id     BIGINT NOT NULL,
    enrolled_at   TIMESTAMP NOT NULL,
    final_grade   DECIMAL(5,2),
    status        VARCHAR(16) NOT NULL
);
