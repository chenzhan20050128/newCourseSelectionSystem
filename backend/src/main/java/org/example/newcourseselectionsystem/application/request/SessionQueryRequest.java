package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 节次倒查课程请求
 */
@Data
public class SessionQueryRequest {

    /**
     * 支持多天的或关系，例如：["周一","周三","周五"]
     */
    private List<String> weekdays;

    @Min(value = 1, message = "节次最小为1")
    @Max(value = 12, message = "节次最大为12")
    private Integer startPeriod;

    @Min(value = 1, message = "节次最小为1")
    @Max(value = 12, message = "节次最大为12")
    private Integer endPeriod;
}
