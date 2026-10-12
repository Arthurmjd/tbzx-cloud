package cn.tbzx.cloud.modules.device.service;

import cn.tbzx.cloud.common.ApiKeyUtils;
import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.modules.device.dto.DeviceRegisterResponse;
import cn.tbzx.cloud.modules.device.dto.LatestLocationResponse;
import cn.tbzx.cloud.modules.device.entity.Device;
import cn.tbzx.cloud.modules.device.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;

    @Value("${tbzx.online-threshold-seconds}")
    private long onlineThresholdSeconds;

    /** 设备出厂注册 (开放接口): 生成编号 + 密钥, 明文仅此一次 */
    public DeviceRegisterResponse register(String deviceName) {
        String deviceId;
        do {
            deviceId = ApiKeyUtils.newDeviceId();
        } while (deviceMapper.selectCount(new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, deviceId)) > 0);

        String apiKey = ApiKeyUtils.newApiKey();
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceName(deviceName == null || deviceName.isBlank() ? "助行设备" : deviceName);
        device.setApiKey(ApiKeyUtils.sha256Hex(apiKey));
        deviceMapper.insert(device);
        return new DeviceRegisterResponse(deviceId, apiKey, device.getDeviceName());
    }

    /** 当前家属绑定的设备列表 */
    public List<Device> listByUser(Long userId) {
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getUserId, userId)
                .orderByAsc(Device::getId));
    }

    /** 绑定 (未被占用时可认领; 已属于自己则幂等成功) */
    public void bind(String deviceId, Long userId) {
        Device device = mustGet(deviceId);
        if (device.getUserId() != null && !device.getUserId().equals(userId)) {
            throw BizException.forbidden("该设备已被其他账号绑定");
        }
        Device update = new Device();
        update.setId(device.getId());
        update.setUserId(userId);
        deviceMapper.updateById(update);
    }

    public void unbind(String deviceId, Long userId) {
        Device device = mustGet(deviceId);
        if (device.getUserId() == null || !device.getUserId().equals(userId)) {
            throw BizException.forbidden("该设备不属于当前账号");
        }
        Device update = new Device();
        update.setId(device.getId());
        update.setUserId(null);
        deviceMapper.updateById(update);
    }

    /** 设备心跳: 刷新电量 + 心跳时间 */
    public void heartbeat(String deviceId, Integer batteryLevel) {
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getDeviceId, deviceId)
                .set(Device::getBatteryLevel, batteryLevel)
                .set(Device::getLastHeartbeatAt, LocalDateTime.now()));
    }

    /** 最新位置: 单行查询, online = 最后心跳距今 <= 阈值 */
    public LatestLocationResponse latest(String deviceId) {
        Device d = mustGet(deviceId);
        boolean online = d.getLastHeartbeatAt() != null
                && d.getLastHeartbeatAt().isAfter(LocalDateTime.now().minusSeconds(onlineThresholdSeconds));
        return new LatestLocationResponse(
                d.getDeviceId(), d.getDeviceName(), online,
                d.getLastLat(), d.getLastLng(), d.getBatteryLevel(),
                d.getLastHeartbeatAt(), d.getLastPointAt(), d.getCurrentSessionId());
    }

    public Device mustGet(String deviceId) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, deviceId));
        if (device == null) {
            throw BizException.notFound("设备不存在: " + deviceId);
        }
        return device;
    }
}
