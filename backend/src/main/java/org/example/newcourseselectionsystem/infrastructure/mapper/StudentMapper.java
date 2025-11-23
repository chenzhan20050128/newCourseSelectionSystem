package org.example.newcourseselectionsystem.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.newcourseselectionsystem.domain.entity.Student;

/**
 * 学生表 Mapper
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
