package org.example.newcourseselectionsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 选课实体
 */
@Data
@TableName("enrollments")
public class Enrollment {

    @TableId(value = "enrollment_id", type = IdType.AUTO)
    private Long enrollmentId;

    private Long studentId;

    private Long courseId;

    @TableField("enrolled_at")
    private LocalDateTime enrolledAt;

    private BigDecimal finalGrade;

    private String status;
}
