package com.ruoyi.huiyi.websocket;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 从 /ws/huiyi/record/{meetingId} 中解析出 meetingId，
 * 并校验该会议是否已经通过 REST /start 接口开启了录制会话——
 * 没有对应会话则直接拒绝握手，避免脏连接占用资源。
 */
@Component
public class MeetingHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MeetingHandshakeInterceptor.class);

    @Autowired
    private MeetingSessionManager sessionManager;

    @Override
    @SuppressWarnings("unchecked")
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, String> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        Map<String, String> uriTemplateVars = (Map<String, String>)
                servletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (uriTemplateVars == null || uriTemplateVars.get("meetingId") == null){
            log.warn("WebSocket握手缺少meetingId路径参数");
            return false;
        }

        Long meetingId;
        try {
            meetingId = Long.valueOf(uriTemplateVars.get("meetingId"));
        } catch (NumberFormatException e) {
            log.warn("WebSocket握手meetingId格式非法: {}", uriTemplateVars.get("meetingId"));
            return false;
        }

        if (!sessionManager.hasSession(meetingId)) {
            log.warn("会议[{}]尚未开始录制(未调用/start)，拒绝WebSocket握手", meetingId);
            return false;
        }

        attributes.put(MeetingAudioWebSocketHandler.ATTR_MEETING_ID, "meetingId");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
