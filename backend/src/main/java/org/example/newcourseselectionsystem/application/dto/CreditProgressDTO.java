package org.example.newcourseselectionsystem.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditProgressDTO {
    private String type;      // 课程类型
    private Integer earned;   // 已选/已修学分
    private Integer required; // 总需学分
}