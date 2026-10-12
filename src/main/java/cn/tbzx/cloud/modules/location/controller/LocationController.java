package cn.tbzx.cloud.modules.location.controller;

import cn.tbzx.cloud.modules.location.dto.LocationReportRequest;
import cn.tbzx.cloud.modules.location.dto.RecentLocationPoint;
import cn.tbzx.cloud.modules.location.entity.DeviceLocation;
import cn.tbzx.cloud.modules.location.mapper.DeviceLocationMapper;
import cn.tbzx.cloud.modules.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 设备位置: 写 (ApiKey) + 家属实时轨迹尾巴读 (JWT)
 * - POST /{deviceId}/locations      设备批量上报 (ApiKey 拦截器鉴权)
 * - GET  /{deviceId}/recent-locations 家属拉最近 N 点画尾巴 (JWT, AuthInterceptor 按 method 分流)
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final DeviceLocationMapper locationMapper;

    @PostMapping("/{deviceId}/locations")
    public Map<String, Object> report(@PathVariable String deviceId,
                                      @Valid @RequestBody LocationReportRequest req) {
        int accepted = locationService.report(deviceId, req.locations());
        return Map.of("accepted", accepted);
    }

    /** 家属实时页轨迹尾巴: 最近 N 点 (默认30, 上限200), 时间升序便于前端直接画线 */
    @GetMapping("/{deviceId}/recent-locations")
    public List<RecentLocationPoint> recent(@PathVariable String deviceId,
                                            @RequestParam(defaultValue = "30") int limit) {
        int n = Math.max(1, Math.min(limit, 200));
        List<DeviceLocation> desc = locationMapper.selectRecentDesc(deviceId, n);
        Collections.reverse(desc);   // 降序→升序, 末端=最新点
        return desc.stream()
                .map(p -> new RecentLocationPoint(p.getLat(), p.getLng(),
                        p.getSpeed(), p.getAccuracy(), p.getPointTs()))
                .toList();
    }
}
