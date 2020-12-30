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
	private static final String pubKeyPath = "D:\\project-0713\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\project-0713\\rsa\\rsa.pri";

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
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDkzMTY5Mjl9.I7fHB6WiOYc-CE_FOEAWCtj5R2k4oEZdu-oudUOGENgLDprIUWH69kvOMXwJVQOvLwojqAB52PdOBPAvbD7OFy0xO8ARJ7wA9vwPYnCx5en2legkZd3RBwAbheedsz91oUIPGLdSUJ16FCIKjRVfGMcO5lQlqL5eUnjvLosgqsFPgt7nkiyAAkzBhbAO1z_q2u1Av__7kFKNZZL1hkFOh0LIkF8o8JydByxeabCVIR7TGRMF2zTF3iccwuvJjKWaLGMEu6pERm3xcu-1_beTWrXPDPAkIe8j_1cBkZFcwlwIPNKqhsuFj10x1geq2sO-mq6OIITLCyWx3m_epjIaJw";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
