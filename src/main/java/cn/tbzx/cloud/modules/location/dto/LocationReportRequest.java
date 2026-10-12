package cn.tbzx.cloud.modules.location.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record LocationReportRequest(
        @NotEmpty(message = "locations 不能为空")
        @Size(max = 50, message = "单批最多 50 个点")
        @Valid List<@Valid LocationPoint> locations) {

    public record LocationPoint(
            @NotNull(message = "lat 不能为空") Double lat,
            @NotNull(message = "lng 不能为空") Double lng,
            Double accuracy,
            Double speed,
            @NotNull(message = "timestamp 不能为空") LocalDateTime timestamp) {
    }
}
