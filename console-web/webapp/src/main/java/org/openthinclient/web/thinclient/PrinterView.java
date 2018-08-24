package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.presenter.ReferenceComponentPresenter;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.OtcView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = "printer_view")
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_PRINTER_HEADER", order = 90)
public final class PrinterView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrinterView.class);

  @Autowired
  private ManagerHome managerHome;
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
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ClientGroupService clientGroupService;

   private final IMessageConveyor mc;
   private VerticalLayout right;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public PrinterView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_PRINTER_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());
   }


   @PostConstruct
   private void setup() {
      init();
      setItems((HashSet) printerService.findAll());
   }

  public ProfilePanel createProfilePanel (Profile profile) {

       ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());

       List<OtcPropertyGroup> otcPropertyGroups = null;
       try {
         otcPropertyGroups = builder.getOtcPropertyGroups(profile);
       } catch (BuildProfileException e) {
         showError(e);
         return null;
       }

       // attach save-action
       otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(ipg, profile)));
       // put to panel
       profilePanel.setItemGroups(otcPropertyGroups);

       Set<DirectoryObject> members = ((Printer) profile).getMembers();
       showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER), clientService.findAll(), Client.class);
       showReference(profile, profilePanel, members, mc.getMessage(UI_LOCATION_HEADER), locationService.findAll(), Location.class);
       showReference(profile, profilePanel, members, mc.getMessage(UI_USER_HEADER), userService.findAll(), User.class);

    return profilePanel;
    }

  @Override
  public <T extends Profile> T getFreshProfile(T profile) {
     return (T) printerService.findByName(profile.getName());
  }

  @Override
  public void save(Profile profile) {
    printerService.save((Printer) profile);
  }

}
