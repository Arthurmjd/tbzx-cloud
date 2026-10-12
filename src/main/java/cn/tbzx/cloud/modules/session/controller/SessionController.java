package cn.tbzx.cloud.modules.session.controller;

import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.modules.location.entity.DeviceLocation;
import cn.tbzx.cloud.modules.location.mapper.DeviceLocationMapper;
import cn.tbzx.cloud.modules.session.dto.TrackResponse;
import cn.tbzx.cloud.modules.session.entity.TravelSession;
import cn.tbzx.cloud.modules.session.mapper.TravelSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 出行记录与轨迹 (家属 JWT)
 * 轨迹由 session 时间窗推导 (track 不走 device_location.session_id 外键列)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SessionController {

    /** 返回点数上限 (DP 后仍超出再等间隔裁尾) */
    private static final int MAX_TRACK_POINTS = 500;
    /** Douglas-Peucker 垂直距离阈值 (米): 小于此值视为可合并的冗余点/漂移 */
    private static final double DP_EPSILON_M = 5.0;
    private static final double EARTH_R_M = 6371000.0;

    private final TravelSessionMapper sessionMapper;
    private final DeviceLocationMapper locationMapper;

    @GetMapping("/devices/{deviceId}/sessions")
    public List<TravelSession> list(@PathVariable String deviceId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return sessionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new LambdaQueryWrapper<TravelSession>()
                        .eq(TravelSession::getDeviceId, deviceId)
                        .orderByDesc(TravelSession::getId)).getRecords();
    }

    @GetMapping("/sessions/{sessionId}/track")
    public TrackResponse track(@PathVariable Long sessionId) {
        TravelSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BizException.notFound("出行记录不存在: " + sessionId);
        }
        List<DeviceLocation> raw = locationMapper.selectList(new LambdaQueryWrapper<DeviceLocation>()
                .eq(DeviceLocation::getDeviceId, session.getDeviceId())
                .eq(DeviceLocation::getSessionId, sessionId)
                .orderByAsc(DeviceLocation::getPointTs));
        List<DeviceLocation> thinned = thin(raw);
        List<TrackResponse.TrackPoint> points = thinned.stream()
                .map(p -> new TrackResponse.TrackPoint(p.getLat(), p.getLng(), p.getSpeed(), p.getPointTs()))
                .toList();
        return new TrackResponse(sessionId, session.getDeviceId(), points.size(), points);
    }

    /** Douglas-Peucker 抽稀: 保弯道拐点, 去静止漂移; DP 后仍超上限则等间隔裁尾 */
    private List<DeviceLocation> thin(List<DeviceLocation> raw) {
        if (raw.size() < 3) {
            return raw;
        }
        double lat0Rad = Math.toRadians(
                (raw.get(0).getLat() + raw.get(raw.size() - 1).getLat()) / 2.0);
        List<DeviceLocation> dp = douglasPeucker(raw, DP_EPSILON_M, lat0Rad);
        if (dp.size() <= MAX_TRACK_POINTS) {
            return dp;
        }
        int stride = (int) Math.ceil((double) dp.size() / MAX_TRACK_POINTS);
        List<DeviceLocation> out = new ArrayList<>(MAX_TRACK_POINTS + 1);
        for (int i = 0; i < dp.size(); i += stride) {
            out.add(dp.get(i));
        }
        DeviceLocation last = dp.get(dp.size() - 1);
        if (out.get(out.size() - 1) != last) {
            out.add(last);
        }
        return out;
    }

    private List<DeviceLocation> douglasPeucker(List<DeviceLocation> pts, double eps, double lat0Rad) {
        boolean[] keep = new boolean[pts.size()];
        keep[0] = keep[pts.size() - 1] = true;
        markKeep(pts, 0, pts.size() - 1, keep, eps, lat0Rad);
        List<DeviceLocation> out = new ArrayList<>(pts.size());
        for (int i = 0; i < pts.size(); i++) {
            if (keep[i]) {
                out.add(pts.get(i));
            }
        }
        return out;
    }

    private void markKeep(List<DeviceLocation> pts, int lo, int hi, boolean[] keep, double eps, double lat0Rad) {
        if (hi - lo < 2) {
            return;
        }
        double[] a = toMeters(pts.get(lo), lat0Rad);
        double[] b = toMeters(pts.get(hi), lat0Rad);
        double maxD = -1;
        int maxIdx = -1;
        for (int i = lo + 1; i < hi; i++) {
            double[] p = toMeters(pts.get(i), lat0Rad);
            double d = perpMeters(p, a, b);
            if (d > maxD) {
                maxD = d;
                maxIdx = i;
            }
        }
        if (maxD > eps) {
            keep[maxIdx] = true;
            markKeep(pts, lo, maxIdx, keep, eps, lat0Rad);
            markKeep(pts, maxIdx, hi, keep, eps, lat0Rad);
        }
    }

    /** 点 P 到线段 AB 的垂直距离 (米), Equirectangular 投影 */
    private double perpMeters(double[] p, double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double len2 = dx * dx + dy * dy;
        if (len2 == 0) {
            return Math.hypot(p[0] - a[0], p[1] - a[1]);
        }
        double t = ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double px = a[0] + t * dx;
        double py = a[1] + t * dy;
        return Math.hypot(p[0] - px, p[1] - py);
    }

    private double[] toMeters(DeviceLocation loc, double lat0Rad) {
        double x = Math.toRadians(loc.getLng()) * Math.cos(lat0Rad) * EARTH_R_M;
        double y = Math.toRadians(loc.getLat()) * EARTH_R_M;
        return new double[]{x, y};
    }
}
