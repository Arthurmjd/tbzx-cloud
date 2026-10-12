package cn.tbzx.cloud.modules.user.controller;

import cn.tbzx.cloud.modules.user.dto.CaptchaResponse;
import cn.tbzx.cloud.modules.user.dto.LoginRequest;
import cn.tbzx.cloud.modules.user.dto.LoginResponse;
import cn.tbzx.cloud.modules.user.dto.MeResponse;
import cn.tbzx.cloud.modules.user.dto.RegisterRequest;
import cn.tbzx.cloud.modules.user.service.CaptchaService;
import cn.tbzx.cloud.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CaptchaService captchaService;

    /** 图形验证码: 服务端签发, 前端只拿到 id + 图片 (答案不下发) */
    @GetMapping("/captcha")
    public CaptchaResponse captcha() {
        return captchaService.issue();
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        Long userId = userService.register(req);
        return Map.of("userId", userId, "username", req.username());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return userService.login(req);
    }

    /** 校验 JWT 并返回当前家属资料 (前端启动时校验本地 token 是否仍有效) */
    @GetMapping("/me")
    public MeResponse me() {
        return userService.currentUser();
    }
}
