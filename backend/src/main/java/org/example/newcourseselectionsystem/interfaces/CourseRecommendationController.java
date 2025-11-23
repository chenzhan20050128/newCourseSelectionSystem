package org.example.newcourseselectionsystem.interfaces;

import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.request.CourseRecommendationRequest;
import org.example.newcourseselectionsystem.application.service.CourseRecommendationService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import javax.validation.Valid;
import java.io.IOException;

/**
 * 课程推荐接口
 */
@RestController
@RequestMapping("/api/recommendations")
@Validated
@RequiredArgsConstructor
public class CourseRecommendationController {

    private final CourseRecommendationService courseRecommendationService;

    /**
     * 获取流式课程推荐（Server-Sent Events）
     *
     * @param request 推荐请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/recommend", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter recommendStream(@Valid @RequestBody CourseRecommendationRequest request) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        Flux<String> recommendationStream = courseRecommendationService.getRecommendationStream(request);

        recommendationStream.subscribe(
                content -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .data(content)
                                .name("message"));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .data("错误: " + error.getMessage())
                                .name("error"));
                    } catch (IOException e) {
                        // 忽略发送错误时的异常
                    }
                    emitter.completeWithError(error);
                },
                () -> emitter.complete()
        );

        return emitter;
    }
}

