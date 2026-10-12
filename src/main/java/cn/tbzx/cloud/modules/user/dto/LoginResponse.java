package cn.tbzx.cloud.modules.user.dto;

public record LoginResponse(String token, Long userId, String username) {
}
