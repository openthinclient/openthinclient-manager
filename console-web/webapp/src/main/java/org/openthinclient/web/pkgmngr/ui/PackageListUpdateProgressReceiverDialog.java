package org.openthinclient.web.pkgmngr.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CAPTION_SUCCESS;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_ADDED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_REMOVED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_SKIPPED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_UPDATED;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.vaadin.viritin.layouts.MVerticalLayout;

public class PackageListUpdateProgressReceiverDialog extends ProgressReceiverDialog {

    public PackageListUpdateProgressReceiverDialog(String caption) {
      super(caption);
    }

    @Override
    public void onSuccess(Object res) {
      final UI ui = window.getUI();
      ui.access(() -> {
        // once the process is finished, there is no need for further polling.
        ui.setPollInterval(-1);

        if (res != null) {
          PackageListUpdateReport report = (PackageListUpdateReport) res;
          final Label checkLabel = new Label(VaadinIcons.CHECK_CIRCLE.getHtml() + " " + mc.getMessage(UI_CAPTION_SUCCESS), ContentMode.HTML);
          checkLabel.setStyleName("state-label-success-xl");
          VerticalLayout operationReport = new VerticalLayout();
          operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_ADDED) + " " + report.getAdded()));
          operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_REMOVED) + " " + report.getRemoved()));
          operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_UPDATED) + " " + report.getUpdated()));
          operationReport.addComponent(new Label(mc.getMessage(UI_PACKAGESOURCES_UPDATE_PROGRESS_INFO_SKIPPED) + " " + report.getSkipped()));
          window.setContent(new MVerticalLayout(checkLabel, operationReport, footer).withFullWidth().withMargin(true).withSpacing(true));
        }
      });
    }

    /**
     * Shows report summary for {@linkplain PackageListUpdateReport} type
     * @param report {@linkplain PackageListUpdateReport}
     */
    private void onSuccess(PackageListUpdateReport report) {

    }





  }