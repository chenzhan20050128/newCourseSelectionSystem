package org.example.newcourseselectionsystem.application.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 毕业学分要求常量
 */
public final class GraduationRequirements {
    private GraduationRequirements() {}

    // 定义课程类型到学分的映射（顺序保留用于优先级展示）
    public static final Map<String, Integer> CREDIT_REQUIREMENTS;

    static {
        Map<String, Integer> m = new LinkedHashMap<>();
        // 通识通修课程
        m.put("大学数学", 14);   // 微积分I(5)+微积分II(5)+线性代数(4)
        m.put("大学英语", 8);    // 大学英语(一)(4)+大学英语(二)(4)
        m.put("思政", 20);       // 形势与政策(1 * 8)+马原(3)+思修(3)+近代史(3)+毛概(5)
        m.put("军事课", 4);      // 军事技能训练(2)+军事理论(2)
        m.put("体育", 4);    // 体育(一)到体育(四)(1 * 4)
        m.put("美育", 2);
        m.put("悦读", 1);
        m.put("科光", 1);
        m.put("通识课", 10);     // 14-2-1-1=10
        

        // 学科专业课程
        m.put("学科基础课程", 64);  // 准入+准出课程
//        m.put("专业核心课程", 35);  // 核心课程
        m.put("专业选修", 33);      // 18(方向模块)+15(任选)

        // 毕业论文
        m.put("毕业论文", 6);

        CREDIT_REQUIREMENTS = Collections.unmodifiableMap(m);
    }
}
