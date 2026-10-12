package cn.tbzx.cloud.modules.user.dto;

/** GET /api/auth/captcha 返回: 验证码 id + 图片 (PNG data URI) */
public record CaptchaResponse(String captchaId, String image) {
}
