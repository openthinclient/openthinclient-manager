package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.SerializableComparator;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.api.ldif.export.LdifExporterService;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.component.ProfilesListOverviewPanel;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotDeletedException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.model.DeleteMandate;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilesListOverviewPanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_ERROR_DIRECTORY_EXCEPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_NAME_ALREADY_EXISTS;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION;

public abstract class AbstractDirectoryObjectView<P extends DirectoryObject> extends Panel implements View {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  @Autowired
  protected ManagerHome managerHome;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired
  private RealmService realmService;
  @Autowired
  private ClientService clientService;
  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;

  protected IMessageConveyor mc;
  private VerticalLayout clientSettingsVL;
  private CssLayout clientReferencesCL;
  private Component clientReferencesCaption;
  protected ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

  protected final CssLayout overviewCL;
  private final CssLayout clientCL;

  public AbstractDirectoryObjectView() {
    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setStyleName("item-view");
    setSizeFull();

    // clientSettingsVL main content
    CssLayout view = new CssLayout();
    view.addStyleName("maincontent");
    view.setResponsive(true);
    view.setSizeFull();

    // setup thinclient overview-state
    overviewCL = new CssLayout();
    overviewCL.addStyleName("profiles-overview");
    view.addComponent(overviewCL);

    // setup thinclient settings and references
    clientCL = new CssLayout();
    clientCL.addStyleName("profile");
    clientCL.setSizeFull();
    clientSettingsVL = new VerticalLayout();
    clientSettingsVL.addStyleName("profile-settings");
    clientSettingsVL.setMargin(new MarginInfo(false, false, false, false));
    clientSettingsVL.setSizeFull();
    clientSettingsVL.setSpacing(false);
    clientReferencesCL = new CssLayout();
    clientReferencesCL.addStyleName("profile-references");
    clientReferencesCL.setVisible(false);
    clientReferencesCaption = new Button(mc.getMessage(UI_THINCLIENTS_HINT_ASSOCIATION), this::toggleVisibilityClass);
    clientReferencesCaption.setPrimaryStyleName("references-caption");
    clientCL.addComponents(clientSettingsVL, clientReferencesCL);
    clientCL.setVisible(false);
    view.addComponent(clientCL);

    setContent(view);
  }

  void toggleVisibilityClass(Button.ClickEvent ev) {
    if(getStyleName().contains("expanded")) {
      removeStyleName("expanded");
    } else {
      addStyleName("expanded");
    }
  }

  public abstract ProfilePanel createProfilePanel(P item) throws BuildProfileException;

  public abstract ProfileReferencesPanel createReferencesPanel(P profile) throws BuildProfileException;

  public abstract Set<? extends DirectoryObject> getAllItems();

