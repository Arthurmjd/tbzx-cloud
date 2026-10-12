package cn.tbzx.cloud.modules.device.controller;

import cn.tbzx.cloud.common.ContextHolder;
import cn.tbzx.cloud.modules.device.dto.DeviceRegisterRequest;
import cn.tbzx.cloud.modules.device.dto.DeviceRegisterResponse;
import cn.tbzx.cloud.modules.device.dto.HeartbeatRequest;
import cn.tbzx.cloud.modules.device.dto.LatestLocationResponse;
import cn.tbzx.cloud.modules.device.entity.Device;
import cn.tbzx.cloud.modules.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /** 设备出厂注册 (开放, 鉴权在 WebConfig 中排除) */
    @PostMapping("/register")
    public DeviceRegisterResponse register(@RequestBody(required = false) DeviceRegisterRequest req) {
        return deviceService.register(req == null ? null : req.deviceName());
    }

    /** 当前家属绑定的设备 */
    @GetMapping
    public List<Device> list() {
        return deviceService.listByUser(ContextHolder.getUserId());
    }

    @PostMapping("/{deviceId}/bind")
    public void bind(@PathVariable String deviceId) {
        deviceService.bind(deviceId, ContextHolder.getUserId());
    }

    @PostMapping("/{deviceId}/unbind")
    public void unbind(@PathVariable String deviceId) {
        deviceService.unbind(deviceId, ContextHolder.getUserId());
    }

    /** 设备心跳 (ApiKey 拦截器) */
    @PostMapping("/{deviceId}/heartbeat")
    public void heartbeat(@PathVariable String deviceId, @Valid @RequestBody HeartbeatRequest req) {
        deviceService.heartbeat(deviceId, req.batteryLevel());
    }

    /** 最新位置 (家属 JWT) */
    @GetMapping("/{deviceId}/latest-location")
    public LatestLocationResponse latest(@PathVariable String deviceId) {
        return deviceService.latest(deviceId);
    }
}
