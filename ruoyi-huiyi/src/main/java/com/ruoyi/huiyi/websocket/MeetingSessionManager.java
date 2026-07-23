package com.ruoyi.huiyi.websocket;

import com.ruoyi.huiyi.config.MeetingRecordProperties;
import com.ruoyi.huiyi.service.impl.MeetingAsrServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MeetingSessionManager {

    private static final Logger log = LoggerFactory.getLogger(MeetingSessionManager.class);

    private final Map<Long, RecordingSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MeetingRecordProperties properties;

    @Autowired
    private MeetingAsrServiceImpl asrService;

    public RecordingSession getSession(Long meetingId) {
        return sessions.get(meetingId);
    }

    public boolean hasSession(Long meetingId) {
        return sessions.containsKey(meetingId);
    }
}
