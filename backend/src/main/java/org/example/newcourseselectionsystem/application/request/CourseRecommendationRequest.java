package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 课程推荐请求
 */
@Data
public class CourseRecommendationRequest {

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 用户输入的推荐请求文本
     */
    @NotBlank(message = "推荐请求不能为空")
    private String prompt;
}

