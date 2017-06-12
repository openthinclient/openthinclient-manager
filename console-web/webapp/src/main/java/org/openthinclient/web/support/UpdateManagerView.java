package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.api.proc.RuntimeProcessExecutor;
import org.openthinclient.api.versioncheck.AvailableVersionChecker;
import org.openthinclient.api.versioncheck.UpdateDescriptor;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.component.NotificationDialog;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.net.URI;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SpringView(name = "support")
@SideBarItem(sectionId = DashboardSections.SUPPORT, captionCode = "UI_SUPPORT_APPLICATION_HEADER", order = -100)
public class UpdateManagerView extends Panel implements View {

  /** serialVersionUID */
  private static final long serialVersionUID = -8836300902351197949L;

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateManagerView.class);

  @Value("${application.version}")
  private String applicationVersion;
  @Value("${otc.application.version.update.location}")
  private String updateLocation;
  @Value("${otc.application.version.update.process}")
  private String updateProcess;

  @Autowired
  private ManagerHome managerHome;

  final MessageConveyor mc;
  final VerticalLayout root ;

  public UpdateManagerView() {

     mc = new MessageConveyor(UI.getCurrent().getLocale());
     
     addStyleName(ValoTheme.PANEL_BORDERLESS);
     setSizeFull();
     DashboardEventBus.register(this);

     root = new VerticalLayout();
     root.setSizeFull();
     root.setMargin(true);
     root.addStyleName("dashboard-view");
     setContent(root);
     Responsive.makeResponsive(root);

     root.addComponent(new ViewHeader(mc.getMessage(UI_SUPPORT_CONSOLE_ABOUT_HEADER)));
  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SUPPORT_APPLICATION_HEADER);
  }

  @PostConstruct
  private void init() {
     Component content = buildContent();
     root.addComponent(content);
     root.setExpandRatio(content, 1);
  }
  
  private Component buildContent() {
     
     VerticalLayout content = new VerticalLayout();
     
     final Label labelDescription = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_CURRENT_APPLICATION_VERSION, applicationVersion), ContentMode.HTML);
     content.addComponent(labelDescription);
     Button button = new Button(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_CHECK_APPLICATION_VERSION_BUTTON));
     button.addClickListener(e -> {
         AvailableVersionChecker avc = new AvailableVersionChecker(managerHome);
         try {
             UpdateDescriptor versionDescriptor = avc.getVersion(new URI(this.updateLocation));
             Version newVersion = Version.parse(versionDescriptor.getNewVersion());
             Version currentVersion = Version.parse(applicationVersion);

             int result = currentVersion.compareTo(newVersion);
             NotificationDialog notification;
             if (result < 0) {
                 notification = new NotificationDialog(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_CAPTION),
                                                       mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_UPDATE, versionDescriptor.getNewVersion()),
                                                       NotificationDialog.NotificationDialogType.PLAIN);
                 Button updateBtn = new Button("Update");
                 updateBtn.addClickListener(event -> RuntimeProcessExecutor.executeManagerUpdateCheck(updateProcess));
                 notification.addContent(updateBtn);
             } else {
                 notification = new NotificationDialog(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_CAPTION),
                                                       mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_OK),
                                                       NotificationDialog.NotificationDialogType.SUCCESS);
             }
             notification.open(false);

         } catch (Exception exception) {
             final NotificationDialog notification = new NotificationDialog(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_CAPTION),
                     mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_FAIL),
                     NotificationDialog.NotificationDialogType.ERROR);
             notification.open(false);
             return;
         }

     });
     content.addComponent(button);
     
     return content;
  }

    @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }
}
