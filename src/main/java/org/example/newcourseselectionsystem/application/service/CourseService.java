package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseQueryRequest;
import org.example.newcourseselectionsystem.application.request.SessionQueryRequest;

import java.util.List;

/**
 * 课程相关业务
 */
public interface CourseService {

    /**
     * 根据课程表的全部字段进行组合过滤，并返回附带节次的课程信息
     */
    List<CourseWithSessionsDTO> listCoursesByCondition(CourseQueryRequest request);

    /**
     * 根据节次条件找到符合条件的所有课程ID
     */
    List<Long> listCourseIdsBySession(SessionQueryRequest request);
}
