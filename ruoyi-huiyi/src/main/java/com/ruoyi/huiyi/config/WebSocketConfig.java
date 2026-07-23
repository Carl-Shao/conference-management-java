package com.ruoyi.huiyi.config;

import com.ruoyi.huiyi.websocket.MeetingAudioWebSocketHandler;
import com.ruoyi.huiyi.websocket.MeetingHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.context.annotation.Bean;

/**
 * 注册会议录制音频上行的 WebSocket 端点。
 *
 * 音频压缩说明：permessage-deflate 是 WebSocket 协议标准扩展(RFC 7692)，
 * 只要浏览器请求头带 Sec-WebSocket-Extensions: permessage-deflate，
 * Tomcat 8.5+/9.x 的 WebSocket 引擎会自动协商启用，Spring 层不需要额外配置。
 * 如果你想强制要求压缩、或者观察到没有生效，可以检查 Tomcat 版本
 * 及是否有反向代理(Nginx等)剥离了该请求头。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MeetingAudioWebSocketHandler audioWebSocketHandler;

    @Autowired
    private MeetingHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioWebSocketHandler, "/ws/huiyi/record/{meetingId}")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * 音频二进制帧可能比默认64KB上限大（尤其网络抖动导致缓冲攒积时），
     * 适当调大 Tomcat WebSocket 的单帧大小限制。
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(512 * 1024);
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L); // 30分钟空闲超时
        return container;
    }
}