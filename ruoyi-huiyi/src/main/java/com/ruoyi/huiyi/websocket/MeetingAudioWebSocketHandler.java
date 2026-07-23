package com.ruoyi.huiyi.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Component
public class MeetingAudioWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MeetingAudioWebSocketHandler.class);

    public static final String ATTR_MEETING_ID = "meetingId";

    @Autowired
    private MeetingSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        Long meetingId = resolveMeetingId(wsSession);
        RecordingSession recordingSession = sessionManager.getSession(meetingId);
        if(recordingSession == null) {
            log.warn("会议[{}]的WebSocket已建立，但录制会话不存在，关闭连接", meetingId);
            wsSession.close(CloseStatus.POLICY_VIOLATION.withReason("recording session not started"));
            return;
        }
        recordingSession.setWsSession(wsSession);
        log.info("会议[{}]音频WebSocket连接已建立: {}", meetingId, wsSession.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession wsSession, BinaryMessage message) {
        Long meetingId = resolveMeetingId(wsSession);
        RecordingSession recordingSession = sessionManager.getSession(meetingId);
        if (recordingSession == null) {
            return;
        }
        byte[] payload = new byte[message.getPayload().remaining()];
        message.getPayload().get(payload);
        recordingSession.appendAudio(payload);
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            wsSession.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
        Long meetingId = resolveMeetingId(wsSession);
        RecordingSession recordingSession = sessionManager.getSession(meetingId);
        if (recordingSession == null && recordingSession.getWsSession() == wsSession) {
            recordingSession.setWsSession(null);
        }
        log.info("会议[{}]音频WebSocket连接已关闭: {}, 原因: {}", meetingId, wsSession.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession wsSession, Throwable exception) {
        log.error("会议[{}]音频WebSocket传输异常", resolveMeetingId(wsSession), exception);
    }

    private Long resolveMeetingId(WebSocketSession wsSession) {
        return (Long) wsSession.getAttributes().get(ATTR_MEETING_ID);
    }
}
