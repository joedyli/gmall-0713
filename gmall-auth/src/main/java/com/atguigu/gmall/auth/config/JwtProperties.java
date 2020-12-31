package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * 将来使用时要通过@EnableConfigurationProperties(JwtProperties.class)
 */
@ConfigurationProperties(prefix = "auth.jwt")
@Data
public class JwtProperties {
    private String pubKeyPath;
    private String priKeyPath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String unick;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void init(){
        try {
            File pubFile = new File(pubKeyPath);
            File priFile = new File(pubKeyPath);
            // 判断公钥文件或者私钥文件是否为空，只要有一个为空，全部重新生成。
            if (!pubFile.exists() || !priFile.exists()){
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }
            // 把公钥文件和私钥文件对应的内容读取出来放入公钥和私钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
