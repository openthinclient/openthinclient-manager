package org.openthinclient.web.services.ui;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.service.common.ManagedService;
import org.openthinclient.service.common.Service;
import org.openthinclient.service.common.ServiceManager;

public class ServicePanel extends Panel {

  private final Class<Service> serviceClass;
  private final ServiceManager serviceManager;
  private final Label stateLabel;
  private final Button startButton;
  private final Button stopButton;

  public ServicePanel(ServiceManager serviceManager, Class serviceClass) {
    this.serviceClass = serviceClass;
    this.serviceManager = serviceManager;

    String serviceName = serviceClass.getSimpleName();

    if (serviceName.endsWith("Service"))
      serviceName = serviceName.substring(0, serviceName.length() - 7);

    setCaption(serviceName);

    final VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.addComponent(stateLabel = new Label());
    final HorizontalLayout buttonBar = new HorizontalLayout(
            startButton = new Button("Start"),
            stopButton = new Button("Stop")
    );
    buttonBar.setWidth(100, Unit.PERCENTAGE);
    buttonBar.setSpacing(true);
    buttonBar.setExpandRatio(startButton, 1);
    buttonBar.setExpandRatio(stopButton, 1);

    startButton.setStyleName(ValoTheme.BUTTON_FRIENDLY);
    startButton.setWidth(100, Unit.PERCENTAGE);
    startButton.setIcon(FontAwesome.PLAY);
    stopButton.setStyleName(ValoTheme.BUTTON_DANGER);
    stopButton.setWidth(100, Unit.PERCENTAGE);
    stopButton.setIcon(FontAwesome.STOP);

    layout.addComponent(buttonBar);
    setContent(layout);


    startButton.addClickListener(e -> {
      if (getService() != null && !getService().isRunning()) {
        getService().start();
        refresh();
      }
    });

    stopButton.addClickListener(e -> {
      if (getService() != null && getService().isRunning()) {
        getService().stop();
        refresh();
      }
    });
  }

  @Override
  public void attach() {
    super.attach();

    refresh();
  }

  @SuppressWarnings("unchecked")
  public void refresh() {
    final ManagedService service = getService();
    if (service != null) {
      if (service.isRunning()) {
        stateLabel.setValue("Running");
        stateLabel.setStyleName(ValoTheme.LABEL_SUCCESS);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
      } else {
        if (service.isFaulty()) {
          stateLabel.setValue("Error");
          stateLabel.setStyleName(ValoTheme.LABEL_FAILURE);
        } else {
          stateLabel.setValue("Stopped");
          stateLabel.setStyleName(ValoTheme.LABEL_FAILURE);
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
      }
    }
  }

  private ManagedService getService() {
    return serviceManager.getManagedService(serviceClass);
  }
}
