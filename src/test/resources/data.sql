-- H2数据库测试数据，移除了USE语句

-- 1. 插入 instructors（教师表）
INSERT INTO instructors (instructor_name, college, password) VALUES
('李强', '计算机学院', 'pass123'),
('陈明', '计算机学院', 'pass124'),
('张伟', '计算机学院', 'pass125');

-- 2. 插入 students（学生表）
INSERT INTO students (student_name, birth_date, college, phone, password) VALUES
('张三', '2002-03-15', '计算机学院', '13800000001', 'stu123'),
('张明', '2002-05-20', '计算机学院', '13800000011', 'stu124'),
('李华', '2003-01-10', '计算机学院', '13800000012', 'stu125');

-- 3. 插入 courses（课程表）
INSERT INTO courses (course_name, credits, description, college, instructor_id, campus, classroom, start_week, end_week, capacity, enrolled_count) VALUES
('数据结构', 4, '基础数据结构课程', '计算机学院', 1, '本部', 'A101', 1, 16, 60, 1),
('数据库系统', 3, '关系数据库原理与设计', '计算机学院', 1, '本部', 'A202', 5, 16, 50, 1),
('操作系统', 4, '操作系统原理与实现', '计算机学院', 2, '本部', 'A103', 1, 16, 55, 1),
('计算机网络', 3, '网络协议与网络编程', '计算机学院', 2, '本部', 'A204', 8, 16, 50, 0),
('编译原理', 4, '编译器设计与实现', '计算机学院', 3, '本部', 'A105', 1, 16, 45, 0);

-- 4. 插入 course_sessions（课程节次表）
INSERT INTO course_sessions (course_id, weekday, start_period, end_period) VALUES
-- 数据结构：周三第5节
(1, '周三', 5, 5),
-- 数据库系统：周一第1-2节
(2, '周一', 1, 2),
-- 操作系统：周二第3-4节
(3, '周二', 3, 4),
-- 计算机网络：周四第5-6节
(4, '周四', 5, 6),
-- 编译原理：周五第1-2节
(5, '周五', 1, 2);

-- 5. 插入 enrollments（选课表）
INSERT INTO enrollments (student_id, course_id, enrolled_at, final_grade, status) VALUES
-- 学生1选了课程1、2、3
(1, 1, '2024-02-20 09:00:00', 92.5, '已选'),
(1, 2, '2024-02-20 10:00:00', NULL, '已选'),
(1, 3, '2024-02-20 11:00:00', 88.0, '已选');
