package cn.tbzx.cloud.modules.location.service;

import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.common.GeoUtils;
import cn.tbzx.cloud.modules.alert.entity.Alert;
import cn.tbzx.cloud.modules.alert.mapper.AlertMapper;
import cn.tbzx.cloud.modules.device.entity.Device;
import cn.tbzx.cloud.modules.device.mapper.DeviceMapper;
import cn.tbzx.cloud.modules.location.dto.LocationReportRequest.LocationPoint;
import cn.tbzx.cloud.modules.location.entity.DeviceLocation;
import cn.tbzx.cloud.modules.location.mapper.DeviceLocationMapper;
import cn.tbzx.cloud.modules.session.entity.TravelSession;
import cn.tbzx.cloud.modules.session.mapper.TravelSessionMapper;
import cn.tbzx.cloud.modules.zone.entity.SafeZone;
import cn.tbzx.cloud.modules.zone.mapper.SafeZoneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 位置上报管线 (单事务):
 *  1. 幂等落库 (INSERT IGNORE, 重复点自动去重)
 *  2. session 自动管理 (无进行中 → 自动开新出行)
 *  3. 里程累加 (haversine, 跳变 >100m 视为漂移不计)
 *  4. 安全区域越界判定 (上一真实点 inside → 当前 outside, 同 zone 60s 去重抗抖动)
 *  5. 更新 device 最新位置 (家属端轮询零热表查询)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    /** 相邻两点 >100m 不计入里程 (步行不可能, 视为定位漂移) */
    private static final double JUMP_FILTER_M = 100.0;

    private final DeviceLocationMapper locationMapper;
    private final DeviceMapper deviceMapper;
    private final TravelSessionMapper sessionMapper;
    private final SafeZoneMapper zoneMapper;
    private final AlertMapper alertMapper;

    @Transactional
    public int report(String deviceId, List<LocationPoint> points) {
        if (points == null || points.isEmpty()) {
            return 0;
        }
        List<LocationPoint> sorted = points.stream()
                .sorted(Comparator.comparing(LocationPoint::timestamp))
                .toList();

        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, deviceId));
        if (device == null) {
            throw BizException.notFound("设备不存在: " + deviceId);
        }

        TravelSession session = resolveSession(device, sorted);

        // 3+4. 里程累加 + 安全区域越界判定 (基于"更新前的上一位置"做边沿检测)
        double deltaMeters = 0;
        int newAlerts = 0;
        int accepted = 0;
        Double prevLat = device.getLastLat();
        Double prevLng = device.getLastLng();
        for (LocationPoint p : sorted) {
            accepted += insertIgnore(deviceId, p, session.getId());
            if (prevLat != null && prevLng != null) {
                double d = GeoUtils.haversineMeters(prevLat, prevLng, p.lat(), p.lng());
                if (d <= JUMP_FILTER_M) {
                    deltaMeters += d;
                }
                newAlerts += checkGeofence(deviceId, prevLat, prevLng, p);
            }
            prevLat = p.lat();
            prevLng = p.lng();
        }

        // 累计到 session
        long delta = Math.round(deltaMeters);
        if (delta > 0) {
            sessionMapper.update(null, new LambdaUpdateWrapper<TravelSession>()
                    .eq(TravelSession::getId, session.getId())
                    .setSql("distance_m = distance_m + " + delta));
        }
        if (newAlerts > 0) {
            sessionMapper.update(null, new LambdaUpdateWrapper<TravelSession>()
                    .eq(TravelSession::getId, session.getId())
                    .setSql("alert_count = alert_count + " + newAlerts));
        }

        // 5. 更新设备最新位置 (单行 UPDATE)
        LocationPoint last = sorted.get(sorted.size() - 1);
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, device.getId())
                .set(Device::getLastLat, last.lat())
                .set(Device::getLastLng, last.lng())
                .set(Device::getLastPointAt, last.timestamp())
                .set(Device::getLastHeartbeatAt, LocalDateTime.now()));

        log.debug("设备 {} 上报 {} 点, 去重后入库 {} 点, 里程 +{}m, 新告警 {} 条",
                deviceId, points.size(), accepted, delta, newAlerts);
        return accepted;
    }

    private int insertIgnore(String deviceId, LocationPoint p, Long sessionId) {
        DeviceLocation loc = new DeviceLocation();
        loc.setDeviceId(deviceId);
        loc.setLat(p.lat());
        loc.setLng(p.lng());
        loc.setAccuracy(p.accuracy());
        loc.setSpeed(p.speed());
        loc.setSessionId(sessionId);
        loc.setPointTs(p.timestamp());
        return locationMapper.insertIgnore(loc);
    }

    /** 无进行中 session → 用本批首点自动开新出行, 并写回 device.current_session_id */
    private TravelSession resolveSession(Device device, List<LocationPoint> sorted) {
        if (device.getCurrentSessionId() != null) {
            TravelSession existing = sessionMapper.selectById(device.getCurrentSessionId());
            if (existing != null && existing.getStatus() == 0) {
                return existing;
            }
        }
        LocationPoint first = sorted.get(0);
        TravelSession s = new TravelSession();
        s.setDeviceId(device.getDeviceId());
        s.setStartTime(first.timestamp());
        s.setStartLat(first.lat());
        s.setStartLng(first.lng());
        s.setDistanceM(0);
        s.setAlertCount(0);
        s.setStatus(0);
        sessionMapper.insert(s);
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, device.getId())
                .set(Device::getCurrentSessionId, s.getId()));
        return s;
    }

    /** 返回本次新插入的告警数 (0 或 1) */
    private int checkGeofence(String deviceId, double prevLat, double prevLng, LocationPoint cur) {
        List<SafeZone> zones = zoneMapper.selectList(new LambdaQueryWrapper<SafeZone>()
                .eq(SafeZone::getDeviceId, deviceId)
                .eq(SafeZone::getActive, 1));
        int inserted = 0;
        for (SafeZone z : zones) {
            boolean wasInside = GeoUtils.insideCircle(prevLat, prevLng, z.getCenterLat(), z.getCenterLng(), z.getRadiusM());
            boolean nowOutside = !GeoUtils.insideCircle(cur.lat(), cur.lng(), z.getCenterLat(), z.getCenterLng(), z.getRadiusM());
            if (!wasInside || !nowOutside) {
                continue;
            }
            long recent = alertMapper.countRecent(deviceId, "GEOFENCE_EXIT", z.getId(), 60);
            if (recent > 0) {
                continue;   // 同 zone 60s 内已告警, 抗边界抖动
            }
            Alert alert = new Alert();
            alert.setDeviceId(deviceId);
            alert.setAlertType("GEOFENCE_EXIT");
            alert.setSeverity(2);
            alert.setDescription("已离开安全区域「" + z.getZoneName() + "」");
            alert.setRefId(z.getId());
            alert.setLat(cur.lat());
            alert.setLng(cur.lng());
            alert.setHasImage(0);
            alertMapper.insert(alert);
            inserted++;
        }
        return inserted;
    }
}
