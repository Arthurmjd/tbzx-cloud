package cn.tbzx.cloud.modules.alert.controller;

import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.common.ContextHolder;
import cn.tbzx.cloud.modules.alert.entity.Alert;
import cn.tbzx.cloud.modules.alert.entity.AlertImage;
import cn.tbzx.cloud.modules.alert.mapper.AlertImageMapper;
import cn.tbzx.cloud.modules.alert.mapper.AlertMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * 现场图片上传 (设备 ApiKey 鉴权) — 香橙派抓拍 → App 转发 → 云端落盘
 * 图片存 ./data/images/, URL 路径 /images/** 由 WebConfig 静态服务
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertImageController {

    private final AlertMapper alertMapper;
    private final AlertImageMapper alertImageMapper;

    @Value("${tbzx.image-dir:./data/images}")
    private String imageDir;

    @PostMapping("/{alertId}/images")
    public Map<String, Object> upload(@PathVariable Long alertId,
                                      @RequestParam("file") MultipartFile file) {
        Alert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw BizException.notFound("告警不存在: " + alertId);
        }
        // 设备只能给自己的告警传图 (AuthInterceptor 已按 X-Api-Key 设置 ContextHolder.deviceId)
        if (!alert.getDeviceId().equals(ContextHolder.getDeviceId())) {
            throw BizException.forbidden("该设备无权操作此告警");
        }
        if (file.isEmpty()) {
            throw BizException.badRequest("文件为空");
        }
        try {
            // 转绝对路径: 避免 MultipartFile.transferTo(File) 把相对路径按 Tomcat 临时目录解析
            // (否则 .\data\images 会被拼到 ...\Temp\tomcat...\work\...\.\data\images 下 -> NotFound)
            Path dir = Paths.get(imageDir).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = "alert_" + alertId + "_" + System.currentTimeMillis() + ".jpg";
            Path target = dir.resolve(filename);
            // 用 Files.copy 而非 transferTo, 彻底绕开 StandardMultipartFile/Part 的相对路径坑
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            AlertImage img = new AlertImage();
            img.setAlertId(alertId);
            img.setImagePath(target.toString());
            img.setImageUrl("/images/" + filename);
            img.setFileSize((int) file.getSize());
            alertImageMapper.insert(img);

            // 关联回 alert: has_image = 1
            alertMapper.update(null, new LambdaUpdateWrapper<Alert>()
                    .eq(Alert::getId, alertId)
                    .set(Alert::getHasImage, 1));

            return Map.of("imageId", img.getId(), "url", img.getImageUrl());
        } catch (Exception e) {
            throw new RuntimeException("图片保存失败: " + e.getMessage(), e);
        }
    }
}
