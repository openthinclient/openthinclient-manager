package org.openthinclient.web.support;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;

import org.openthinclient.sysreport.SystemReport;
import org.openthinclient.web.support.ui.SystemReportDesign;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_SYSTEMREPORT_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_SYSTEMREPORT_DESCRIPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_SYSTEMREPORT_TRANSMITTED;

@SpringView(name = "system-report")
@SideBarItem(sectionId = DashboardSections.SUPPORT, captionCode = "UI_SUPPORT_SYSTEMREPORT_CAPTION")
public class SystemReportView extends SystemReportDesign implements View {

  @Autowired
  SystemReportGenerator generator;
  @Autowired
  SystemReportPublisher publisher;

  public SystemReportView() {

    final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    header.setTitle(mc.getMessage(UI_SUPPORT_SYSTEMREPORT_CAPTION));

    descriptionLabel.setValue(mc.getMessage(UI_SUPPORT_SYSTEMREPORT_DESCRIPTION));
    reportTransmittedDescriptionLabel.setValue(mc.getMessage(UI_SUPPORT_SYSTEMREPORT_TRANSMITTED));

    Responsive.makeResponsive(this);

    resultLayout.setVisible(false);

    generateReportButton.addClickListener((e) -> {
      final SystemReport report = generator.generateReport();
      final SystemReportPublisher.SystemReportUploadResult result = publisher.upload(report);

      supportIdLabel.setValue(result.getSupportId());

      resultLayout.setVisible(true);
      generateReportButton.setVisible(false);


    });
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }
}
