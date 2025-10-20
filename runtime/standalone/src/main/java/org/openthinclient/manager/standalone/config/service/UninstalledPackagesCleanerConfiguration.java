package org.openthinclient.manager.standalone.config.service;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.openthinclient.api.ws.WebSocketHandler;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.UninstalledPackagesCleaner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class UninstalledPackagesCleanerConfiguration {

    private final static long CLEANUP_INTERVAL = 15 * 60 * 1000;  // 15 min
    private final static long COLLECTION_TIMEOUT = 5 * 1000;      // 5 sec

    @Autowired
    WebSocketHandler webSocket;

    @Autowired
    UninstalledPackagesCleaner cleaner;

    @Autowired
    PackageManager packageManager;

    @Bean
    public UninstalledPackagesCleaner uninstalledPackagesCleaner() {
        return new UninstalledPackagesCleaner();
    }


    @PostConstruct
    public void init() {
        // Receive mounted package paths (when requested in scheduled task)
        webSocket.register(
            "mounted-packages-list",
            (sess, msg) ->
                cleaner.addMountedPaths(Arrays.asList(msg.split("\\R")))
        );
    }

    @Scheduled(fixedRate = CLEANUP_INTERVAL)
    public void requestMountedPackagesLists() {
        if (packageManager.isRunning()) return;

        // Request mounted packages lists from all connected clients
        cleaner.startCollection();
        webSocket.sendToAll("mounted-packages-list");

        // Wait COLLECTION_TIMEOUT for clients to report before running cleanup
        (new Thread(() -> {
            try {
                Thread.sleep(COLLECTION_TIMEOUT);
            } catch (InterruptedException e) {
                return;
            }
            if (packageManager.isRunning()) return;
            cleaner.runCleanup();
        })).start();
    }
}
