package cn.tbzx.cloud.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,

        @NotBlank(message = "验证码标识不能为空") String captchaId,
        @NotBlank(message = "请输入验证码") String captchaCode) {
}
