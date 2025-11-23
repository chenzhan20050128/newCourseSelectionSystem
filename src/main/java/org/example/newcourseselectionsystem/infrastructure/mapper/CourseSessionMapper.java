package org.example.newcourseselectionsystem.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.newcourseselectionsystem.domain.entity.CourseSession;

/**
 * 课程节次 Mapper
 */
@Mapper
public interface CourseSessionMapper extends BaseMapper<CourseSession> {
}
