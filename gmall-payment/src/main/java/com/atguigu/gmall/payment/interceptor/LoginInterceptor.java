package com.atguigu.gmall.payment.interceptor;

import com.atguigu.gmall.payment.pojo.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {


    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();

        Long userId = Long.valueOf(request.getHeader("userId"));

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
