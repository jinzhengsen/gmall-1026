package com.atguigu.gmall.auth.config;


import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {
    private String priKeyPath;
    private String pubKeyPath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String unick;

    private PublicKey publicKey;
    private PrivateKey privateKey;


    @PostConstruct
    public void init(){
        try {
            File priFile = new File(priKeyPath);
            File pubFile = new File(pubKeyPath);

            // 判断秘钥是否存在，只要任何一个不存在，重新生成一对
            if (!priFile.exists() || !pubFile.exists()){
                RsaUtils.generateKey(pubKeyPath,priKeyPath,secret);
            }
            this.publicKey=RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey=RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
