package org.example.newcourseselectionsystem.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.newcourseselectionsystem.domain.entity.Course;

/**
 * 课程表 Mapper
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
