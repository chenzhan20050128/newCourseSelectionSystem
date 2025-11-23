package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.request.CourseRecommendationRequest;
import reactor.core.publisher.Flux;

/**
 * 课程推荐服务接口
 */
public interface CourseRecommendationService {

    /**
     * 根据学生输入获取流式课程推荐
     *
     * @param request 推荐请求
     * @return 推荐内容流
     */
    Flux<String> getRecommendationStream(CourseRecommendationRequest request);
}
