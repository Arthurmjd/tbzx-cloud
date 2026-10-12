package cn.tbzx.cloud.interceptor;

import cn.tbzx.cloud.common.ApiKeyUtils;
import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.common.ContextHolder;
import cn.tbzx.cloud.common.JwtUtils;
import cn.tbzx.cloud.modules.device.mapper.DeviceMapper;
import com.github.benmanes.caffeine.cache.Cache;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * 统一鉴权拦截器 — 按方法 + 路径分流, 解决"同路径不同方法"的冲突
 * (例如 GET /api/devices/{id}/alerts 是家属 JWT 读, POST 同路径是设备 ApiKey 写)
 *
 * - 开放: /api/auth/**, /api/devices/register
 * - 设备写 (POST): heartbeat / locations / alerts / images  → X-Api-Key
 * - 其余 /api/**: 家属 JWT
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final DeviceMapper deviceMapper;
    private final Cache<String, String> apiKeyCache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 开放端点
        if (path.startsWith("/api/auth/") || path.equals("/api/devices/register")) {
            return true;
        }

        // 静态资源 (图片) 放行, 由 ResourceHandler 直接服务
        if (path.startsWith("/images/")) {
            return true;
        }

        // 设备写: POST 到下列路径 → X-Api-Key
        if ("POST".equals(method) && isDeviceWritePath(path)) {
            String apiKey = request.getHeader("X-Api-Key");
            if (apiKey == null || apiKey.isBlank()) {
                throw BizException.unauthorized("缺少 X-Api-Key 头");
            }
            String hash = ApiKeyUtils.sha256Hex(apiKey);
            String deviceId = apiKeyCache.get(hash, k -> deviceMapper.selectDeviceIdByApiKeyHash(k));
            if (deviceId == null) {
                throw BizException.unauthorized("设备密钥无效");
            }
            // 路径含 deviceId 时校验一致 (heartbeat/locations/alerts)
            String pathDeviceId = extractPathDeviceId(path);
            if (pathDeviceId != null && !deviceId.equals(pathDeviceId)) {
                throw BizException.forbidden("密钥与路径中的设备编号不匹配");
            }
            ContextHolder.setDeviceId(deviceId);
            return true;
        }

        // 其余: 家属 JWT
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw BizException.unauthorized("未登录或缺少 Authorization 头");
        }
        Claims claims = jwtUtils.parse(header.substring(7));
        ContextHolder.setUserId(Long.valueOf(claims.getSubject()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ContextHolder.clearAll();
    }

    private static final Pattern P_DEVICE_HEARTBEAT = Pattern.compile("/api/devices/[^/]+/heartbeat");
    private static final Pattern P_DEVICE_LOCATIONS = Pattern.compile("/api/devices/[^/]+/locations");
    private static final Pattern P_DEVICE_ALERTS = Pattern.compile("/api/devices/[^/]+/alerts");
    private static final Pattern P_ALERT_IMAGES = Pattern.compile("/api/alerts/[^/]+/images");

    private boolean isDeviceWritePath(String path) {
        return P_DEVICE_HEARTBEAT.matcher(path).matches()
                || P_DEVICE_LOCATIONS.matcher(path).matches()
                || P_DEVICE_ALERTS.matcher(path).matches()
                || P_ALERT_IMAGES.matcher(path).matches();
    }

    /** /api/devices/{deviceId}/... → 取第 4 段; /api/alerts/... 无 deviceId 返回 null */
    private String extractPathDeviceId(String path) {
        String[] seg = path.split("/");
        if (seg.length >= 4 && "api".equals(seg[1]) && "devices".equals(seg[2])) {
            return seg[3];
        }
        return null;
    }
}
