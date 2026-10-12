package cn.tbzx.cloud.modules.location.dto;

import java.time.LocalDateTime;

/**
 * 家属侧实时轨迹尾巴的单点 (精简字段, 不暴露 device_id/created_at)
 * 实时页拉最近 N 个点画淡色 polyline, marker 在末端, 一眼看出走向
 */
public record RecentLocationPoint(
        Double lat,
        Double lng,
        Double speed,
        Double accuracy,
        LocalDateTime pointTs) {
}
