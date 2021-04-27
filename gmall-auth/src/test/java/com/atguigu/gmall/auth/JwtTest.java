package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "F:\\ShangGuigu\\IdeaWorkSpace\\gulimall\\rsa\\rsa.pub";
    private static final String priKeyPath = "F:\\ShangGuigu\\IdeaWorkSpace\\gulimall\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 3);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MTk0ODUxMzR9.KYdhrX8L8RPTtpG8iJyZ_gSkIzp0uYuy4psZv-_-0rsVdwXjHefU5qFDKPAVYSG41_sLNxsitX7BtdWqHIvf99iPHt1GNXnwRTtFu8uZIoz-dEPTOqRxR8sn-jzF9JryQ2blNwcfsgsHoWbzhgEWZ4JY3Iz9OJ_d14sXznnD3_USysrSJQTwUFaqnUk7-ui-fNDhmW3IXrYIUf95iMlL1SpTnq6ICJOgvh7G7xRkP60GZNBFOKUHceT4_qatJ4k18zFQuZjmaqqrfokgOOQivMYDQTY4907lvvD_JEKGLKqL1WbuDOt06kdmm1OLcneY3VUYc0gBAWPEYkreg-SJ8A";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}