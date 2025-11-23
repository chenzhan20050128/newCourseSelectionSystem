package org.example.newcourseselectionsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课程实体
 */
@Data
@TableName("courses")
public class Course {

    @TableId(value = "course_id", type = IdType.AUTO)
    private Long courseId;

    private String courseName;

    private Integer credits;

    private String description;

    private String college;

    private Long instructorId;

    private String campus;

    private String classroom;

    private Integer startWeek;

    private Integer endWeek;

    private Integer capacity;

    private Integer enrolledCount;
}
