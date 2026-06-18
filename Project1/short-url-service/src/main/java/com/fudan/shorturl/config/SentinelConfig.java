package com.fudan.shorturl.config;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class SentinelConfig implements WebMvcConfigurer {

    /** 短链跳转：保护后端，单机 1000 QPS */
    public static final String RESOURCE_REDIRECT = "GET:/{shortCode}";
    /** 创建短链：写操作 + DB 落库，单机 20 QPS 防恶意刷 */
    public static final String RESOURCE_CREATE = "POST:/api/url/create";

    private static final String CONTEXT_NAME = "sentinel_web_context";
    private static final String ENTRY_ATTR = "sentinel.entry";

    private static final int REDIRECT_QPS_LIMIT = 1000;
    private static final int CREATE_QPS_LIMIT = 20;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SentinelHandlerInterceptor()).addPathPatterns("/**");
    }

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule redirectRule = new FlowRule();
        redirectRule.setResource(RESOURCE_REDIRECT);
        redirectRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        redirectRule.setCount(REDIRECT_QPS_LIMIT);
        redirectRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(redirectRule);

        FlowRule createRule = new FlowRule();
        createRule.setResource(RESOURCE_CREATE);
        createRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createRule.setCount(CREATE_QPS_LIMIT);
        createRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(createRule);

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel 限流规则已加载: {}={}qps, {}={}qps",
                RESOURCE_REDIRECT, REDIRECT_QPS_LIMIT, RESOURCE_CREATE, CREATE_QPS_LIMIT);
    }

    /** 自定义 HandlerInterceptor：preHandle 调 SphU.entry，afterCompletion 释放 entry */
    static class SentinelHandlerInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String resource = resolveResource(request);
            if (resource == null) {
                return true;
            }
            ContextUtil.enter(CONTEXT_NAME);
            try {
                Entry entry = SphU.entry(resource, EntryType.IN);
                request.setAttribute(ENTRY_ATTR, entry);
                return true;
            } catch (BlockException e) {
                ContextUtil.exit();
                handleBlocked(response, resource);
                return false;
            }
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            Entry entry = (Entry) request.getAttribute(ENTRY_ATTR);
            if (entry != null) {
                entry.exit();
                request.removeAttribute(ENTRY_ATTR);
            }
            ContextUtil.exit();
        }

        /** 把请求映射成 Sentinel 资源名；只关心需要限流的两个接口 */
        private String resolveResource(HttpServletRequest request) {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            if ("POST".equals(method) && "/api/url/create".equals(uri)) {
                return RESOURCE_CREATE;
            }
            if ("GET".equals(method) && uri.matches("/[0-9A-Za-z]{1,16}")) {
                return RESOURCE_REDIRECT;
            }
            return null;
        }

        private void handleBlocked(HttpServletResponse response, String resource) throws Exception {
            log.warn("Sentinel 限流触发: resource={}", resource);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}"
            );
        }
    }
}
