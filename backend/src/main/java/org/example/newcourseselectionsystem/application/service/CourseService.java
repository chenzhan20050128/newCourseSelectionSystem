package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseAttributeQueryRequest;
import org.example.newcourseselectionsystem.application.request.CourseQueryRequest;
import org.example.newcourseselectionsystem.application.request.SessionQueryRequest;
import org.example.newcourseselectionsystem.application.request.CombinedCourseQueryRequest;

import java.util.List;

/**
 * 课程相关业务
 */
public interface CourseService {

    /**
     * 合并后的课程查询：支持课程字段筛选 + 节次条件筛选（可同时存在）
     */
    List<CourseWithSessionsDTO> searchCourses(CombinedCourseQueryRequest request);

    /**
     * 根据课程表的全部字段进行组合过滤，并返回附带节次的课程信息
     */
    List<CourseWithSessionsDTO> listCoursesByCondition(CourseQueryRequest request);

    /**
     * 根据节次条件找到符合条件的所有课程，返回携带节次信息的完整课程信息
     */
    List<CourseWithSessionsDTO> listCoursesBySession(SessionQueryRequest request);

    /**
     * 根据条件筛选课程，然后返回指定属性的所有唯一值集合
     * 
     * @param request 包含查询条件和属性名称的请求
     * @return 属性值的唯一集合
     */
    List<Object> getAttributeValues(CourseAttributeQueryRequest request);
}
