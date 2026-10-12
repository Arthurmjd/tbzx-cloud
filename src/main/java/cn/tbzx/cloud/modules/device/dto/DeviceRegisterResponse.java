package cn.tbzx.cloud.modules.device.dto;

/**
 * 设备注册响应: apiKey 明文仅此一次返回, 云端只存 SHA-256
 */
public record DeviceRegisterResponse(String deviceId, String apiKey, String deviceName) {
}
