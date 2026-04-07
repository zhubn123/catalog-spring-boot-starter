package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * sample Web 配置。
 *
 * <p>同时保留跨域支持和最小请求日志，方便本地调试目录树、绑定和示例接口。</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor())
            .addPathPatterns("/catalog/**", "/contract/**", "/project/**");
    }

    /**
     * sample 请求日志拦截器。
     *
     * <p>当前只记录请求耗时、query string、parameter map 和响应状态码，先满足本地排查需求。</p>
     */
    @Slf4j
    private static class RequestLoggingInterceptor implements HandlerInterceptor {

        private static final String START_TIME_ATTR = "sample.request.startTime";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object handler,
                                    Exception ex) {
            long startedAt = request.getAttribute(START_TIME_ATTR) instanceof Long
                ? (Long) request.getAttribute(START_TIME_ATTR)
                : System.currentTimeMillis();
            long durationMs = System.currentTimeMillis() - startedAt;

            log.info(
                "sample request method={} uri={} status={} durationMs={} query={} params={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                safeText(request.getQueryString()),
                formatParameters(request.getParameterMap())
            );

            if (ex != null) {
                log.error("sample request failed uri={}", request.getRequestURI(), ex);
            }
        }

        private String formatParameters(Map<String, String[]> parameterMap) {
            if (parameterMap == null || parameterMap.isEmpty()) {
                return "-";
            }
            return parameterMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                .collect(Collectors.joining(", "));
        }

        private String safeText(String value) {
            if (value == null || value.isBlank()) {
                return "-";
            }
            return value;
        }
    }
}