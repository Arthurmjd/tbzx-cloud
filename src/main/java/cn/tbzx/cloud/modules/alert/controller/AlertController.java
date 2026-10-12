package cn.tbzx.cloud.modules.alert.controller;

import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.modules.alert.dto.AlertReportRequest;
import cn.tbzx.cloud.modules.alert.entity.Alert;
import cn.tbzx.cloud.modules.alert.mapper.AlertMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 告警接口:
 * - GET  /api/devices/{deviceId}/alerts  家属 JWT 分页查询
 * - POST /api/devices/{deviceId}/alerts  设备 ApiKey 上报 (香橙派检测 → App 转发)
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class AlertController {

    private final AlertMapper alertMapper;

    public record PageResponse(long total, List<Alert> list) {
    }

    @GetMapping("/{deviceId}/alerts")
    public PageResponse list(@PathVariable String deviceId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        Page<Alert> result = alertMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Alert>()
                        .eq(Alert::getDeviceId, deviceId)
                        .orderByDesc(Alert::getId));
        return new PageResponse(result.getTotal(), result.getRecords());
    }

    /** 设备上报告警, 返回 alertId 供后续图片关联 */
    @PostMapping("/{deviceId}/alerts")
    public Map<String, Object> report(@PathVariable String deviceId,
                                      @RequestBody AlertReportRequest req) {
        if (req.alertType() == null || req.alertType().isBlank()) {
            throw BizException.badRequest("alertType 不能为空");
        }
        Alert alert = new Alert();
        alert.setDeviceId(deviceId);
        alert.setAlertType(req.alertType());
        alert.setSeverity(req.severity() == null ? 1 : req.severity());
        alert.setDescription(req.description());
        alert.setLat(req.lat());
        alert.setLng(req.lng());
        alert.setHasImage(0);
        alertMapper.insert(alert);
        return Map.of("alertId", alert.getId());
    }
}
