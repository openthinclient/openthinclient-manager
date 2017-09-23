package org.openthinclient.web.support;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SOURCESLISTNAVIGATORVIEW_CAPTION;

@SpringView(name = "system-report")
@SideBarItem(sectionId = DashboardSections.SUPPORT, captionCode = "UI_SUPPORT_SYSTEMREPORT_CAPTION")
public class SystemReportView extends Panel implements View {

  @Autowired
  SystemReportGenerator generator;
  @Autowired
  SystemReportPublisher publisher;

  public SystemReportView() {

    final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    addStyleName(ValoTheme.PANEL_BORDERLESS);
    setSizeFull();

    VerticalLayout root = new VerticalLayout();
    root.setSizeFull();
    root.setMargin(true);
    root.addStyleName("dashboard-view");
    setContent(root);
    Responsive.makeResponsive(root);

    root.addComponent(new ViewHeader(mc.getMessage(UI_SOURCESLISTNAVIGATORVIEW_CAPTION)));


    root.addComponent(new Button("Create System Report", (e) -> {
      final SystemReport report = generator.generateReport();

      final SystemReportPublisher.SystemReportUploadResult result = publisher.upload(report);
      System.out.println("+++++++++++++++++++++++++++++++++++++++++");
      System.out.println(result);
      System.out.println("+++++++++++++++++++++++++++++++++++++++++");
    }));
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }
}
