package org.openthinclient.api.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy.TERMINATE;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketHandler.class);

    private static Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private static Map<String, List<BiConsumer<WebSocketSession, String>>> consumers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessions.remove(session);
    }

    public void register(String message_type, BiConsumer<WebSocketSession, String> consumer) {
        if(!consumers.containsKey(message_type)) {
            consumers.put(message_type, new CopyOnWriteArrayList<>());
        }
        consumers.get(message_type).add(consumer);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws InterruptedException, IOException {
        String payload = message.getPayload();
        String[] parts = payload.split("\\R", 2);
        String message_type = parts[0];
        if(consumers.containsKey(message_type)) {
            for(BiConsumer<WebSocketSession, String> consumer: consumers.get(message_type)) {
                try {
                    consumer.accept(session, payload);
                } catch(Exception ex) {
                    LOG.error("Error in websocket handler", ex);
                }
            }
        } else {
            String remote_ip = session.getRemoteAddress().getAddress().getHostAddress();
            LOG.error("Received message with unknown type '{}' from IP {}", message_type, remote_ip);
        }
    }


	public void sendToAll(String message_type) {
        for(WebSocketSession session: sessions) {
            try (WebSocketSession wrappedSession = new ConcurrentWebSocketSessionDecorator(
                    session, 1000 /*ms*/, 1024/*B*/, TERMINATE)) {
                wrappedSession.sendMessage(new TextMessage(message_type));
            } catch(IOException ex) {
                LOG.error("Failed to send {} to {}", message_type, session.getRemoteAddress());
            } catch(SessionLimitExceededException ex) {
                LOG.info("Could not send {} to {}. Seems to be offline.", message_type, session.getRemoteAddress());
                sessions.remove(session);
            }
        }
	}
}
