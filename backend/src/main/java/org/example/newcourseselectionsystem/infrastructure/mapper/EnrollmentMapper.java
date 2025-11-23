package org.example.newcourseselectionsystem.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.newcourseselectionsystem.domain.entity.Enrollment;

/**
 * 选课记录 Mapper
 */
@Mapper
public interface EnrollmentMapper extends BaseMapper<Enrollment> {
}
