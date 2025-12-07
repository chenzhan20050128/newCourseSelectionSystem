package org.example.newcourseselectionsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 选课轮次实体
 */
@Data
@TableName("elective_batches")
public class ElectiveBatch {

    @TableId(value = "batch_id", type = IdType.AUTO)
    private Long batchId;

    private String batchName; // 选课轮次名称

    private String roundName; // 轮次名称（第一轮、第二轮等）

    private LocalDateTime startTime; // 开始时间

    private LocalDateTime endTime; // 结束时间

    private String selectionMode; // 选课模式

    private String selectionStrategy; // 选课策略

    private String status; // 状态：未开始、进行中、已结束

    private String description; // 说明
}
