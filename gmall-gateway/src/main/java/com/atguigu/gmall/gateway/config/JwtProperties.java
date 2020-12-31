package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * 将来使用时要通过@EnableConfigurationProperties(JwtProperties.class)
 */
@ConfigurationProperties(prefix = "auth.jwt")
@Data
public class JwtProperties {
    private String pubKeyPath;
    private String cookieName;

    private PublicKey publicKey;

    @PostConstruct
    public void init(){
        try {
            // 把公钥文件和私钥文件对应的内容读取出来放入公钥和私钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
