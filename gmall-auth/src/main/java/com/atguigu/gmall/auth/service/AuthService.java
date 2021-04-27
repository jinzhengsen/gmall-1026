package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exceptions.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {
    @Autowired
    private GmallUmsClient umsClient;

    @Resource
    private JwtProperties jwtProperties;

    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
        // 远程调用ums接口，校验登录名和密码是否正确
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUser(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();
        // 判断用户信息是否为空，为空抛出一个异常
        if (userEntity==null){
            throw  new UserException("用户名或者密码错误!");
        }

        // 组装载荷信息
        Map<String, Object> map=new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("userName",userEntity.getUsername());

        // 为了防止token被盗用，添加当前登录用户的ip地址
        map.put("ip", IpUtils.getIpAddressAtService(request));

        try {
            // 制作jwt类型的token
            String token = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());

            // 放入cookie中
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpire()*60);

            // 把昵称放入cookie中
            CookieUtils.setCookie(request,response,jwtProperties.getUnick(),userEntity.getNickname(),jwtProperties.getExpire()*60);
        } catch (Exception e) {
            throw new UserException("服务端错误!");
        }

    }
}
