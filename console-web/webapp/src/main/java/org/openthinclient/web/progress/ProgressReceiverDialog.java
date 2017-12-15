package org.openthinclient.web.progress;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_BUTTON_CLOSE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CAPTION_FAILED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CAPTION_SUCCESS;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_AT_SOURCE_ERROR;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_ERROR;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_ADDED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_REMOVED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_SKIPPED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_UPDATED;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import java.text.NumberFormat;
import org.openthinclient.pkgmgr.exception.PackageManagerDownloadException;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReport;
import org.openthinclient.progress.AbstractProgressReceiver;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

public class ProgressReceiverDialog {

    private final Logger LOGGER = LoggerFactory.getLogger(ProgressReceiverDialog.class);

    private final ProgressBar progressBar;
    private final Label messageLabel;
    private final Window window;
    private final HorizontalLayout footer;
    private final Button closeButton;

    private final IMessageConveyor mc;

    public ProgressReceiverDialog(String caption) {
      
        window = new Window(caption);

        window.setResizable(false);
        window.setClosable(false);
        window.setWidth(60, Sizeable.Unit.PERCENTAGE);
        window.setHeight(null);
        window.center();

        mc = new MessageConveyor(UI.getCurrent().getLocale());

        final VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        content.setWidth("100%");

        this.messageLabel = new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_CAPTION));

        this.progressBar = new ProgressBar();
        this.progressBar.setIndeterminate(true);
        this.progressBar.setWidth(100, Sizeable.Unit.PERCENTAGE);
        content.addComponent(this.messageLabel);
        content.addComponent(this.progressBar);

        this.footer = new MHorizontalLayout().withFullWidth().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        closeButton = new MButton(mc.getMessage(UI_BUTTON_CLOSE)).withStyleName(ValoTheme.BUTTON_PRIMARY).withListener((Button.ClickListener) event -> close());
        this.footer.addComponent(closeButton);
        footer.setComponentAlignment(closeButton, Alignment.MIDDLE_RIGHT);

        window.setContent(content);
    }

    public void open(boolean modal) {
        LOGGER.debug("open window");
        window.setModal(modal);
        final UI ui = UI.getCurrent();
        if (!ui.getWindows().contains(window)) {
            ui.setPollInterval(500);
            ui.addWindow(window);
        }
    }

    public void close() {
        // disable polling. In most cases, this will already be done by either onSuccess or onError
        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().removeWindow(window);
        LOGGER.debug("close");
    }

    public void watch(ListenableProgressFuture<?> future) {
        future.addProgressReceiver(createProgressReceiver());
        future.addCallback(
                this::onSuccess,
                this::onError);
    }

    public ProgressReceiver createProgressReceiver() {

        return new UIAccessProgressReceiverProxy(window::getUI, new AbstractProgressReceiver() {
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

    private void onSuccess(Object res) {
      final UI ui = window.getUI();
      ui.access(() -> {
        // once the process is finished, there is no need for further polling.
        ui.setPollInterval(-1);

        // FIXME Disyplaying any kind of result must be refactored out of this dialog.
            if (res instanceof PackageManagerOperationReport) {
                onSuccess((PackageManagerOperationReport) res);
            } else if (res instanceof PackageListUpdateReport) {
                onSuccess((PackageListUpdateReport) res);
            }
        });
    }

    /**
     * Shows report summary for {@linkplain PackageListUpdateReport} type
     * @param report {@linkplain PackageListUpdateReport}
     */
    private void onSuccess(PackageListUpdateReport report) {
      final Label checkLabel = new Label(VaadinIcons.CHECK_CIRCLE.getHtml() + " " + mc.getMessage(UI_CAPTION_SUCCESS), ContentMode.HTML);
      checkLabel.setStyleName("state-label-success-xl");
      VerticalLayout operationReport = new VerticalLayout();
      operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_ADDED) + " " + report.getAdded()));
      operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_REMOVED) + " " + report.getRemoved()));
      operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_UPDATED) + " " + report.getUpdated()));
      operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_SKIPPED) + " " + report.getSkipped()));
      window.setContent(new MVerticalLayout(checkLabel, operationReport, footer).withFullWidth().withMargin(true).withSpacing(true));
    }

    /**
     * Shows report summary for {@linkplain PackageManagerOperationReport} type
     * @param report {@linkplain PackageManagerOperationReport}
     */
    public void onSuccess(PackageManagerOperationReport report) {
        final Label checkLabel = new Label(VaadinIcons.CHECK_CIRCLE.getHtml() + " " + mc.getMessage(UI_CAPTION_SUCCESS), ContentMode.HTML);
        checkLabel.setStyleName("state-label-success-xl");

        Grid<PackageReport> operationReport = new Grid<>();
        if (report != null) {
          operationReport.setDataProvider(DataProvider.ofCollection(report.getPackageReports()));
          operationReport.setWidth("100%");
          operationReport.setHeightByRows(report.getPackageReports().size());
          operationReport.addColumn(PackageReport::getPackageName);
          operationReport.addColumn(PackageReport::getType);
          operationReport.setHeaderVisible(false);
        }

        window.setContent(new MVerticalLayout(checkLabel, operationReport, footer).withFullWidth().withMargin(true).withSpacing(true));
    }

    public void onError(final Throwable throwable) {
      final UI ui = window.getUI();
      ui.access(()-> {
        // once the process is finished, there is no need for further polling.
        ui.setPollInterval(-1);

        final Label errorLabel = new Label(VaadinIcons.SPINNER.getHtml() + " " + mc.getMessage(UI_CAPTION_FAILED), ContentMode.HTML);
        errorLabel.setStyleName("state-label-error-xl");

        Label errorMessage;
        if (throwable instanceof PackageManagerDownloadException) {
            errorMessage = new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_AT_SOURCE_ERROR, ((PackageManagerDownloadException) throwable).getDownloadUrl()));
        } else {
            errorMessage = new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_ERROR));
        }
        window.setContent(new MVerticalLayout(errorLabel, errorMessage, footer).withFullWidth().withMargin(true).withSpacing(true));
      });
    }

    protected void onCompleted() {
        LOGGER.debug("completed");
    }

    protected void onProgress(double progress) {

        if (progress < 0) {
            progressBar.setIndeterminate(true);
            LOGGER.debug("onProgress setIndeterminate " + progress);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setValue((float) progress);

            NumberFormat defaultFormat = NumberFormat.getPercentInstance();
            defaultFormat.setMinimumFractionDigits(1);

            this.messageLabel.setValue(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_CAPTION) + " " + defaultFormat.format(progress));

            LOGGER.debug("onProgress " + progress);
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
