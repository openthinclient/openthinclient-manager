package org.openthinclient.web.ui.event;

import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;

public class PackageManagerTaskActivatedEvent {
    private final PackageManagerExecutionEngine engine;

    public PackageManagerTaskActivatedEvent(PackageManagerExecutionEngine engine) {
        this.engine = engine;

    }

    public PackageManagerExecutionEngine getEngine() {
        return engine;
    }
}
