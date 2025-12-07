package org.example.newcourseselectionsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生实体
 */
@Data
@TableName("students")
public class Student {

    @TableId(value = "student_id", type = IdType.INPUT)
    private Long studentId;

    private String studentName;

    private LocalDate birthDate;

    private String college;

    private String password;
}
