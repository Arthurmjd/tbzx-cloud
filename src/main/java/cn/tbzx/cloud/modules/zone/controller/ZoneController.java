package cn.tbzx.cloud.modules.zone.controller;

import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.common.ContextHolder;
import cn.tbzx.cloud.modules.device.entity.Device;
import cn.tbzx.cloud.modules.device.mapper.DeviceMapper;
import cn.tbzx.cloud.modules.zone.dto.ZoneCreateRequest;
import cn.tbzx.cloud.modules.zone.entity.SafeZone;
import cn.tbzx.cloud.modules.zone.mapper.SafeZoneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 安全区域 (家属 JWT)。
 * 归属校验: zone.device_id → device.user_id = 当前登录家属
 * (v2.1 起 safe_zone 不存 user_id, 换绑自动跟随)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ZoneController {

    private final SafeZoneMapper zoneMapper;
    private final DeviceMapper deviceMapper;

    @GetMapping("/devices/{deviceId}/zones")
    public List<SafeZone> list(@PathVariable String deviceId) {
        checkOwnership(deviceId);
        return zoneMapper.selectList(new LambdaQueryWrapper<SafeZone>()
                .eq(SafeZone::getDeviceId, deviceId)
                .orderByAsc(SafeZone::getId));
    }

    @PostMapping("/devices/{deviceId}/zones")
    public SafeZone create(@PathVariable String deviceId, @Valid @RequestBody ZoneCreateRequest req) {
        checkOwnership(deviceId);
        SafeZone zone = new SafeZone();
        zone.setDeviceId(deviceId);
        zone.setZoneName(req.zoneName());
        zone.setCenterLat(req.centerLat());
        zone.setCenterLng(req.centerLng());
        zone.setRadiusM(req.radiusM());
        zone.setActive(1);
        zoneMapper.insert(zone);
        return zone;
    }

    @PatchMapping("/zones/{zoneId}/toggle")
    public void toggle(@PathVariable Long zoneId) {
        SafeZone zone = mustOwn(zoneId);
        zoneMapper.update(null, new LambdaUpdateWrapper<SafeZone>()
                .eq(SafeZone::getId, zoneId)
                .set(SafeZone::getActive, zone.getActive() == 1 ? 0 : 1));
    }

    @DeleteMapping("/zones/{zoneId}")
    public void delete(@PathVariable Long zoneId) {
        mustOwn(zoneId);
        zoneMapper.deleteById(zoneId);
    }

    private void checkOwnership(String deviceId) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, deviceId));
        if (device == null) {
            throw BizException.notFound("设备不存在: " + deviceId);
        }
        if (device.getUserId() == null || !device.getUserId().equals(ContextHolder.getUserId())) {
            throw BizException.forbidden("无权操作该设备");
        }
    }

    private SafeZone mustOwn(Long zoneId) {
        SafeZone zone = zoneMapper.selectById(zoneId);
        if (zone == null) {
            throw BizException.notFound("安全区域不存在: " + zoneId);
        }
        checkOwnership(zone.getDeviceId());
        return zone;
    }
}
