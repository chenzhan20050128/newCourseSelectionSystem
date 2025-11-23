package org.example.newcourseselectionsystem.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.newcourseselectionsystem.domain.entity.Instructor;

/**
 * 教师表 Mapper
 */
@Mapper
public interface InstructorMapper extends BaseMapper<Instructor> {
}
