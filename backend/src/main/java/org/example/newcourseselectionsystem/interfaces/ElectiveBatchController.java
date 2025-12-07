package org.example.newcourseselectionsystem.interfaces;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.domain.entity.ElectiveBatch;
import org.example.newcourseselectionsystem.infrastructure.mapper.ElectiveBatchMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选课轮次控制器
 */
@RestController
@RequestMapping("/api/elective-batches")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ElectiveBatchController {

    private final ElectiveBatchMapper electiveBatchMapper;

    /**
     * 获取所有选课轮次
     */
    @GetMapping
    public ResponseEntity<List<ElectiveBatch>> getAllBatches() {
        LambdaQueryWrapper<ElectiveBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ElectiveBatch::getStartTime);
        List<ElectiveBatch> batches = electiveBatchMapper.selectList(wrapper);

        // 动态更新状态
        LocalDateTime now = LocalDateTime.now();
        for (ElectiveBatch batch : batches) {
            updateBatchStatus(batch, now);
        }

        return ResponseEntity.ok(batches);
    }

    /**
     * 根据当前时间更新选课轮次状态
     */
    private void updateBatchStatus(ElectiveBatch batch, LocalDateTime now) {
        String oldStatus = batch.getStatus();
        if (now.isBefore(batch.getStartTime())) {
            batch.setStatus("未开始");
        } else if (now.isAfter(batch.getEndTime())) {
            batch.setStatus("已结束");
        } else {
            batch.setStatus("进行中");
        }
        System.out.println("轮次: " + batch.getBatchName() + " - " + batch.getRoundName() +
                ", 原状态: " + oldStatus + ", 新状态: " + batch.getStatus() +
                ", 开始时间: " + batch.getStartTime() + ", 结束时间: " + batch.getEndTime() +
                ", 当前时间: " + now);
    }

    /**
     * 获取当前进行中的选课轮次
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentBatch() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<ElectiveBatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElectiveBatch::getStatus, "进行中")
                .le(ElectiveBatch::getStartTime, now)
                .ge(ElectiveBatch::getEndTime, now)
                .orderByDesc(ElectiveBatch::getStartTime)
                .last("LIMIT 1");

        ElectiveBatch currentBatch = electiveBatchMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        if (currentBatch != null) {
            updateBatchStatus(currentBatch, now);
            result.put("hasBatch", true);
            result.put("batch", currentBatch);
        } else {
            // 查找即将开始的轮次
            LambdaQueryWrapper<ElectiveBatch> upcomingWrapper = new LambdaQueryWrapper<>();
            upcomingWrapper.gt(ElectiveBatch::getStartTime, now)
                    .orderByAsc(ElectiveBatch::getStartTime)
                    .last("LIMIT 1");

            ElectiveBatch upcomingBatch = electiveBatchMapper.selectOne(upcomingWrapper);

            result.put("hasBatch", false);
            if (upcomingBatch != null) {
                updateBatchStatus(upcomingBatch, now);
                result.put("upcomingBatch", upcomingBatch);
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取选课轮次
     */
    @GetMapping("/{id}")
    public ResponseEntity<ElectiveBatch> getBatchById(@PathVariable Long id) {
        ElectiveBatch batch = electiveBatchMapper.selectById(id);
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }

        // 动态更新状态
        updateBatchStatus(batch, LocalDateTime.now());

        return ResponseEntity.ok(batch);
    }
}
