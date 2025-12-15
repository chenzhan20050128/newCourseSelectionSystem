package org.example.newcourseselectionsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课程节次实体
 */
@Data
@TableName("course_sessions")
public class CourseSession {

    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    private Long courseId;

    private String weekday;

    private Integer startPeriod;

    private Integer endPeriod;

    private Integer weekType; // 0: 全周, 1: 单周, 2: 双周
}
