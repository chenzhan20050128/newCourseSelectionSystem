import csv
import math
import os
import random
import shutil

# 目标 CSV 路径
CSV_PATH = r"c:\Users\Administrator\Desktop\大三上学期\人机交互系统\newCourseSelectionSystem\newCourseSelectionSystem\backend\src\main\resources\sql\courses.csv"

# 可调参数
LOWER_RATIO = 0.0  # 最低比率（已选人数/容量）
UPPER_RATIO = 2.0  # 最高比率（已选人数/容量）


def pick_enrolled(
    capacity: int, lower_ratio: float = LOWER_RATIO, upper_ratio: float = UPPER_RATIO
) -> int:
    """
    根据容量生成随机已选人数：
    - 区间为 [ceil(lower_ratio * capacity), ceil(upper_ratio * capacity)]
    - capacity <= 0 时返回 0
    - 确保下界 <= 上界
    """
    if capacity <= 0:
        return 0

    # 计算范围边界
    low = math.ceil(lower_ratio * capacity)
    high = math.ceil(upper_ratio * capacity)

    # 确保 low <= high
    if low > high:
        low, high = high, low

    # 确保至少为 0
    low = max(0, low)
    high = max(low, high)  # 确保 high >= low

    return random.randint(low, high)


def validate_ratios():
    """验证比率参数是否合理"""
    if LOWER_RATIO < 0:
        raise ValueError(f"LOWER_RATIO 不能小于 0: {LOWER_RATIO}")
    if UPPER_RATIO < 0:
        raise ValueError(f"UPPER_RATIO 不能小于 0: {UPPER_RATIO}")
    if LOWER_RATIO > UPPER_RATIO:
        print(
            f"警告: LOWER_RATIO({LOWER_RATIO}) > UPPER_RATIO({UPPER_RATIO})，将自动交换"
        )


def main():
    # 验证参数
    validate_ratios()

    if not os.path.exists(CSV_PATH):
        raise FileNotFoundError(f"找不到文件：{CSV_PATH}")

    # 备份原文件
    backup_path = CSV_PATH + ".bak"
    shutil.copy2(CSV_PATH, backup_path)
    print(f"已备份到：{backup_path}")
    print(f"参数设置: 比率范围 = {LOWER_RATIO:.1f} ~ {UPPER_RATIO:.1f} 倍容量")

    # 读取
    rows = []
    with open(CSV_PATH, "r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        if not fieldnames:
            raise ValueError("未能读取到表头，请检查 CSV 格式。")

        # 简单健壮性校验
        required_cols = {"capacity", "enrolled_count"}
        if not required_cols.issubset(set(fieldnames)):
            raise ValueError(f"CSV 缺少必要列：{required_cols - set(fieldnames)}")

        stats = {
            "total_rows": 0,
            "updated_rows": 0,
            "zero_capacity": 0,
            "min_enrolled": float("inf"),
            "max_enrolled": 0,
            "over_capacity": 0,
        }

        for row in reader:
            stats["total_rows"] += 1

            # 解析 capacity
            cap_str = (row.get("capacity") or "").strip()
            try:
                capacity = int(cap_str)
            except ValueError:
                # 无法解析容量时，保守设为 0
                capacity = 0
                stats["zero_capacity"] += 1

            if capacity > 0:
                # 生成随机已选人数
                new_enrolled = pick_enrolled(capacity, LOWER_RATIO, UPPER_RATIO)
                row["enrolled_count"] = str(new_enrolled)
                stats["updated_rows"] += 1

                # 统计信息
                stats["min_enrolled"] = min(stats["min_enrolled"], new_enrolled)
                stats["max_enrolled"] = max(stats["max_enrolled"], new_enrolled)
                if new_enrolled > capacity:
                    stats["over_capacity"] += 1
            else:
                # 容量为 0 的课程，已选人数也设为 0
                row["enrolled_count"] = "0"

            # 确保空值写出为空字符串
            for k in fieldnames:
                if row.get(k) is None:
                    row[k] = ""

            rows.append(row)

    # 写回
    with open(CSV_PATH, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, quoting=csv.QUOTE_ALL)
        writer.writeheader()
        writer.writerows(rows)

    # 输出统计信息
    print(f"\n处理完成！统计信息：")
    print(f"总行数: {stats['total_rows']}")
    print(f"更新行数: {stats['updated_rows']}")
    print(f"零容量课程: {stats['zero_capacity']}")
    if stats["updated_rows"] > 0:
        print(f"已选人数范围: {stats['min_enrolled']} ~ {stats['max_enrolled']}")
        print(
            f"超容量课程数: {stats['over_capacity']} ({(stats['over_capacity']/stats['updated_rows'])*100:.1f}%)"
        )
    print(f"\n文件已写回：{CSV_PATH}")


if __name__ == "__main__":
    main()
