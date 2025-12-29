## New Course Selection System

2025-2026学年第一学期，南京大学软件学院大三本科生专业课《人机交互系统》开发类大作业。

本项目为面向南京大学课程数据的选课系统，对现有选课系统、教务系统等进行了重构、整合和人机交互上的优化。系统包含前端（Vue + Vite）与后端（Spring Boot + MyBatis-Plus + MySQL），并可选启用 DeepSeek API 智能选课能力。

## 环境要求
- Node.js 18+，npm
- JDK 8，Maven 3.8+
- MySQL 8（已创建用户并有建库/导入权限）

## 数据准备（后端）
1. 在 MySQL 创建数据库并设置字符集
	 ```sql
	 CREATE DATABASE ncss CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
	 ```
2. 建表：在数据源执行 `backend/src/main/resources/sql/createTable.sql`。
3. 插入基础学生与选课轮次数据：执行 `backend/src/main/resources/sql/insertTable.sql`。
4. 导入课程信息：
	 - 将 `backend/src/main/resources/sql/courses.csv` 导入 `course` 表。
	 - 导入时请保持 `course_id` 使用 CSV 中的值，不要选择自动递增/自动生成主键。
5. 导入课程场次信息：将 `backend/src/main/resources/sql/course_sessions.csv` 导入 `course_sessions` 表（同样保留 CSV 中的 `course_id` 关联）。

## 后端配置
- 核心配置文件：`backend/src/main/resources/application.properties`
	- 设置 MySQL 连接信息（`spring.datasource.url/username/password`）。
	- 如需使用 DeepSeek 智能选课，填写 `deepseek.api.key`。
- 可选本地覆盖：`backend/src/main/resources/application-dev.properties`（已在主配置中通过 `spring.config.import` 自动加载）。

## 后端启动方式（建议使用IDEA启动）
- IDE：直接运行主类 `org.example.newcourseselectionsystem.NewCourseSelectionSystemApplication`。
- Maven 命令行：
	```bash
	cd backend
	mvn clean package
	java -jar target/newCourseSelectionSystem-0.0.1-SNAPSHOT.jar
	# 或开发模式
	mvn spring-boot:run
	```

## 前端启动方式
在根目录下打开
```bash
cd frontend
npm install
npm run dev
```
- 前端开发地址为 http://localhost:3000 。
- 建议使用新账户进行登录，你可以在首次使用时创建一个新账户。

## 目录速览
- backend/：Spring Boot 服务端代码与 SQL 初始化脚本
- frontend/：基于 Vite 的 Vue 前端
- docs/：需求与接口文档

## 常用排查
- 若后端启动失败，优先检查数据库是否已建库、表是否已导入、配置文件中的账号密码是否正确。
- 导入 CSV 时若出现主键或外键冲突，确认 `course_id` 未被自动改写且导入顺序为课程表先于课程场次表。
