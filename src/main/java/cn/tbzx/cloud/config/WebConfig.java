package cn.tbzx.cloud.config;

import cn.tbzx.cloud.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 统一鉴权路由 (AuthInterceptor 按方法+路径分流, 见该类注释)
 * + 图片静态资源服务 (开发期由 Spring 直接服务, 生产由 nginx)
 * + CORS (开发期前端 vite 与后端分端口, 需放行跨域)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /** CORS 允许的来源 (逗号分隔), 可用 tbzx.cors.allowed-origins 覆盖 */
    @Value("${tbzx.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:5175}")
    private String[] allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
        // /images/** 不拦截 — 静态文件, ResourceHandler 直出
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 前端携带 Bearer token (非 Cookie), 故不开启 allowCredentials
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 图片目录: /images/** → 磁盘 ./data/images/
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:./data/images/");
    }
}
