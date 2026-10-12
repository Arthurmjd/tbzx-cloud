package cn.tbzx.cloud.modules.device.dto;

import java.time.LocalDateTime;

/**
 * 最新位置响应 (直接读 device 单行, 不扫轨迹表)
 */
public record LatestLocationResponse(
        String deviceId,
        String deviceName,
        Boolean online,
        Double lastLat,
        Double lastLng,
        Integer batteryLevel,
        LocalDateTime lastHeartbeatAt,
        LocalDateTime lastPointAt,
        Long currentSessionId) {
}
