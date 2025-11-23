package org.example.newcourseselectionsystem.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选课响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 警告信息（如时间冲突等，不影响选课成功）
     */
    private String warn;

    /**
     * 选课记录ID
     */
    private Long enrollmentId;
}

