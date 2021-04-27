package com.atguigu.gmall.gateway.filters;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import com.google.common.net.HttpHeaders;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// 1.编写实现类继承AbstractGatewayFilterFactory抽象类
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {  // 4.指定泛型就是静态的内部实体类

    @Resource
    private JwtProperties jwtProperties;

    @Override
    public GatewayFilter apply(PathConfig config) {
//        System.out.println("我是一个局部过滤器1");
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//                System.out.println("我是一个局部过滤器22"+config.getKey()+"====="+config.getValue());
                System.out.println("我是一个局部过滤器2==>"+config.getPaths());

                // 获取请求中request 和 response
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 1.判断请求路径在不在拦截名单中，不在则直接放行
                List<String> paths = config.getPaths();  //获取拦截名单
                String currentUserPath = request.getURI().getPath();  //获取用户当前请求路径
                if (!paths.stream().anyMatch(path-> StringUtils.startsWith(currentUserPath,path))){
                    return chain.filter(exchange);
                }

                // 2.获取请求中的token：头信息  cookie
                String token = request.getHeaders().getFirst("token");
                if (StringUtils.isBlank(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    // 如果cookies不为空，并且包含token的情况下，获取cookie中的token信息
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){
                        HttpCookie httpCookie = cookies.getFirst(jwtProperties.getCookieName());
                         token = httpCookie.getValue();
                    }
                }

                // 3.判断token是否为空，为空则重定向到登录页面并拦截
                if (StringUtils.isBlank(token)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);  //设置重定向的状态码303
//                    重定向的地址
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                }

                try {
                    // 4.解析token，如果出现异常则重定向到登录页面并拦截
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());

                    // 5.获取载荷中ip（登录用户），和 当前用户的ip地址 比较。不一致则重定向到登录页面并拦截
                    String ip = map.get("ip").toString();  //登录用户的ip地址
                    String curIp = IpUtils.getIpAddressAtGateway(request);  //当前用户的ip地址
                    if (!StringUtils.equals(ip, curIp)){
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        return response.setComplete();
                    }

                    // 6.把解析后的用户信息传递给后续服务
                    String userId = map.get("userId").toString();
                    request.mutate().header("userId",userId).build();  //转换request对象把用户信息放入request对象中
                    exchange.mutate().request(request).build();

                    // 7.放行
                    return chain.filter(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 如果出现异常则重定向到登录页面并拦截
                    response.setStatusCode(HttpStatus.SEE_OTHER); // 设置重定向的状态码303
                    // 重定向的地址
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }
            }
        };
    }

    // 5.重写无参构造方法，指定内部实体类接受参数
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }


    // 6.重写shortcutFieldOrder方法指定接受参数的字段顺序
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    // 7.重写shortcutType方法指定接受参数的字段类型
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    // 3.定义匿名内部实体类，定义接受参数的字段
    @Data
    public static class PathConfig{
//        private String key;
//        private String value;
        private List<String> paths;
    }
}
