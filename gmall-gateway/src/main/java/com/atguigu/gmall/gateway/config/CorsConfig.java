package com.atguigu.gmall.gateway.config;

import com.netflix.config.sources.URLConfigurationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter(){
        // 初始化CORS配置对象
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的域,不要写*，否则cookie就无法使用了
        configuration.addAllowedOrigin("http://manager.gmall.com");
        configuration.addAllowedOrigin("http://gmall.com");
        configuration.addAllowedOrigin("http://www.gmall.com");
        configuration.addAllowedOrigin("http://localhost:1000");
        // 允许的头信息
        configuration.addAllowedHeader("*");
        // 允许的请求方式
        configuration.addAllowedMethod("*");
        // 是否允许携带Cookie信息
        configuration.setAllowCredentials(true);
        // 添加映射路径，我们拦截一切请求
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",configuration);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);

    }
}
