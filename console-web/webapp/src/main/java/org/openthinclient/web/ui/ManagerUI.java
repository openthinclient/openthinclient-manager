package org.openthinclient.web.ui;

import com.vaadin.data.HasValue;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UserService;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.thinclient.ApplicationGroupView;
import org.openthinclient.web.thinclient.ApplicationView;
import org.openthinclient.web.thinclient.ClientView;
import org.openthinclient.web.thinclient.DeviceView;
import org.openthinclient.web.thinclient.HardwaretypeView;
import org.openthinclient.web.thinclient.LocationView;
import org.openthinclient.web.thinclient.PrinterView;
import org.openthinclient.web.thinclient.UserView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import java.util.ArrayList;
import java.util.List;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_SEARCH_NO_RESULT;

@SpringUI
public final class ManagerUI extends AbstractUI implements View {

  private static final long serialVersionUID = 4314279050575370517L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagerUI.class);

  public static final long REFRESH_DASHBOARD_MILLS = 10000;

  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;
  @Autowired
  private EventBus.SessionEventBus eventBus;

  @Autowired
  private RealmService realmService;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private UserService userService;

  private ComboBox<DirectoryObject> searchTextField;

  private boolean runThread = true;

  @Override
  protected OTCSideBar getSideBar() {
    return deviceSideBar;
  }

  @Override
  protected void init(VaadinRequest request) {
    buildSearchTextField();
    super.init(request);
    Page.getCurrent().getStyles().add(String.format(".openthinclient {--no-results-feedback: \"%s\"}",
                                                    mc.getMessage(UI_COMMON_SEARCH_NO_RESULT)));
    new RefreshDashboardThread().start();
  }

  @Override
  protected void afterNavigatorViewChange(ViewChangeEvent event) {
    searchTextField.setValue(null);
    if(event.getNavigator().getState() != null) {
      deviceSideBar.updateFilterGrid(event.getNewView(), event.getParameters());
    }
    super.afterNavigatorViewChange(event);
  }

  @EventBusListenerMethod
  public void closeOpenWindows(final CloseOpenWindowsEvent event) {
      for (Window window : UI.getCurrent().getWindows()) {
          window.close();
          UI.getCurrent().removeWindow(window);
      }
  }

  @Override
  public void detach() {
      LOGGER.debug("Detach ManagerUI " + this + " and stop Thread");
      runThread = false;
      super.detach();
  }

  @Override
  protected Component buildHeader() {
    CssLayout header = new CssLayout(
      getRealmLabel(),
      searchTextField,
      buildLogoutButton()
    );
    header.addStyleName("header");
    return header;
  }

  private void buildSearchTextField() {
    searchTextField = new ComboBox<>();
    searchTextField.addStyleName("header-searchfield");
    searchTextField.setEmptySelectionAllowed(false);
    searchTextField.setPopupWidth("300px");
    searchTextField.addValueChangeListener(this::onSearchSelect);
    searchTextField.setItemCaptionGenerator(DirectoryObject::getName);
    searchTextField.setItemIconGenerator(profile -> {
      String icon;
      if (profile instanceof Application) {
        icon = ApplicationView.ICON;
      } else if (profile instanceof ApplicationGroup) {
        icon = ApplicationGroupView.ICON;
      } else if (profile instanceof Printer) {
        icon = PrinterView.ICON;
      } else if (profile instanceof HardwareType) {
        icon = HardwaretypeView.ICON;
      } else if (profile instanceof Device) {
        icon = DeviceView.ICON;
      } else if (profile instanceof ClientMetaData) {
        icon = ClientView.ICON;
      } else if (profile instanceof Client) {
        icon = ClientView.ICON;
      } else if (profile instanceof Location) {
        icon = LocationView.ICON;
      } else if (profile instanceof User) {
        icon = UserView.ICON;
      } else {
        return null;
      }
      return new ThemeResource(icon);
    });

    (new Thread() {
      @Override
      public void run() {
        long start = System.currentTimeMillis();
        List<DirectoryObject> directoryObjects = new ArrayList<>();
        try {
          directoryObjects.addAll(applicationService.findAll());
          directoryObjects.addAll(printerService.findAll());
          directoryObjects.addAll(deviceService.findAll());
          directoryObjects.addAll(hardwareTypeService.findAll());
          directoryObjects.addAll(locationService.findAll());
          directoryObjects.addAll(clientService.findAllClientMetaData());
          directoryObjects.addAll(userService.findAll());
          realmService.findAllRealms().forEach(realm ->
            directoryObjects.removeAll(realm.getAdministrators().getMembers())
          );
        } catch (Exception e) {
          LOGGER.warn("Cannot find clients for search: " + e.getMessage());
        }
        LOGGER.info("Setup directoryObjects-grid took " + (System.currentTimeMillis() - start) + "ms");
        eventBus.publish(this, new DashboardEvent.SearchObjectsSetupEvent(directoryObjects));
      }
    }).start();
  }

  @EventBusListenerMethod
  public void setupSearchObjects(DashboardEvent.SearchObjectsSetupEvent ev) {
    ListDataProvider<DirectoryObject> dataProvider = DataProvider.ofCollection(ev.getDirectoryObjects());
    dataProvider.setSortOrder(source -> source.getName().toLowerCase(), SortDirection.ASCENDING);
    searchTextField.setDataProvider(dataProvider.filteringBy(
      (directoryObject, filterText) -> {
        String value = filterText.toLowerCase();
        if(directoryObject.getName().toLowerCase().contains(value)) {
            return true;
        } else if (directoryObject instanceof ClientMetaData) {
          String macaddress = ((ClientMetaData) directoryObject).getMacAddress();
          if(macaddress != null && macaddress.contains(value)) {
            return true;
          }
        }
        return false;
      }
    ));
  }

  private void onSearchSelect(HasValue.ValueChangeEvent<DirectoryObject> event) {
    DirectoryObject directoryObject = event.getValue();
    String navigationState = null;
    if (directoryObject instanceof ApplicationGroup) {
      navigationState = ApplicationGroupView.NAME;
    } else if (directoryObject instanceof Application) {
      navigationState = ApplicationView.NAME;
    } else if (directoryObject instanceof ClientMetaData) {
      navigationState = ClientView.NAME;
    } else if (directoryObject instanceof Client) {
      navigationState = ClientView.NAME;
    } else if (directoryObject instanceof Device) {
      navigationState = DeviceView.NAME;
    } else if (directoryObject instanceof HardwareType) {
      navigationState = HardwaretypeView.NAME;
    } else if (directoryObject instanceof Location) {
      navigationState = LocationView.NAME;
    } else if (directoryObject instanceof Printer) {
      navigationState = PrinterView.NAME;
    } else if (directoryObject instanceof User) {
      navigationState = UserView.NAME;
    }

    if (navigationState != null) {
      getNavigator().navigateTo(navigationState + "/edit/" + directoryObject.getName());
    }
  }

  class RefreshDashboardThread extends Thread {

    @Override
    public void run() {
      LOGGER.info("Refreshing Dashboard each {} seconds.", (REFRESH_DASHBOARD_MILLS /1000));
      final ManagerUI ui = ManagerUI.this;
      try {
        // Update the data for a while
        while (runThread) {
          Thread.sleep(REFRESH_DASHBOARD_MILLS);
          if (ui.isAttached()) {
            try {
              ui.access(new Runnable() {
                @Override
                public void run() {
                  eventBus.publish(this, new DashboardEvent.PXEClientListRefreshEvent(null));
                }
              });
            } catch(com.vaadin.ui.UIDetachedException e){
              LOGGER.info("UIDetachedException detected, ui class=" + ui);
//            Authentication authentication = vaadinSecurity.getAuthentication();
//            LOGGER.error("UIDetachedException found when accessing ManagerUI with authentication="+authentication);
//            LOGGER.error("detached exception is "+e.getMessage());
            }
          } else {
            LOGGER.debug(ui + " not attached.");
          }
        }
      } catch (InterruptedException e) {
        LOGGER.error("Error while executing RefreshDashboardThread", e);
      }
    }
  }
}
