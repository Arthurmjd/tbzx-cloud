package cn.tbzx.cloud.modules.session.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TrackResponse(
        Long sessionId,
        String deviceId,
        int pointCount,
        List<TrackPoint> points) {

    public record TrackPoint(Double lat, Double lng, Double speed, LocalDateTime createdAt) {
    }
}
