package com.atguigu.gmall.gateway.filters;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties properties;

    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

                // 获取了请求对象及响应对象ServerHttpRequest --> httpServletRequest
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 1.判断请求在不在拦截名单之中，不在直接放行
                List<String> pathes = config.pathes;// 拦截名单
                String curPath = request.getURI().getPath();// 获取了当前请求的路径
                if (!CollectionUtils.isEmpty(pathes)){
                    // 如果名单中所有路径都不满足，直接放行
                    if (!pathes.stream().anyMatch(path -> curPath.startsWith(path))){
                        return chain.filter(exchange);
                    }
                }

                // 2.获取请求中的token信息：同步-cookie 异步-头信息
                String token = request.getHeaders().getFirst("token"); // 异步请求况下头信息获取token信息
                if (StringUtils.isBlank(token)){
                    // 如果从头信息中没有获取到token，尝试从cookie中获取token信息
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies)){
                        HttpCookie cookie = cookies.getFirst(properties.getCookieName());
                        token = cookie.getValue();
                    }
                }

                // 3.判断token是否为空，为空重定向到登录页面并拦截
                if (StringUtils.isBlank(token)){
                    // 设置重定向相关信息
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    // 拦截请求
                    return response.setComplete();
                }

                try {
                    // 4.解析token信息，如果解析出现异常要重定向到登录页面并拦截
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());

                    // 5.判断token中ip和当前请求的ip是否一致，不一致重定向到登录页面并拦截
                    String ip = map.get("ip").toString();
                    String curIp = IpUtils.getIpAddressAtGateway(request);
                    if (!StringUtils.equals(ip, curIp)){
                        // 设置重定向相关信息
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        // 拦截请求
                        return response.setComplete();
                    }

                    // 6.把解析后的登录信息传递给后续服务
                    request.mutate().header("userId", map.get("userId").toString()).build();
                    exchange.mutate().request(request).build();

                    // 7.放行
                    return chain.filter(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 设置重定向相关信息
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    // 拦截请求
                    return response.setComplete();
                }
            }
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("pathes");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Data
    public static class PathConfig{
        private List<String> pathes;
    }

//    @Data
//    public static class KeyValueConfig{
//        private String key;
//        private String value;
//        private String desc;
//    }
}
