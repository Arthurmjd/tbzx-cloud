package cn.tbzx.cloud.config;

import cn.tbzx.cloud.common.ApiKeyUtils;
import cn.tbzx.cloud.modules.device.entity.Device;
import cn.tbzx.cloud.modules.device.mapper.DeviceMapper;
import cn.tbzx.cloud.modules.user.entity.User;
import cn.tbzx.cloud.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 开发演示数据: user/device 表为空时插入
 * - 家属账号 test / 123456
 * - 演示设备 TBZX-DEMO00001, 上报密钥 demo-key-123456 (云端只存 SHA-256)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tbzx.init-demo-data", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    public static final String DEMO_USERNAME = "test";
    public static final String DEMO_PASSWORD = "123456";
    public static final String DEMO_DEVICE_ID = "TBZX-DEMO00001";
    public static final String DEMO_API_KEY = "demo-key-123456";

    private final UserMapper userMapper;
    private final DeviceMapper deviceMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.selectCount(null) == 0) {
            User user = new User();
            user.setUsername(DEMO_USERNAME);
            user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
            user.setRealName("测试用户");
            user.setStatus(1);
            userMapper.insert(user);
            log.info("[演示数据] 已创建家属账号 {} / {}", DEMO_USERNAME, DEMO_PASSWORD);
        }
        if (deviceMapper.selectCount(null) == 0) {
            // 关联到第一个家属账号 (避免硬编码 user_id=1 在账号非空时配错)
            User first = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                    .orderByAsc(User::getId).last("LIMIT 1"));
            Device device = new Device();
            device.setDeviceId(DEMO_DEVICE_ID);
            device.setDeviceName("我的助行设备");
            device.setApiKey(ApiKeyUtils.sha256Hex(DEMO_API_KEY));
            if (first != null) {
                device.setUserId(first.getId());
            }
            deviceMapper.insert(device);
            log.info("[演示数据] 已创建设备 {}, 上报密钥: {}, 绑定家属 user_id={}",
                    DEMO_DEVICE_ID, DEMO_API_KEY, first == null ? "无" : first.getId());
        }
    }
}
