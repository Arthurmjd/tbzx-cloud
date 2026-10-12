package cn.tbzx.cloud.modules.device.dto;

import jakarta.validation.constraints.NotNull;

public record HeartbeatRequest(@NotNull(message = "电量不能为空") Integer batteryLevel) {
}
