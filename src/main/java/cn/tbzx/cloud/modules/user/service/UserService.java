package cn.tbzx.cloud.modules.user.service;

import cn.tbzx.cloud.common.BizException;
import cn.tbzx.cloud.common.ContextHolder;
import cn.tbzx.cloud.common.JwtUtils;
import cn.tbzx.cloud.modules.user.dto.LoginRequest;
import cn.tbzx.cloud.modules.user.dto.LoginResponse;
import cn.tbzx.cloud.modules.user.dto.MeResponse;
import cn.tbzx.cloud.modules.user.dto.RegisterRequest;
import cn.tbzx.cloud.modules.user.entity.User;
import cn.tbzx.cloud.modules.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CaptchaService captchaService;

    public Long register(RegisterRequest req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.username()));
        if (exists > 0) {
            throw BizException.badRequest("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRealName(req.realName());
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    public LoginResponse login(LoginRequest req) {
        // 先校验图形验证码 (一次性), 再验账号密码
        captchaService.verify(req.captchaId(), req.captchaCode());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.username()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw BizException.forbidden("账号已被禁用");
        }
        String token = jwtUtils.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    /** 依当前 JWT 用户 (ContextHolder) 返回资料; 用户已删/禁用则视为登录失效 */
    public MeResponse currentUser() {
        Long userId = ContextHolder.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("登录状态无效，请重新登录");
        }
        return new MeResponse(user.getId(), user.getUsername(), user.getRealName());
    }
}
