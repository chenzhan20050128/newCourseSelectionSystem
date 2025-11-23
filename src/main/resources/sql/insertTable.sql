USE ncss;
-- 1. 先插入 instructors（教师表）
INSERT INTO instructors (instructor_name, college, password) VALUES
                                                                 ('李强', '计算机学院', 'pass123'),
                                                                 ('王老师', '信息工程学院', 'pass456'),
                                                                 ('赵敏', '数学学院', 'pass789');

-- 2. 再插入 students（学生表）
INSERT INTO students (student_name, birth_date, college, phone, password) VALUES
                                                                              ('张三', '2002-03-15', '计算机学院', '13800000001', 'stu123'),
                                                                              ('李四', '2001-07-21', '数学学院', '13800000002', 'stu456'),
                                                                              ('王五', '2003-11-09', '信息工程学院', '13800000003', 'stu789');

-- 3. 再插入 courses（课程表）- 依赖instructors表
INSERT INTO courses (course_name, credits, description, college, instructor_id, campus, classroom, start_week, end_week) VALUES
                                                                                                                             ('数据结构', 4, '基础数据结构课程', '计算机学院', 1, '本部', 'A101', 1, 16),
                                                                                                                             ('高等数学', 5, '数学分析进阶内容', '数学学院', 3, '南校区', 'B203', 1, 18),
                                                                                                                             ('数据库系统', 3, '关系数据库原理', '计算机学院', 1, '本部', 'A202', 5, 16),
                                                                                                                             ('通信原理', 3, '通信系统基本原理', '信息工程学院', 2, '北校区', 'C305', 8, 16);

-- 4. 再插入 course_sessions（课程时间段表）- 依赖courses表
INSERT INTO course_sessions (course_id, weekday, start_period, end_period) VALUES
                                                                               (1, '周一', 1, 2),
                                                                               (1, '周三', 5, 7),
                                                                               (2, '周三', 4, 6),
                                                                               (2, '周五', 3, 4),
                                                                               (3, '周三', 6, 8),
                                                                               (3, '周四', 1, 2),
                                                                               (4, '周三', 5, 6),
                                                                               (4, '周四', 3, 5);

-- 5. 最后插入 enrollments（选课表）- 依赖students和courses表
INSERT INTO enrollments (student_id, course_id, enrolled_at, final_grade, status) VALUES
                                                                                      (1, 1, '2024-02-20 09:00:00', 92.5, '已选'),
                                                                                      (1, 3, '2024-02-20 10:00:00', NULL, '已选'),
                                                                                      (2, 2, '2024-02-21 11:00:00', 88.0, '已选'),
                                                                                      (3, 4, '2024-02-22 14:00:00', NULL, '已选');