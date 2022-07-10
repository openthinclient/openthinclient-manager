package org.openthinclient.web;

import java.util.Set;

import org.springframework.context.ApplicationEvent;

public class ClientStatusUpdateEvent extends ApplicationEvent {
    private Set<String> online;
    private Set<String> offline;

    ClientStatusUpdateEvent(Object source, Set<String> online, Set<String> offline) {
        super(source);
        this.online = online;
        this.offline = offline;
    }
    public Set<String> getOnline() {
        return online;
    }
    public Set<String> getOffline() {
        return offline;
    }
}
