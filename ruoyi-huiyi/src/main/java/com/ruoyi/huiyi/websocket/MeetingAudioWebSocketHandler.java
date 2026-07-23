package com.ruoyi.huiyi.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
    }

    private Long resolveMeetingId(WebSocketSession wsSession) {
        return (Long) wsSession.getAttributes().get(ATTR_MEETING_ID);
    }
}
