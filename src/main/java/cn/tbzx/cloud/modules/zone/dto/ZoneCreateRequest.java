package cn.tbzx.cloud.modules.zone.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ZoneCreateRequest(
        @NotBlank(message = "区域名称不能为空") String zoneName,
        @NotNull(message = "圆心纬度不能为空") Double centerLat,
        @NotNull(message = "圆心经度不能为空") Double centerLng,
        @NotNull(message = "半径不能为空")
        @Min(value = 50, message = "半径最小 50 米")
        @Max(value = 5000, message = "半径最大 5000 米") Integer radiusM) {
}
