package cn.tbzx.cloud.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

@Configuration
public class AppConfig {

    /** 家属密码 BCrypt */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 设备 ApiKey 校验缓存: key = apiKey 的 SHA-256, value = deviceId
     * 免每次上报都查库; 10 分钟过期兜底, 换绑/吊销后最多延迟 10 分钟生效
     */
    @Bean
    public Cache<String, String> apiKeyCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }
}
