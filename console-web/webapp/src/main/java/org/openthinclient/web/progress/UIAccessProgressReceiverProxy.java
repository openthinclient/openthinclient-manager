package org.openthinclient.web.progress;

import com.vaadin.ui.UI;

import org.openthinclient.progress.ProgressReceiver;

import java.util.function.Supplier;

/**
 * A {@link ProgressReceiver} implementation that proxies all {@link ProgressReceiver} calls to a
 * target {@link ProgressReceiver} using {@link UI#access(Runnable)}. This will ensure the correct
 * access state for vaadin UI interactions
 */
public class UIAccessProgressReceiverProxy implements ProgressReceiver {
    private final Supplier<UI> uiSupplier;
    private final ProgressReceiver target;

    public UIAccessProgressReceiverProxy(Supplier<UI> uiSupplier, ProgressReceiver target) {
        this.uiSupplier = uiSupplier;
        this.target = target;
    }

    @Override
    public void progress(String message, double progress) {
        UI ui = uiSupplier.get();
        if (ui != null)
            ui.access(() -> target.progress(message, progress));
    }

    @Override
    public void progress(String message) {
        UI ui = uiSupplier.get();
        if (ui != null)
            ui.access(() -> target.progress(message));
    }

    @Override
    public void progress(double progress) {
        UI ui = uiSupplier.get();
        if (ui != null)
            ui.access(() -> target.progress(progress));
    }

    @Override
    public ProgressReceiver subprogress(double progressMin, double progressMax) {
        // creating a ui access org.openthinclient.progress receiver that will decorate another subprogress receiver.
        // This implementation assumes that creating a subprogress receiver does not require a UI to be available.
        return new UIAccessProgressReceiverProxy(uiSupplier, target.subprogress(progressMin, progressMax));
    }

    @Override
    public void completed() {
        UI ui = uiSupplier.get();
        if (ui != null)
            ui.access(target::completed);
    }
}
