USE ncss;

DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS course_sessions;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS elective_batches;  #选课轮次
DROP TABLE IF EXISTS students;

CREATE TABLE students (
    student_id    BIGINT PRIMARY KEY,
    student_name  VARCHAR(64) NOT NULL,
    birth_date    DATE        NOT NULL,
    college       VARCHAR(64) NOT NULL,
    password      VARCHAR(128) NOT NULL
);

-- 课程表，新增type字段用于毕业要求分类
CREATE TABLE courses (
    course_id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_name     VARCHAR(128) NOT NULL,
    credits         INT NOT NULL,
    description     VARCHAR(255),
    college         VARCHAR(255) NOT NULL,
    instructor_name VARCHAR(255) NOT NULL,
    campus          VARCHAR(64) NOT NULL,
    classroom       VARCHAR(64) NOT NULL,
    start_week      INT NOT NULL,
    end_week        INT NOT NULL,
    capacity        INT NOT NULL COMMENT '课程容量，与enrolled_count（已选人数）一起用于判断课程是否已满',
    enrolled_count  INT NOT NULL DEFAULT 0 COMMENT '已选人数，与capacity（课程容量）一起用于判断课程是否已满',
    type            VARCHAR(50) NULL COMMENT '课程类型，用于毕业要求分类'
);

-- 课程节次表
CREATE TABLE course_sessions (
    session_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id     BIGINT NOT NULL,
    weekday       VARCHAR(16) NOT NULL,
    start_period  INT NOT NULL,
    end_period    INT NOT NULL,
    week_type     INT NOT NULL DEFAULT 0 COMMENT '0:全周, 1:单周, 2:双周'
);

CREATE TABLE elective_batches (
    batch_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_name    VARCHAR(128) NOT NULL COMMENT '选课轮次名称，如"2024-2025学年第一学期选课"',
    round_name    VARCHAR(64) NOT NULL COMMENT '轮次名称，如"第一轮"、"第二轮"、"补退选"',
    start_time    DATETIME NOT NULL COMMENT '选课开始时间',
    end_time      DATETIME NOT NULL COMMENT '选课结束时间',
    selection_mode VARCHAR(32) NOT NULL COMMENT '选课模式：志愿、即选即中、先到先得',
    selection_strategy VARCHAR(64) NOT NULL COMMENT '选课策略说明',
    status        VARCHAR(16) NOT NULL DEFAULT '未开始' COMMENT '状态：未开始、进行中、已结束',
    description   TEXT COMMENT '选课说明'
);

CREATE TABLE enrollments (
    enrollment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id    BIGINT NOT NULL,
    course_id     BIGINT NOT NULL,
    batch_id      BIGINT COMMENT '所属选课轮次',
    enrolled_at   DATETIME NOT NULL,
    final_grade   DECIMAL(5,2),
    status        VARCHAR(16) NOT NULL COMMENT '选课状态，如"已选"、"已退选"等'
);
