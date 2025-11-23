package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 节次倒查课程请求
 */
@Data
public class SessionQueryRequest {

    @NotBlank(message = "周几不能为空")
    private String weekday;

    @NotNull(message = "开始节次不能为空")
    @Min(value = 1, message = "节次最小为1")
    @Max(value = 12, message = "节次最大为12")
    private Integer startPeriod;

    @NotNull(message = "结束节次不能为空")
    @Min(value = 1, message = "节次最小为1")
    @Max(value = 12, message = "节次最大为12")
    private Integer endPeriod;
}
