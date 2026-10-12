package cn.tbzx.cloud.config;

import cn.tbzx.cloud.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 统一鉴权路由 (AuthInterceptor 按方法+路径分流, 见该类注释)
 * + 图片静态资源服务 (开发期由 Spring 直接服务, 生产由 nginx)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
        // /images/** 不拦截 — 静态文件, ResourceHandler 直出
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 图片目录: /images/** → 磁盘 ./data/images/
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:./data/images/");
    }
}
