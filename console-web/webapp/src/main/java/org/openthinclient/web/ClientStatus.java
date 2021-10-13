package org.openthinclient.web;

import org.openthinclient.api.ws.WebSocketHandler;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;

@Component
@EnableScheduling
public class ClientStatus {

    /** Check for offline clients every _ milliseconds */
    private static final long STATUS_UPDATE_INTERVAL = 3 * 1000;

    /** Miliseconds since last heartbeat before client counts as offline */
    private static final long HEARTBEAT_TOLERANCE = 10 * 1000;

    Pattern MAC_LINE = Pattern.compile(
        "^MAC:((?:[0-9a-f]{2}:){5}[0-9a-f]{2})$",
        Pattern.MULTILINE
    );

    @Autowired
    WebSocketHandler webSocket;

    @Autowired
    ClientService clientService;

    Map<String, Long> clients;

    @PostConstruct
    public void init() {
        clients = new HashMap<>();
        webSocket.register("heartbeat", this::onHeartbeat);
    }

    private void onHeartbeat(WebSocketSession session, String message) {
        String remote_ip = session.getRemoteAddress().getAddress().getHostAddress();
        Matcher matcher = MAC_LINE.matcher(message);
        synchronized(clients) {
            while(matcher.find()) {
                String mac = matcher.group(1);
                if(!clients.containsKey(mac)) {
                    for(Client client: clientService.findByHwAddress(mac)) {
                        client.setIpHostNumber(remote_ip);
                        // Run in background, as saving can take a lot of time.
                        new Thread(() -> {
                            clientService.save(client);
                        }).start();
                    }
                }
                clients.put(mac, System.currentTimeMillis());
            }
        }
    }

    @Scheduled(fixedRate = STATUS_UPDATE_INTERVAL)
    public void checkClientsStatus() {
        long cutOff = System.currentTimeMillis() - HEARTBEAT_TOLERANCE;
        synchronized(clients) {
            clients.values().removeIf(timestamp -> timestamp < cutOff);
        }
    }

    public boolean isOnline(String mac) {
        return clients.containsKey(mac);
    }

    public Set<String> getOnlineMACs() {
        return Collections.unmodifiableSet(clients.keySet());
    }

}
