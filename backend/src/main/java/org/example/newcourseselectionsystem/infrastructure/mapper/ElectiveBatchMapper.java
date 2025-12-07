package org.example.newcourseselectionsystem.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.newcourseselectionsystem.domain.entity.ElectiveBatch;

/**
 * 选课轮次Mapper
 */
@Mapper
public interface ElectiveBatchMapper extends BaseMapper<ElectiveBatch> {
}
