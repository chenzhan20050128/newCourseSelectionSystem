package org.example.newcourseselectionsystem.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程推荐响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRecommendationResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 推荐结果文本
     */
    private String recommendation;

    /**
     * 错误信息
     */
    private String message;
}

