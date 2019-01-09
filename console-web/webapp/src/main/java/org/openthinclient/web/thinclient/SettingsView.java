package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = SettingsView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_SETTINGS_HEADER", order = 99)
@ThemeIcon("icon/sysinfo.svg")
public final class SettingsView extends ThinclientView {

  public static final String NAME = "settings_view";
  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsView.class);
   private final IMessageConveyor mc;
  @Autowired
  private RealmService realmService;
  @Autowired
  private SchemaProvider schemaProvider;

   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public SettingsView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_SETTINGS_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());
   }


  @PostConstruct
  private void setup() {
    setItems(getAllItems());
  }

  @Override
  public HashSet getAllItems() {
    Set<Realm> allRealms = realmService.findAllRealms();
    HashSet hashSet = new HashSet();
    hashSet.addAll(allRealms);
    return hashSet;
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Realm.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Realm.class);
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups;
    try {
      otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);
    } catch (BuildProfileException e) {
      showError(e);
      return null;
    }

    ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.hideCopyButton();
    presenter.hideDeleteButton();

    // set MetaInformation
    presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));

    // attach save-action
    otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(ipg, profile)));
    // put to panel
    profilePanel.setItemGroups(otcPropertyGroups);

    return profilePanel;
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
    Set<Realm> allRealms = realmService.findAllRealms();
    return (T) allRealms.stream().filter(realm -> realm.getName().equals(name)).findFirst().get();
  }

  @Override
  public void save(DirectoryObject profile) {
    try {
      ((Realm) profile).getDirectory().save(profile);
    } catch (DirectoryException e) {
      // TODO: handle exception
      e.printStackTrace();
    }
  }

}
