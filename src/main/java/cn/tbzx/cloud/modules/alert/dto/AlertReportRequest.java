package cn.tbzx.cloud.modules.alert.dto;

/**
 * 设备侧告警上报 (香橙派检测 → App 转发 → 云端写入)
 * timestamp 由 App 补 ISO 字符串; 云端用 created_at (服务端时间), 不采纳客户端 ts
 */
public record AlertReportRequest(
        String alertType,
        Integer severity,
        String description,
        Double lat,
        Double lng,
        String timestamp) {
}