  protected Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(getItemClass(), schemaName);
  }

  protected Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(getItemClass()))
                 .collect(Collectors.toMap(name -> name,
                                            name -> getSchema(name).getLabel()));
  }

  public abstract String getViewName();

  public String getParentViewName() {
    return getViewName();
  }

  public abstract ConsoleWebMessages getViewTitleKey();

  protected abstract Class<P> getItemClass();

  protected abstract P newProfile();

  public abstract P getFreshProfile(String profileName);

  protected abstract <D extends DirectoryObject> Set<D> getMembers(P profile, Class<D> clazz);

  public abstract void save(P profile) throws ProfileNotSavedException;

  public void delete(P profile) throws ProfileNotDeletedException {
    Realm realm = profile.getRealm();
    try {
      realm.getDirectory().delete(profile);
    } catch (DirectoryException e) {
      throw new ProfileNotDeletedException("Cannot delete object " + profile, e);
    }
    Audit.logDelete(profile);
  }

  private Client getClient(String name) {
    return clientService.findByName(name);
  }

  protected void addOverviewComponent(Component c) {
    overviewCL.addComponent(c);
  }

  public void showError(Exception e) {
    overviewCL.removeAllComponents();
    clientSettingsVL.removeAllComponents();
    clientReferencesCL.removeAllComponents();

    String message;
    if (e.getCause() instanceof DirectoryException) {
      message = mc.getMessage(UI_ERROR_DIRECTORY_EXCEPTION);
    } else {
      message = e.getLocalizedMessage();
    }

    Label emptyScreenHint = new Label(
        VaadinIcons.WARNING.getHtml() + "&nbsp;&nbsp;&nbsp;" + mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ERROR) + message,
        ContentMode.HTML);
    emptyScreenHint.setStyleName("errorScreenHint");
    clientSettingsVL.addComponent(emptyScreenHint);
  }

  /**
   * Add/remove members (references) to profile and save
   * @param profile to be changed
   * @param values the state of value to be saved
   * @param clazz subset of member-types which has been modified
   */
  protected <T extends DirectoryObject>
  void saveReference(P profile, List<Item> values,
                      Set<? extends DirectoryObject> profileAndDirectoryObjects,
                      Class<T> clazz) {

    Set<T> members = getMembers(profile, clazz);

    List<Item> oldValues = ProfilePropertiesBuilder.createFilteredItemsFromDO(members, clazz);
    oldValues.forEach(oldItem -> {
      if (values.contains(oldItem)) {
        LOGGER.debug("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.debug("Remove oldValue from members: " + oldItem);
        // get values from available-values set and remove members
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.stream()
                                        .filter(o -> o.getName().equals(oldItem.getName()))
                                        .findFirst();
        if (directoryObject.isPresent()) {
          DirectoryObject object = directoryObject.get();
          if (object.getClass().equals(ClientMetaData.class)) { // we need to get a full client-profile
            members.remove(getClient(object.getName()));
          } else {
            members.remove(object);
          }
        } else {
          LOGGER.info("DirectoryObject (to remove) not found for " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
      if (newValue != null && !oldValues.contains(newValue)) {
        LOGGER.debug("Add newValue to members: " + newValue);
        // get values from available-values set and add to members
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.stream()
                                        .filter(o -> o.getName().equals(newValue.getName()))
                                        .findFirst();
        if (directoryObject.isPresent()) {
          DirectoryObject object = directoryObject.get();
          if (object.getClass().equals(ClientMetaData.class)) { // we need to get a full client-profile
            members.add((T) getClient(object.getName()));
          } else {
            members.add((T) object);
          }
        } else {
          LOGGER.info("DirectoryObject not found for " + newValue);
        }
      }
    });

    saveProfile(profile, null);
  }

  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.debug("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(getViewName(), directoryObject, getAllItems());
  }

  /**
   * Save profile, return success status
   * @param profile Profile
   * @param panelPresenter
   * @return true if save action completed successfully
   */
  public boolean saveProfile(P profile, DirectoryObjectPanelPresenter panelPresenter) {
    try {
      save(profile);
      LOGGER.info("Profile saved {}", profile);
      if (panelPresenter != null) {
        panelPresenter.setInfo(mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SAVE_SUCCESS));
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Cannot save profile", e);
      if (panelPresenter != null) {
        panelPresenter.setError(mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SAVE_ERROR) + e.getMessage());
      }
      return false;
    }
  }

  abstract protected void showProfileMetadata(P profile);


  public void showProfileMetadataPanel(ProfilePanel panel) {
    overviewCL.setVisible(false);
    clientReferencesCL.setVisible(false);
    clientCL.setVisible(true);

    clientSettingsVL.removeAllComponents();
    clientSettingsVL.addComponent(panel);
  }

  protected void addProfileNameAlreadyExistsValidator(OtcPropertyGroup meta) {
    meta.getProperty("name").ifPresent(nameProperty -> {
      nameProperty.getConfiguration().getValidators().add(new AbstractValidator<String>(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS)) {
        @Override
        public ValidationResult apply(String value, ValueContext context) {
          DirectoryObject directoryObject = getFreshProfile(value);
          return (nameProperty.getInitialValue() == null &&  directoryObject == null) ||  // name-property wasn't set before and no object was found
              (nameProperty.getInitialValue() != null && nameProperty.getInitialValue().equals(value) && directoryObject != null) || // name property not changed, and directorObject found, the profile changed case
              (nameProperty.getInitialValue() != null && !nameProperty.getInitialValue().equals(value) && directoryObject == null)   // property changed, but no directoryObject found, name is unique
              ? ValidationResult.ok() : ValidationResult.error(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS));
        }
      });
    });
  }

  protected SerializableComparator<DirectoryObject> getComparator() {
    return Comparator.comparing(
      DirectoryObject::getName,
      String::compareToIgnoreCase
    )::compare;
  }

  // Overview panel setup
  public ProfilesListOverviewPanelPresenter createOverviewItemlistPanel(ConsoleWebMessages i18nTitleKey, Set items, boolean enabled) {

    ProfilesListOverviewPanel plop = new ProfilesListOverviewPanel(i18nTitleKey, enabled);
    ProfilesListOverviewPanelPresenter plopPresenter = new ProfilesListOverviewPanelPresenter(this, plop, new LdifExporterService(realmService.getDefaultRealm().getConnectionDescriptor()), null);

    ListDataProvider<DirectoryObject> dataProvider = DataProvider.ofCollection(items);
    dataProvider.setSortComparator(getComparator());
    plop.setDataProvider(dataProvider);
    plopPresenter.setVisible(true);

    return plopPresenter;
  }


  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    String[] params = Optional.ofNullable(event.getParameters()).orElse("").split("/", 2);
    if (params.length > 0) {
      // handle create action
      if ("create".equals(params[0])) {
        showProfileMetadata(newProfile());
      // register new client with mac-address
      } else if ("register".equals(params[0])
                && params.length == 2
                && ClientView.NAME.equals(event.getViewName())) {
        Client client = new Client();
        client.setMacAddress(params[1]);
        showProfileMetadata((P)client);

      // view-profile action
      } else if("edit".equals(params[0])
                && params.length == 2
                && params[1].length() > 0) {
        P profile = getFreshProfile(params[1]);
        if (profile != null) {
          showProfile(profile);
        } else {
          LOGGER.info("No profile found for name '" + params[1] + "'.");
        }

      // initial overview
      } else {
        showOverview();
      }
    }
  }

  /**
   * Display overview-page with list of items
   */
  public void showOverview(boolean enabled) {
    ProfilesListOverviewPanelPresenter overviewPanelPresenter = createOverviewItemlistPanel(getViewTitleKey(), getAllItems(), enabled);
    overviewPanelPresenter.setDeleteMandatSupplier(createDeleteMandateFunction());
    displayOverviewPanel(overviewPanelPresenter.getPanel());
  }

  public void showOverview() {
    showOverview(true);
  }

  /**
   * Overwritten by Hardware- and location type to prevent deletion of dependent items
   * @return function-supplier to check item-dependencies
   */
  protected Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
    return null;
  }

  public void showProfile(P profile) {
    try {
      ProfilePanel profilePanel = createProfilePanel(profile);
      ProfileReferencesPanel profileReferencesPanel = createReferencesPanel(profile);
      displayProfilePanel(profilePanel, profileReferencesPanel);
    } catch (BuildProfileException e) {
      LOGGER.error("Failed to build profile!", e);
      showError(e);
    }
  }

  public void displayOverviewPanel(ProfilesListOverviewPanel overviewPanel) {
    clientSettingsVL.setVisible(false);
    clientReferencesCL.setVisible(false);
    clientCL.setVisible(false);

    overviewCL.removeAllComponents();
    overviewCL.addComponent(overviewPanel);
    overviewCL.setVisible(true);
  }

  /** Display the settings and references of profile, remove actions-panes */
  public void displayProfilePanel(ProfilePanel profilePanel, ProfileReferencesPanel profileReferencesPanel) {
    overviewCL.setVisible(false);
    clientCL.setVisible(true);
    clientSettingsVL.removeAllComponents();
    clientSettingsVL.addComponent(profilePanel);
    clientReferencesCL.removeAllComponents();
    if (profileReferencesPanel != null) {
      clientReferencesCL.addComponent(clientReferencesCaption);
      clientReferencesCL.addComponent(profileReferencesPanel);
      clientReferencesCL.setVisible(true);
    } else {
      clientReferencesCL.setVisible(false);
    }
  }

  public void navigateTo(DirectoryObject directoryObject) {
    Navigator navigator = UI.getCurrent().getNavigator();
    if (directoryObject != null) {
      navigator.navigateTo(getViewName() + "/edit/" + directoryObject.getName());
    } else {
      navigator.navigateTo(getParentViewName());
    }
  }

  public RealmService getRealmService() {
    return realmService;
  }
}
