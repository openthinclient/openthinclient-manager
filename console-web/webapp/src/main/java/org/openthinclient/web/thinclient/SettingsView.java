package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SETTINGS_HEADER;

@SuppressWarnings("serial")
@SpringView(name = SettingsView.NAME)
// @SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_SETTINGS_HEADER", order = 99)
// @ThemeIcon("icon/sysinfo-white.svg")
public final class SettingsView extends ThinclientView {

  public static final String NAME = "settings_view";
  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsView.class);

  @Autowired
  private RealmService realmService;
  @Autowired
  private SchemaProvider schemaProvider;

   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public SettingsView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_SETTINGS_HEADER, eventBus, notificationService);
   }


  @PostConstruct
  private void setup() {
//    hideItemList();
//    try {
//      Iterator iterator = getAllItems().iterator();
//      if (iterator.hasNext()) {
//        selectItem((DirectoryObject) iterator.next());
//      }
//    } catch (AllItemsListException e) {
//      showError(e);
//    }
  }

  @Override
  public HashSet getAllItems() throws AllItemsListException {
     try {
       Set<Realm> allRealms = realmService.findAllRealms();
       HashSet hashSet = new HashSet();
       hashSet.addAll(allRealms);
       return hashSet;
     } catch (Exception e) {
       throw new AllItemsListException("Cannot load settings.", e);
     }
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Realm.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Realm.class);
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) throws BuildProfileException {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.hideCopyButton();
    presenter.hideDeleteButton();

    // set MetaInformation
    presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));

    // remove last group: last group is named 'hidden objects' and should not be displayed
    otcPropertyGroups.get(1).getGroups().remove(otcPropertyGroups.get(1).getGroups().size() - 1);
    // attach save-action
//    otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(presenter, profile)));
    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
    Set<Realm> allRealms = realmService.findAllRealms();
    return (T) allRealms.stream().filter(realm -> realm.getName().equals(name)).findFirst().get();
  }

  @Override
  public void save(DirectoryObject profile) throws ProfileNotSavedException {
    LOGGER.info("Save realm-settings: " + profile);
    try {
      ((Realm) profile).getDirectory().save(profile);
    } catch (DirectoryException e) {
      throw new ProfileNotSavedException("Cannot save object " + profile, e);
    }
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  protected void selectItem(DirectoryObject directoryObject) {

  }

}
