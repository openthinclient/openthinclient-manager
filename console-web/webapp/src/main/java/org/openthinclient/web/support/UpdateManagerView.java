package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.api.proc.RuntimeProcessExecutor;
import org.openthinclient.api.versioncheck.AvailableVersionChecker;
import org.openthinclient.api.versioncheck.UpdateDescriptor;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.component.NotificationDialog;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.net.URI;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SpringView(name = UpdateManagerView.NAME, ui = SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_APPLICATION_HEADER", order = 70)
public class UpdateManagerView extends Panel implements View {

  public final static String NAME = "support";

  /** serialVersionUID */
  private static final long serialVersionUID = -8836300902351197949L;

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateManagerView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private DownloadManager downloadManager;

  @Value("${application.version}")
  private String applicationVersion;
  @Value("${otc.application.version.update.location}")
  private String updateLocation;
  @Value("${otc.application.version.update.process}")
  private String updateProcess;

  final MessageConveyor mc;
  final CssLayout root ;

  private CssLayout content = null;
  private Button button = null;
  private ProgressBar bar = null;
  private Label labelUpdateProgress = null;
  private ProcessStatus processStatus = ProcessStatus.UNSET;

  public UpdateManagerView(EventBus.SessionEventBus eventBus) {

     mc = new MessageConveyor(UI.getCurrent().getLocale());
     eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_SUPPORT_CONSOLE_ABOUT_HEADER)));
     setSizeFull();

     root = new CssLayout();
     root.setStyleName("updateview");
     setContent(root);

  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SUPPORT_APPLICATION_HEADER);
  }

  @PostConstruct
  private void init() {
      // TODO: get update-status from globally-stored location (and not from session)
      if (UI.getCurrent().getSession().getAttribute("processStatus") != null) {
          processStatus = (ProcessStatus) UI.getCurrent().getSession().getAttribute("processStatus");
      }
      buildContent();
  }
  
  private void buildContent() {

     Label versionInformation = new Label(mc.getMessage(UI_SUPPORT_CURRENT_APPLICATION_VERSION, applicationVersion));
     versionInformation.addStyleName("versionInformation");

     root.addComponent(versionInformation);

     this.content = new CssLayout();
     if (processStatus == ProcessStatus.RUNNING) {
         handleUpdateInProgress();
     } else {
         updateView();
     }
     root.addComponents(content);

     CssLayout wikilinks = new CssLayout();
     wikilinks.addComponents(
       new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_WIKI_CAPTION)),
       new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_WIKI_VERSION_INFORMATION), ContentMode.HTML),
       new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_WIKI_ROADMAP), ContentMode.HTML)
     );
     wikilinks.addStyleName("wikilinks");
     root.addComponent(wikilinks);
  }

    private void buildUpdateCheckView() {

        this.button = new Button(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_CHECK_APPLICATION_VERSION_BUTTON));
        this.button.addClickListener(e -> {
            AvailableVersionChecker avc = new AvailableVersionChecker(managerHome, downloadManager);
            try {
                // TODO add UI-ProgressReceiver
                UpdateDescriptor versionDescriptor = avc.getVersion(new URI(this.updateLocation), new NoopProgressReceiver());
                Version newVersion = Version.parse(versionDescriptor.getNewVersion());
                Version currentVersion = Version.parse(applicationVersion);

                int result = currentVersion.compareTo(newVersion);
                NotificationDialog notification;
                if (result < 0) {
                    notification = new NotificationDialog(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_CAPTION),
                                                          mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_UPDATE, versionDescriptor.getNewVersion()),
                                                          NotificationDialog.NotificationDialogType.PLAIN);
                    Button updateBtn = new Button("Update");
                    final PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
                    updateBtn.addClickListener(event -> {
                        updateBtn.setEnabled(false);
                        RuntimeProcessExecutor.executeManagerUpdateCheck(updateProcess, configuration.getProxyConfiguration(), new RuntimeProcessExecutor.Callback() {
                            @Override
                            public void exited(int exitValue) {
                                setStatus(ProcessStatus.EXIT);
                            }

                            @Override
                            public void prepareShutdown() {
                                setStatus(ProcessStatus.SUCCESS);
                            }

                            @Override
                            public void started() {
                                setStatus(ProcessStatus.RUNNING);
                                handleUpdateInProgress();
                            }
                        });
                        notification.close();

                        UI.getCurrent().setPollInterval(500);
                        UI.getCurrent().addPollListener(pollEvent -> updateView());
                    });
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
        content.addComponent(this.button);
    }

    // TODO: store procressUpdate-status in a global 'application'-session (or DB, whatever) - to ensure, all useres obtain the update-process-status
    private void setStatus(ProcessStatus status) {
      this.processStatus = status;
      UI.getCurrent().getSession().setAttribute("processStatus", processStatus);
    }

    private void updateView() {
        LOGGER.debug("Update view for status {}", processStatus);
        switch (processStatus) {
            case UNSET: buildUpdateCheckView();
                break;
            case SUCCESS:
                handleUpdateSuccess();
                setStatus(ProcessStatus.UNSET);
                break;
            case EXIT:
                handleUpdateFailed();
                setStatus(ProcessStatus.UNSET);
                break;
        }
    }

    private void handleUpdateInProgress() {
      if (this.button != null) {
          this.content.removeComponent(this.button);
      }
      this.labelUpdateProgress = new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_RUNNING), ContentMode.HTML);
      content.addComponent(labelUpdateProgress);
      this.bar = new ProgressBar();
      bar.setIndeterminate(true);
      content.addComponent(bar);
  }

  private void handleUpdateFailed() {
      if (this.button != null) {
          this.content.removeComponent(this.button);
      }
      if (this.labelUpdateProgress != null) {
          this.content.removeComponent(this.labelUpdateProgress);
      }
      if (this.bar != null) {
          this.content.removeComponent(this.bar);
      }
      content.addComponent(new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_EXIT), ContentMode.HTML));
      UI.getCurrent().setPollInterval(-1);
  }

    private void handleUpdateSuccess() {
        if (this.button != null) {
            this.content.removeComponent(this.button);
        }
        if (this.labelUpdateProgress != null) {
            this.content.removeComponent(this.labelUpdateProgress);
        }
        if (this.bar != null) {
            this.content.removeComponent(this.bar);
        }
        content.addComponent(new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_SUCCESS), ContentMode.HTML));
        UI.getCurrent().setPollInterval(-1);
    }


  enum ProcessStatus {
      UNSET,
      RUNNING,
      EXIT,
      SUCCESS;
  }
}
