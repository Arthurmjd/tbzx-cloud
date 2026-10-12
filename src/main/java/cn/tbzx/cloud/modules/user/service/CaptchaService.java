package cn.tbzx.cloud.modules.user.service;

import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.common.CaptchaUtils;
import cn.tbzx.cloud.modules.user.dto.CaptchaResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 图形验证码签发与校验。
 * 答案只存在服务端缓存 (captchaId → code), 前端拿不到明文;
 * 一次性使用 —— 校验后无论对错立即作废, 防重放。
 */
@Service
public class CaptchaService {

    private static final int LEN = 4;

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(3))   // 3 分钟过期
            .build();

    public CaptchaResponse issue() {
        String code = CaptchaUtils.randomCode(LEN);
        String id = UUID.randomUUID().toString();
        cache.put(id, code);
        return new CaptchaResponse(id, CaptchaUtils.toPngDataUri(code));
    }

    /** 校验并消费; 失败抛 400 */
    public void verify(String captchaId, String code) {
        if (captchaId == null || captchaId.isBlank() || code == null || code.isBlank()) {
            throw BizException.badRequest("请输入验证码");
        }
        String expected = cache.getIfPresent(captchaId);
        cache.invalidate(captchaId);                  // 一次性: 无论对错都作废
        if (expected == null) {
            throw BizException.badRequest("验证码已过期，请刷新");
        }
        if (!expected.equalsIgnoreCase(code.trim())) {
            throw BizException.badRequest("验证码错误");
        }
    }
}
