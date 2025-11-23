package org.example.newcourseselectionsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 教师实体
 */
@Data
@TableName("instructors")
public class Instructor {

    @TableId(value = "instructor_id", type = IdType.AUTO)
    private Long instructorId;

    private String instructorName;

    private String college;

    private String password;
}
