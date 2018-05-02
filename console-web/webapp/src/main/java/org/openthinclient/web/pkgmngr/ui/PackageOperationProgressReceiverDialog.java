package org.openthinclient.web.pkgmngr.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CAPTION_SUCCESS;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReport;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.vaadin.viritin.layouts.MVerticalLayout;

public class PackageOperationProgressReceiverDialog extends ProgressReceiverDialog {

    public PackageOperationProgressReceiverDialog(String caption) {
      super(caption);
    }

    @Override
    public void onSuccess(Object res) {
      final UI ui = window.getUI();
      ui.access(() -> {
        // once the process is finished, there is no need for further polling.
        ui.setPollInterval(-1);

        // display success
        final Label checkLabel = new Label(VaadinIcons.CHECK_CIRCLE.getHtml() + " " + mc.getMessage(UI_CAPTION_SUCCESS), ContentMode.HTML);
        checkLabel.setStyleName("state-label-success-xl");

        Grid<PackageReport> operationReport = new Grid<>();
        if (res != null) {
          PackageManagerOperationReport report = (PackageManagerOperationReport) res;
          operationReport.setDataProvider(DataProvider.ofCollection(report.getPackageReports()));
          operationReport.setWidth("100%");
          operationReport.setHeightByRows(report.getPackageReports().size());
          operationReport.addColumn(PackageReport::getPackageName);
          operationReport.addColumn(PackageReport::getType);
          operationReport.setHeaderVisible(false);
        }

        window.setContent(new MVerticalLayout(checkLabel, operationReport, footer).withFullWidth().withMargin(true).withSpacing(true));
      });
    }

  }