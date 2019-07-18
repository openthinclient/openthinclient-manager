package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SETTINGS_HEADER;

@SuppressWarnings("serial")
@SpringView(name = RealmSettingsView.NAME, ui= SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode="UI_SETTINGS_HEADER", order = 10)
public final class RealmSettingsView extends AbstractThinclientView {

  public static final String NAME = "realm_settings_view";
  private static final Logger LOGGER = LoggerFactory.getLogger(RealmSettingsView.class);
  private IMessageConveyor mc;

  @Autowired
  private RealmService realmService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired @Qualifier("settingsSideBar")
  private OTCSideBar settingsSideBar;

   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public RealmSettingsView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_SETTINGS_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());
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
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(Realm.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) throws BuildProfileException {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_SETTINGS_HEADER), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.hideCopyButton();
    presenter.hideDeleteButton();

    // set MetaInformation
    presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));

    // remove last group: last group is named 'hidden objects' and should not be displayed
    otcPropertyGroups.get(1).getGroups().remove(otcPropertyGroups.get(1).getGroups().size() - 1);
    // remove 'BootOptions' because it's not working
    otcPropertyGroups.get(1).getGroups().remove(otcPropertyGroups.get(1).getGroups().get(3));

    // attach save-action
//    otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(presenter, profile)));
    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(DirectoryObject item) throws BuildProfileException {
    return null;
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
      ((Realm) profile).refresh();
    } catch (DirectoryException e) {
      throw new ProfileNotSavedException("Cannot save object " + profile, e);
    }
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ settingsSideBar);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    try {
      DirectoryObject realmConfiguration = getFreshProfile("RealmConfiguration");
      ProfilePanel profilePanel = createProfilePanel(realmConfiguration);
      ProfileReferencesPanel profileReferencesPanel = createReferencesPanel(realmConfiguration);
      displayProfilePanel(profilePanel, profileReferencesPanel);
    } catch (BuildProfileException e) {
      showError(e);
    }
  }
}
