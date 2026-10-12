package cn.tbzx.cloud.modules.user.controller;

import cn.tbzx.cloud.modules.user.dto.LoginRequest;
import cn.tbzx.cloud.modules.user.dto.LoginResponse;
import cn.tbzx.cloud.modules.user.dto.RegisterRequest;
import cn.tbzx.cloud.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        Long userId = userService.register(req);
        return Map.of("userId", userId, "username", req.username());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return userService.login(req);
    }
}
