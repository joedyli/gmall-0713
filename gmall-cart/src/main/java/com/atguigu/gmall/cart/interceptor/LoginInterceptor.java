package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();

        // 获取UserKey 以及UserId
        String userKey = CookieUtils.getCookieValue(request, this.properties.getUserKey());
        if (StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, this.properties.getUserKey(), userKey, this.properties.getExpire());
        }
        userInfo.setUserKey(userKey);

        // 获取token信息
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        if (StringUtils.isBlank(token)){
            THREAD_LOCAL.set(userInfo);
            return true;
        }

        Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
        Long userId = Long.valueOf(map.get("userId").toString());
        userInfo.setUserId(userId);
        THREAD_LOCAL.set(userInfo);

        // 目的统一获取登录状态，不管有没有登录都要放行
        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 这里一定记得要手动清理threadlocal中的线程局部变量，因为使用的是tomcat线程池，请求结束线程没有结束，否则容易产生内存泄漏
        THREAD_LOCAL.remove();
    }
}
