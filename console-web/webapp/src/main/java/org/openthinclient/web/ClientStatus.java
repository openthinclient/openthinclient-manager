package org.openthinclient.web;

import org.openthinclient.api.ws.WebSocketHandler;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.vaadin.spring.events.EventBus;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;

@Component
@EnableScheduling
public class ClientStatus {
    private static final Logger LOG = LoggerFactory.getLogger(ClientStatus.class);

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
    private ApplicationContext applicationContext;

    @Autowired
    EventBus.ApplicationEventBus applicationEventBus;

    @Autowired
    ClientService clientService;

    Map<String, Long> clients;
    Set<String> gone_online;

    @PostConstruct
    public void init() {
        clients = new HashMap<>();
        gone_online = new HashSet<>();
        webSocket.register("heartbeat", this::onHeartbeat);
    }

    private void onHeartbeat(WebSocketSession session, String message) {
        String remote_ip = session.getRemoteAddress().getAddress().getHostAddress();
        Matcher matcher = MAC_LINE.matcher(message);
        synchronized(clients) {
            while(matcher.find()) {
                String mac = matcher.group(1);
                if(!clients.containsKey(mac)) {
                    synchronized (gone_online) {
                        gone_online.add(mac);
                    }
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
        Set<String> offline = new HashSet<>();
        synchronized(clients) {
            for(Map.Entry<String, Long> entry: clients.entrySet()) {
                if(entry.getValue() < cutOff) {
                    offline.add(entry.getKey());
                }
            }
            clients.keySet().removeAll(offline);
            //clients.values().removeIf(timestamp -> timestamp < cutOff);
        }
        Set<String> online;
        synchronized(gone_online) {
            online = new HashSet<>(gone_online);
            gone_online.clear();
        }
        online.removeAll(offline);
        if(online.size()>0 || offline.size()>0) {
            LOG.info("publish ClientStatusUpdateEvent("+online.toString()+", "+offline.toString()+")");
            //applicationContext.publishEvent(new ClientStatusUpdateEvent(this, online, offline));
            applicationEventBus.publish(this, new ClientStatusUpdateEvent(this, online, offline));
        }
    }

    public boolean isOnline(String mac) {
        return clients.containsKey(mac);
    }

    public Set<String> getOnlineMACs() {
        return Collections.unmodifiableSet(clients.keySet());
    }

}
