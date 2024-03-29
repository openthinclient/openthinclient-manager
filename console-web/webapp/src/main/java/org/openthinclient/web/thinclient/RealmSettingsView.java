package org.openthinclient.web.thinclient;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.i18n.ConsoleWebMessages;
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
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.util.*;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SETTINGS_HEADER;

@SuppressWarnings("serial")
@SpringView(name = RealmSettingsView.NAME, ui= SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode="UI_SETTINGS_HEADER", order = 10)
public final class RealmSettingsView extends AbstractProfileView<Realm> {

  public static final String NAME = "realm_settings_view";
  public static final ConsoleWebMessages TITLE_KEY = UI_SETTINGS_HEADER;
  private static final Logger LOGGER = LoggerFactory.getLogger(RealmSettingsView.class);

  @Autowired @Qualifier("settingsSideBar")
  private OTCSideBar settingsSideBar;

  private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

  @Override
  public Set<Realm> getAllItems() {
     try {
       Set<Realm> allRealms = getRealmService().findAllRealms();
       HashSet<Realm> hashSet = new HashSet<>();
       hashSet.addAll(allRealms);
       return hashSet;
     } catch (Exception e) {
      LOGGER.error("Cannot load realm", e);
      showError(e);
      return new HashSet<>();
     }
  }

  @Override
  protected Class<Realm> getItemClass() {
    return Realm.class;
  }

  public ProfilePanel createProfilePanel(Realm profile) throws BuildProfileException {
    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_SETTINGS_HEADER), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.hideCopyButton();
    presenter.hideDeleteButton();

    // remove name ("RealmConfiguration") - it's an internal value that should not be displayed
    otcPropertyGroups.get(0).removeProperty("name");
    // remove type
    otcPropertyGroups.get(0).removeProperty("type");

    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(Realm realm) {
    return null;
  }

  @Override
  protected Realm newProfile() {
    return new Realm();
  }

  @Override
  public Realm getFreshProfile(String name) {
    Set<Realm> allRealms = getRealmService().findAllRealms();

    Optional<Realm> first = allRealms.stream().filter(realm -> realm.getName().equals(name)).findFirst();
    if (first.isPresent()) {
      Realm realm = first.get();
      try {
        realm.refresh();
      } catch (DirectoryException e) {
        LOGGER.error("Failed to refresh realm: " + e.getMessage(), e);
      }
      return realm;
    } else {
      // TODO: no realm found: should throw/handle 'no realm'-exception
      return new Realm();
    }
  }

  @Override
  protected <D extends DirectoryObject> Set<D> getMembers(Realm profile, Class<D> clazz) {
    return Collections.emptySet();
  }

  @Override
  public void save(Realm profile) throws ProfileNotSavedException {
    LOGGER.info("Save realm-settings: " + profile);
    try {
      profile.getDirectory().save(profile);
      profile.refresh();
      Audit.logSave("Realm settings");
    } catch (DirectoryException e) {
      throw new ProfileNotSavedException("Cannot save object " + profile, e);
    }
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ settingsSideBar);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    LOGGER.info(this.getViewName() + ".enter(), load RealmConfiguration and update view.");
    Realm realmConfiguration = getFreshProfile("RealmConfiguration");
    showProfile(realmConfiguration);
  }
}
