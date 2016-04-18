package org.openthinclient.web.ui.event;

import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;

public class PackageManagerTaskFinalizedEvent {
    private final PackageManagerExecutionEngine engine;

    public PackageManagerTaskFinalizedEvent(PackageManagerExecutionEngine engine) {
        this.engine = engine;

    }

    public PackageManagerExecutionEngine getEngine() {
        return engine;
    }
}
