package cn.gugufish.filter;

import cn.gugufish.entity.dto.OperationLog;
import cn.gugufish.service.OperationLogService;
import cn.gugufish.utils.Const;
import cn.gugufish.utils.SnowflakeIdGenerator;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

/**
 * 请求日志过滤器，用于记录所有用户请求信息
 */
@Slf4j
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    private static final String ATTR_REQUEST_ID = RequestLogFilter.class.getName() + ".requestId";
    private static final int MAX_PREVIEW_LENGTH = 1000;

    @Resource
    SnowflakeIdGenerator generator;

    @Resource
    OperationLogService operationLogService;

    private final Set<String> ignores = Set.of("/swagger-ui", "/v3/api-docs");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (this.isIgnoreUrl(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        this.logRequestStart(request);
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);
        this.logRequestEnd(request, wrapper, startTime);
        this.saveOperationLog(request, wrapper, startTime);
        wrapper.copyBodyToResponse();
        MDC.remove("reqId");
    }

    private boolean isIgnoreUrl(String url) {
        for (String ignore : ignores) {
            if (url.startsWith(ignore)) return true;
        }
        return false;
    }

    public void logRequestEnd(HttpServletRequest request, ContentCachingResponseWrapper wrapper, long startTime) {
        long time = System.currentTimeMillis() - startTime;
        String content = responsePreview(request, wrapper);
        log.info("请求处理耗时: {}ms | 响应结果: {}", time, content);
    }

    public void logRequestStart(HttpServletRequest request) {
        long reqId = generator.nextId();
        request.setAttribute(ATTR_REQUEST_ID, reqId);
        MDC.put("reqId", String.valueOf(reqId));
        JSONObject object = requestParams(request);
        Object id = request.getAttribute(Const.ATTR_USER_ID);
        if (id != null && SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User user) {
            log.info("请求URL: \"{}\" ({}) | 远程IP地址: {} | 身份: {} (UID: {}) | 角色: {} | 请求参数列表: {}",
                    request.getServletPath(), request.getMethod(), request.getRemoteAddr(),
                    user.getUsername(), id, user.getAuthorities(), object);
        } else {
            log.info("请求URL: \"{}\" ({}) | 远程IP地址: {} | 身份: 未验证 | 请求参数列表: {}",
                    request.getServletPath(), request.getMethod(), request.getRemoteAddr(), object);
        }
    }

    private void saveOperationLog(HttpServletRequest request, ContentCachingResponseWrapper wrapper, long startTime) {
        try {
            Long requestId = request.getAttribute(ATTR_REQUEST_ID) instanceof Number number ? number.longValue() : null;
            Integer accountId = request.getAttribute(Const.ATTR_USER_ID) instanceof Number number ? number.intValue() : null;
            String username = null;
            String role = null;
            if (SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User user) {
                username = user.getUsername();
                role = user.getAuthorities() == null ? null : user.getAuthorities().toString();
            }

            OperationLog logItem = new OperationLog(
                    null,
                    requestId,
                    request.getServletPath(),
                    request.getMethod(),
                    request.getRemoteAddr(),
                    accountId,
                    username,
                    role,
                    truncate(requestParams(request).toJSONString()),
                    wrapper.getStatus(),
                    truncate(responsePreview(request, wrapper)),
                    (int) (System.currentTimeMillis() - startTime),
                    new Date()
            );
            operationLogService.saveLog(logItem);
        } catch (Exception exception) {
            log.warn("保存操作日志失败: {}", exception.getMessage());
        }
    }

    private JSONObject requestParams(HttpServletRequest request) {
        JSONObject object = new JSONObject();
        request.getParameterMap().forEach((key, values) -> object.put(key, sanitizeRequestValue(key, values.length > 0 ? values[0] : null)));
        return object;
    }

    private String sanitizeRequestValue(String key, String value) {
        if (key == null) return value;
        String normalizedKey = key.trim().toLowerCase();
        if (normalizedKey.contains("password")) return "******";
        if (normalizedKey.contains("token")) return "******";
        if (normalizedKey.contains("authorization")) return "******";
        return value;
    }

    private String responsePreview(HttpServletRequest request, ContentCachingResponseWrapper wrapper) {
        if (request != null && "/api/auth/login".equals(request.getServletPath())) {
            return "{\"message\":\"login response hidden\"}";
        }
        String content = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        if (content.isBlank()) {
            return wrapper.getStatus() == 200 ? "success" : wrapper.getStatus() + " error";
        }
        return content;
    }

    private String truncate(String value) {
        if (value == null) return null;
        String text = value.trim();
        if (text.length() <= MAX_PREVIEW_LENGTH) return text;
        return text.substring(0, MAX_PREVIEW_LENGTH) + "...";
    }
}
