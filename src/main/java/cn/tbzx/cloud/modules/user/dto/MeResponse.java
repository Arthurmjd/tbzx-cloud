package cn.tbzx.cloud.modules.user.dto;

/** GET /api/auth/me 返回: 当前登录家属的公开资料 */
public record MeResponse(Long userId, String username, String realName) {
}
