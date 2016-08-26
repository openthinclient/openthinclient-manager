package org.openthinclient.web.progress;

import java.util.concurrent.TimeUnit;

import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.pkgmgr.progress.ProgressReceiver;
import org.openthinclient.web.pkgmngr.ui.view.GenericListContainer;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class ProgressReceiverDialog {
  
    private final ProgressBar progressBar;
    private final Label messageLabel;
    private final Window window;
    private final HorizontalLayout footer;
    private final Button closeButton;

    public ProgressReceiverDialog(String caption) {
      
        window = new Window(caption);

        window.setResizable(false);
        window.setClosable(false);
        window.setWidth(60, Sizeable.Unit.PERCENTAGE);
        window.setHeight(null);
        window.center();


        final VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        content.setSpacing(true);
        content.setWidth("100%");

        this.messageLabel = new Label("Running...");

        this.progressBar = new ProgressBar();
        this.progressBar.setIndeterminate(true);
        this.progressBar.setWidth(100, Sizeable.Unit.PERCENTAGE);

        this.footer = new MHorizontalLayout().withFullWidth().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        closeButton = new MButton("Close").withStyleName(ValoTheme.BUTTON_PRIMARY).withListener(e -> close());
        this.footer.addComponent(closeButton);
        footer.setComponentAlignment(closeButton, Alignment.MIDDLE_RIGHT);


        content.addComponent(this.messageLabel);
        content.addComponent(this.progressBar);

        window.setContent(content);
    }

    public void open(boolean modal) {
        window.setModal(modal);
        final UI ui = UI.getCurrent();
        if (!ui.getWindows().contains(window)) {
            ui.setPollInterval((int) TimeUnit.SECONDS.toMillis(1));
            ui.addWindow(window);
        }
    }

    public void close() {
        // disable polling
        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().removeWindow(window);
    }

    public void watch(ListenableProgressFuture<?> future) {

      
        future.addProgressReceiver(createProgressReceiver());
        future.addCallback(res -> {
                    // execution has been successful
                    PackageManagerOperationReport report = null;
                    if (res instanceof PackageManagerOperationReport) {
                      report = (PackageManagerOperationReport) res;
                    }
                    onSuccess(report);
                },
                this::onError);
    }

    public ProgressReceiver createProgressReceiver() {

        return new UIAccessProgressReceiverProxy(window::getUI, new ProgressReceiver() {
            @Override
            public void progress(String message, double progress) {
                onProgress(message, progress);
            }

            @Override
            public void progress(String message) {
                onProgress(message);
            }

            @Override
            public void progress(double progress) {
                onProgress(progress);
            }

            @Override
            public void completed() {
                onCompleted();
            }
        });
    }

    public void onSuccess(PackageManagerOperationReport report) {
        final Label checkLabel = new Label(FontAwesome.CHECK_CIRCLE.getHtml() + " Success", ContentMode.HTML);
        checkLabel.setStyleName("state-label-success-xl");
        
        GenericListContainer<PackageReport> reportsListContainer = new GenericListContainer<>(PackageReport.class);
        TreeTable operationReport = new TreeTable();
        if (report != null) {
          reportsListContainer.addAll(report.getPackageReports());
          // TODO: magic numbers
          operationReport.setWidth("100%");
          operationReport.setHeight((report.getPackageReports().size() * 38) + "px");
          operationReport.setContainerDataSource(reportsListContainer);
          operationReport.setVisibleColumns("packageName", "type");
          operationReport.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        }
        
        window.setContent(new MVerticalLayout(checkLabel, operationReport, footer).withFullWidth().withMargin(true).withSpacing(true));
    }

    public void onError(Throwable throwable) {
        final Label errorLabel = new Label(FontAwesome.TIMES_CIRCLE.getHtml() + " Failed", ContentMode.HTML);
        errorLabel.setStyleName("state-label-error-xl");
        Label errorMessage = new Label("An unexpected exception occurred: " + throwable.getMessage() + ", please take a look into server-logfile.");
        window.setContent(new MVerticalLayout(errorLabel, errorMessage, footer).withFullWidth().withMargin(true).withSpacing(true));
    }

    protected void onCompleted() { }

    protected void onProgress(double progress) {

        if (progress < 0) {
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setValue((float) progress);
        }
    }

    protected void onProgress(String message) {
        messageLabel.setValue(message);
    }

    protected void onProgress(String message, double progress) {
        onProgress(message);
        onProgress(progress);
    }

}
