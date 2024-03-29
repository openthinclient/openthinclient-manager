package org.openthinclient.manager.standalone.config;

import org.openthinclient.api.ws.WebSocketHandler;
import org.openthinclient.web.pkgmngr.event.PackageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ClientNotifier {

    @Autowired
    WebSocketHandler webSocket;

    @EventListener
    public void onPackageEvent(PackageEvent ev) {
        if(ev.changesOccured()) {
            // Call in a new thread to avoid deadlock when event is sent
            // directly from web interface.
            new Thread(() ->
                webSocket.sendToAll("package-update")
            ).start();
        }
    }
}
