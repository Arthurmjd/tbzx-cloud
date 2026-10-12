package cn.tbzx.cloud.modules.session.task;

import cn.tbzx.cloud.modules.device.entity.Device;
import cn.tbzx.cloud.modules.device.mapper.DeviceMapper;
import cn.tbzx.cloud.modules.session.entity.TravelSession;
import cn.tbzx.cloud.modules.session.mapper.TravelSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * session 静默自动关闭:
 * 每 60s 扫描进行中的 session, 设备最后轨迹点距今超过阈值 → 用最后位置收尾
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCloseScheduler {

    private final TravelSessionMapper sessionMapper;
    private final DeviceMapper deviceMapper;

    @Value("${tbzx.session-idle-close-seconds}")
    private long idleCloseSeconds;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void closeIdleSessions() {
        List<TravelSession> open = sessionMapper.selectList(
                new LambdaQueryWrapper<TravelSession>().eq(TravelSession::getStatus, 0));
        if (open.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (TravelSession s : open) {
            try {
                Device device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, s.getDeviceId()));
                if (device == null || device.getLastPointAt() == null
                        || device.getLastPointAt().isAfter(now.minusSeconds(idleCloseSeconds))) {
                    continue;   // 仍在活跃上报
                }
                sessionMapper.update(null, new LambdaUpdateWrapper<TravelSession>()
                        .eq(TravelSession::getId, s.getId())
                        .set(TravelSession::getEndTime, device.getLastPointAt())
                        .set(TravelSession::getEndLat, device.getLastLat())
                        .set(TravelSession::getEndLng, device.getLastLng())
                        .set(TravelSession::getStatus, 1));
                deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                        .eq(Device::getId, device.getId())
                        .set(Device::getCurrentSessionId, null));
                log.info("session {} 已静默 {}s, 自动关闭", s.getId(), idleCloseSeconds);
            } catch (Exception e) {
                // 单条失败不拖垮整个调度线程, 下一轮继续
                log.error("关闭 session {} 失败", s.getId(), e);
            }
        }
    }
}
