package org.openthinclient.flow.progress;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.provider.DataProvider;
import org.openthinclient.pkgmgr.exception.PackageManagerDownloadException;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReport;
import org.openthinclient.progress.AbstractProgressReceiver;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

import static org.openthinclient.flow.i18n.ConsoleWebMessages.*;

public class ProgressReceiverDialog {

    private final Logger LOGGER = LoggerFactory.getLogger(ProgressReceiverDialog.class);

    private final ProgressBar progressBar;
    private final Label messageLabel;
    private final Dialog window;
    private final HorizontalLayout footer;
    private final Button closeButton;

    private final IMessageConveyor mc;

    public ProgressReceiverDialog(String caption) {
      
        window = new Dialog();

//        window.setResizable(false);
//        window.setClosable(false);
//        window.setWidth(60, Sizeable.Unit.PERCENTAGE);
//        window.setHeight(null);
//        window.center();

        mc = new MessageConveyor(UI.getCurrent().getLocale());

        final VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        content.setWidth("100%");

        this.messageLabel = new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_CAPTION));

        this.progressBar = new ProgressBar();
        this.progressBar.setIndeterminate(true);
//        this.progressBar.setWidth(100, Sizeable.Unit.PERCENTAGE);
        content.add(this.messageLabel);
        content.add(this.progressBar);

//        this.footer = new MHorizontalLayout().withFullWidth().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
//        closeButton = new MButton(mc.getMessage(UI_BUTTON_CLOSE)).withStyleName(ValoTheme.BUTTON_PRIMARY).withListener((Button.ClickListener) event -> close());
        this.footer = new HorizontalLayout();
        closeButton = new Button(mc.getMessage(UI_BUTTON_CLOSE));
        closeButton.addClickListener(e -> close());
        this.footer.add(closeButton);
//        footer.setComponentAlignment(closeButton, FlexComponent.Alignment.MIDDLE_RIGHT);

        window.add(content);
    }

    // TODO WTM review open/close
    public void open(boolean modal) {
        LOGGER.debug("open window");
//        window.setModal(modal);
        final UI ui = UI.getCurrent();
        if (!ui.getChildren().anyMatch(component -> component.equals(window))) {
            ui.setPollInterval(500);
            ui.add(window);
        }
    }

    public void close() {
        // disable polling. In most cases, this will already be done by either onSuccess or onError
        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().remove(window);
        LOGGER.debug("close");
    }

    public void watch(ListenableProgressFuture<?> future) {
        future.addProgressReceiver(createProgressReceiver());
        future.addCallback(
                this::onSuccess,
                this::onError);
    }

    public ProgressReceiver createProgressReceiver() {

        return new UIAccessProgressReceiverProxy(window.getUI()::get, new AbstractProgressReceiver() {
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
      final UI ui = window.getUI().get();
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
//      final Label checkLabel = new Label(VaadinIcons.CHECK_CIRCLE.getHtml() + " " + mc.getMessage(UI_CAPTION_SUCCESS), ContentMode.HTML);
      final Label checkLabel = new Label(mc.getMessage(UI_CAPTION_SUCCESS));
      checkLabel.setClassName("state-label-success-xl");
      VerticalLayout operationReport = new VerticalLayout();
      operationReport.add(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_ADDED) + " " + report.getAdded()));
      operationReport.add(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_REMOVED) + " " + report.getRemoved()));
      operationReport.add(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_UPDATED) + " " + report.getUpdated()));
      operationReport.add(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_SKIPPED) + " " + report.getSkipped()));
//      window.setContent(new MVerticalLayout(checkLabel, operationReport, footer).withFullWidth().withMargin(true).withSpacing(true));
      window.add(new VerticalLayout(checkLabel, operationReport, footer));
    }

    /**
     * Shows report summary for {@linkplain PackageManagerOperationReport} type
     * @param report {@linkplain PackageManagerOperationReport}
     */
    public void onSuccess(PackageManagerOperationReport report) {
//        final Label checkLabel = new Label(VaadinIcons.CHECK_CIRCLE.getHtml() + " " + mc.getMessage(UI_CAPTION_SUCCESS), ContentMode.HTML);
        final Label checkLabel = new Label(mc.getMessage(UI_CAPTION_SUCCESS));
        checkLabel.setClassName("state-label-success-xl");

        Grid<PackageReport> operationReport = new Grid<>();
        if (report != null) {
          operationReport.setDataProvider(DataProvider.ofCollection(report.getPackageReports()));
          operationReport.setWidth("100%");
//          operationReport.setHeightByRows(report.getPackageReports().size());
          operationReport.addColumn(PackageReport::getPackageName);
          operationReport.addColumn(PackageReport::getType);
//          operationReport.setHeaderVisible(false);
        }

        window.add(new VerticalLayout(checkLabel, operationReport, footer));
    }

    public void onError(final Throwable throwable) {
      final UI ui = window.getUI().get();
      ui.access(()-> {
        // once the process is finished, there is no need for further polling.
        ui.setPollInterval(-1);

        final Label errorLabel = new Label(mc.getMessage(UI_CAPTION_FAILED));
        errorLabel.setClassName("state-label-error-xl");

        Label errorMessage;
        if (throwable instanceof PackageManagerDownloadException) {
            errorMessage = new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_AT_SOURCE_ERROR, ((PackageManagerDownloadException) throwable).getDownloadUrl()));
        } else {
            errorMessage = new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_ERROR));
        }
        window.add(new VerticalLayout(errorLabel, errorMessage, footer));
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

            this.messageLabel.setTitle(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_CAPTION) + " " + defaultFormat.format(progress));

            LOGGER.debug("onProgress " + progress);
        }
    }

    protected void onProgress(String message) {
        messageLabel.setTitle(message);
    }

    protected void onProgress(String message, double progress) {
        onProgress(message);
        onProgress(progress);
    }

}
