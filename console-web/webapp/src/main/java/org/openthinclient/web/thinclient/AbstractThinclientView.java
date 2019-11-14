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
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.api.ldif.export.LdifExporterService;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.component.ProfilesListOverviewPanel;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.model.DeleteMandate;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilesListOverviewPanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPasswordProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_ERROR_DIRECTORY_EXCEPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_NAME_ALREADY_EXISTS;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_PANEL_NEW_PROFILE_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION;

public abstract class AbstractThinclientView extends Panel implements View {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  @Autowired
  protected ManagerHome managerHome;
  @Autowired
  private RealmService realmService;

  protected IMessageConveyor mc;
  private VerticalLayout clientSettingsVL;
  private CssLayout clientReferencesCL;
  private Component clientReferencesCaption;
  protected ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

  private final HorizontalLayout actionRow;
  protected final CssLayout overviewCL;
  private final CssLayout clientCL;

  public AbstractThinclientView(ConsoleWebMessages i18nTitleKey, EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setStyleName("thinclientview");
    setSizeFull();

    // clientSettingsVL main content
    CssLayout view = new CssLayout();
    view.addStyleName("maincontent");
    view.setResponsive(true);
    view.setSizeFull();

    // setup action row
    actionRow = new HorizontalLayout();
    actionRow.addStyleName("profiles-actionrow");
    view.addComponent(actionRow);

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

  public abstract ProfilePanel createProfilePanel(DirectoryObject item) throws BuildProfileException;

  public abstract ProfileReferencesPanel createReferencesPanel(DirectoryObject item) throws BuildProfileException;

  public abstract Set getAllItems() throws AllItemsListException;

  public abstract Schema getSchema(String value);

  public abstract Map<String, String> getSchemaNames();

  public abstract String getViewName();

  public String getParentViewName() {
    return getViewName();
  }

  public abstract ConsoleWebMessages getViewTitleKey();

  public abstract <T extends DirectoryObject> T getFreshProfile(String profileName);

  public abstract void save(DirectoryObject profile) throws ProfileNotSavedException;

  public abstract Client getClient(String name);

  public void addCreateActionButton(String caption, String icon, String target) {
    Button action = new Button(caption, new ThemeResource(icon));
    action.addStyleName("thinclient-action-button");
    action.addClickListener(e -> UI.getCurrent().getNavigator().navigateTo(target));
    actionRow.addComponent(action);
  }

  protected void addOverviewComponent(Component c) {
    overviewCL.addComponent(c);
  }

  public void showError(Exception e) {
    actionRow.removeAllComponents();
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
   * Add/remove device associations to profile and save
   * @param profile to be changed
   * @param values the state of value to be saved
   */
  public <T extends DirectoryObject> void saveAssociations(AssociatedObjectsProvider profile, List<Item> values, Set<T> directoryObjects, Class<T> clazz) {

    Map<Class, Set<? extends DirectoryObject>> associatedObjects = profile.getAssociatedObjects();
    Set<T> association = (Set<T>) associatedObjects.get(clazz);

    List<Item> oldValues = builder.createFilteredItemsFromDO(association, clazz);
    oldValues.forEach(oldItem -> {
      if (values.contains(oldItem)) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from members: " + oldItem);
        // get values from available-values set and remove members
        Optional<? extends DirectoryObject> directoryObject = directoryObjects.stream().filter(o -> o.getName().equals(oldItem.getName())).findFirst();
        if (directoryObject.isPresent()) {
          association.remove(directoryObject.get());
        } else {
          LOGGER.info("Device (to remove) not found for " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
      if (!oldValues.contains(newValue)) {
        LOGGER.info("Add newValue to members: " + newValue);
        // get values from available-values set and add to members
        Optional<? extends DirectoryObject> directoryObject = directoryObjects.stream().filter(o -> o.getName().equals(newValue.getName())).findFirst();
        if (directoryObject.isPresent()) {
          association.add((T) directoryObject.get());
        } else {
          LOGGER.info("DirectoryObject not found for " + newValue);
        }
      }
    });

    saveProfile((Profile) profile, null);
  }


  /**
   * Add/remove members (references) to profile and save
   * @param profile to be changed
   * @param values the state of value to be saved
   * @param clazz subset of member-types which has been modified
   */
  protected <T extends DirectoryObject> void saveReference(DirectoryObject profile, List<Item> values, Set<T> profileAndDirectoryObjects, Class clazz) {

    Set<T> members;
    if (profile instanceof Application) {
      members = (Set<T>) ((Application) profile).getMembers();

    } else if (profile instanceof ApplicationGroup) {
      if (clazz.equals(Application.class)) {
        members = (Set<T>) ((ApplicationGroup) profile).getApplications();
      } else {
        members = (Set<T>) ((ApplicationGroup) profile).getMembers();
      }

    } else if (profile instanceof Printer) {
      members = (Set<T>) ((Printer) profile).getMembers();

    } else if (profile instanceof HardwareType) {
      // TODO: nur ThinclientGruppen werden vom LDAP als 'members' behandelt, Thinclients werden ignoriert
      Set<? extends DirectoryObject> clients = ((HardwareType) profile).getMembers();
      clients.stream().forEach(o -> {
        LOGGER.info("This class should be of Type Client.class: {}" + o.getClass());
      });
      members = (Set<T>) clients;

    } else if (profile instanceof Device) {
      members = ((Device) profile).getMembers();

    } else if (profile instanceof Client) {
        if (clazz.equals(ClientGroup.class)) {
          members = (Set<T>) ((Client) profile).getClientGroups();
        } else if (clazz.equals(Device.class)) {
          members = (Set<T>) ((Client) profile).getDevices();
        } else if (clazz.equals(Printer.class)) {
          members = (Set<T>) ((Client) profile).getPrinters();
        } else if (clazz.equals(Application.class)) {
          members = (Set<T>) ((Client) profile).getApplications();
        } else if (clazz.equals(ApplicationGroup.class)) {
          members = (Set<T>) ((Client) profile).getApplicationGroups();
        } else {
          members = null;
        }

    } else if (profile instanceof Location) {
      members = (Set<T>) ((Location) profile).getPrinters();

    } else if (profile instanceof UserGroup) {
      if (clazz.equals(User.class)) {
        members = (Set<T>) ((UserGroup) profile).getMembers();
      } else if (clazz.equals(ApplicationGroup.class)) {
        members = (Set<T>) ((UserGroup) profile).getApplicationGroups();
      } else if (clazz.equals(Application.class)) {
        members = (Set<T>) ((UserGroup) profile).getApplications();
      } else if (clazz.equals(Printer.class)) {
        members = (Set<T>) ((UserGroup) profile).getPrinters();
      } else {
        members = null;
      }

    } else if (profile instanceof User) {
      if (clazz.equals(UserGroup.class)) {
        members = (Set<T>) ((User) profile).getUserGroups();
      } else if (clazz.equals(ApplicationGroup.class)) {
        members = (Set<T>) ((User) profile).getApplicationGroups();
      } else if (clazz.equals(Printer.class)) {
        members = (Set<T>) ((User) profile).getPrinters();
      } else if (clazz.equals(Application.class)) {
        members = (Set<T>) ((User) profile).getApplications();
      } else {
        members = null;
      }

    } else {
      throw new RuntimeException("Not implemented Profile-type: " + profile);
    }

    List<Item> oldValues = builder.createFilteredItemsFromDO(members, clazz);
    oldValues.forEach(oldItem -> {
      if (values.contains(oldItem)) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from members: " + oldItem);
        // get values from available-values set and remove members
        Optional<T> directoryObject = profileAndDirectoryObjects.stream().filter(o -> o.getName().equals(oldItem.getName())).findFirst();
        if (directoryObject.isPresent()) {
          DirectoryObject object = directoryObject.get();
          if (object instanceof ClientMetaData) { // we need to get a full client-profile
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
        LOGGER.info("Add newValue to members: " + newValue);
        // get values from available-values set and add to members
        Optional<T> directoryObject = profileAndDirectoryObjects.stream().filter(o -> o.getName().equals(newValue.getName())).findFirst();
        if (directoryObject.isPresent()) {
          DirectoryObject object = directoryObject.get();
          if (object instanceof ClientMetaData) { // we need to get a full client-profile
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

  /**
   * Set form-values to profile
   * @param profilePanelPresenter ProfilePanelPresenter contains ItemGroupPanels with form components
   * @param profile Profile to set the values
   */
  public void saveValues(ProfilePanelPresenter profilePanelPresenter, Profile profile) {

    LOGGER.info("Save values for profile: " + profile);
    profilePanelPresenter.getItemGroupPanels().forEach(itemGroupPanel -> {

          // write values back from bean to profile
          itemGroupPanel.propertyComponents().stream()
              .map(propertyComponent -> (OtcProperty) propertyComponent.getBinder().getBean())
              .collect(Collectors.toList())
              .forEach(otcProperty -> {
                boolean isPasswordProperty = otcProperty instanceof OtcPasswordProperty;
                ItemConfiguration bean = otcProperty.getConfiguration();
                String propertyKey = otcProperty.getKey();
                String org;
                if (propertyKey.equals("name")) org = profile.getName();
                else if (propertyKey.equals("description")) org = profile.getDescription();
                else if (propertyKey.equals("type") && profile.getRealm() != null) org = profile.getSchema(profile.getRealm()).getName();
                else org = profile.getValue(propertyKey);
                String current = bean.getValue() == null || bean.getValue().length() == 0 ? null : bean.getValue();
                if (!StringUtils.equals(org, current)) {
                  if (current != null) {
                    LOGGER.info(" Apply value for " + propertyKey + "=" + (isPasswordProperty ? "***" : org) + " with new value '" + (isPasswordProperty ? "***" : current) + "'");
                    switch (propertyKey) {
                      case "name":
                        profile.setName(current);
                        break;
                      case "description":
                        profile.setDescription(current);
                        break;
                      // handle type-change is working, but disabled at UI
                      case "type": {
                        profile.setSchema(getSchema(current));
                        // remove old schema values
                        Schema orgSchema = getSchema(otcProperty.getInitialValue());
                        if (orgSchema != null) {
                          orgSchema.getChildren().forEach(o -> profile.removeValue(o.getName()));
                        }
                        break;
                      }
                      default:
                        profile.setValue(propertyKey, current);
                        break;
                    }
                  } else {
                    if (propertyKey.equals("description")) {
                      LOGGER.info(" Apply null value for description");
                      profile.setDescription(null);
                    } else {
                      LOGGER.info(" Remove empty value for " + propertyKey);
                      profile.removeValue(propertyKey);
                    }
                  }
                } else {
                  LOGGER.info(" Unchanged " + propertyKey + "=" + org);
                }
              });
    });

    // save
    boolean success = saveProfile(profile, profilePanelPresenter);
    // update view
    if (success) {
      selectItem(profile);
      navigateTo(profile);
    }
  }

  public abstract void selectItem(DirectoryObject directoryObject);

  /**
   * Save profile, return success status
   * @param profile Profile
   * @param panelPresenter
   * @return true if save action completed sucessfully
   */
  public boolean saveProfile(DirectoryObject profile, DirectoryObjectPanelPresenter panelPresenter) {
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

  public void showProfileMetadata(Profile profile) {
    ProfilePanel panel = createProfileMetadataPanel(profile);
    showProfileMetadataPanel(panel);
  }

  public void showProfileMetadataPanel(ProfilePanel panel) {
    actionRow.setVisible(false);
    overviewCL.setVisible(false);
    clientReferencesCL.setVisible(false);
    clientCL.setVisible(true);

    clientSettingsVL.removeAllComponents();
    clientSettingsVL.addComponent(panel);
  }

  /**
   * Creates a ProfilePanel for metadata of new Profile with Save-Handling
   * @param profile the new profile
   * @return ProfilePanel
   */
  protected ProfilePanel createProfileMetadataPanel(Profile profile) {

    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_PROFILE_HEADER), profile.getClass());

    OtcPropertyGroup group = createOtcMetaDataPropertyGroup(profile);

    // show metadata properties, default is hidden
    ProfilePanelPresenter ppp = new ProfilePanelPresenter(this, profilePanel, profile);
    ppp.expandMetaData();
    ppp.hideCopyButton();
    ppp.hideDeleteButton();

    // put property-group to panel
    ppp.setItemGroups(Arrays.asList(group, new OtcPropertyGroup(null, null)));
    ppp.onValuesWritten(profilePanel1 -> saveValues(ppp, profile));

    return profilePanel;
  }

  public OtcPropertyGroup createOtcMetaDataPropertyGroup(Profile profile) {

    OtcPropertyGroup group = builder.createProfileMetaDataGroup(getSchemaNames(), profile);
    // add custom validator to 'name'-property if name is empty - this object must be new
    if (profile.getName() == null || profile.getName().length() == 0) {
      addProfileNameAlreadyExistsValidator(group);
    }
    // profile-type selector is disabled by default: enable it
    group.getProperty("type").ifPresent(otcProperty -> {
      otcProperty.getConfiguration().setRequired(true);
      otcProperty.getConfiguration().enable();
    });

    return group;
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

  /**
   * Creates a list with HorizontalLayout component which contains only a description label of profile
   * @param directoryObject DirectoryObject
   * @return List<Component>
   */
  protected List<Component> createDefaultMetaInformationComponents(DirectoryObject directoryObject) {
    List<Component> information = new ArrayList<>();
    HorizontalLayout desc = new HorizontalLayout();
    desc.addComponent(new Label(directoryObject.getDescription()));
    information.add(desc);
    return information;
  }

  // Overview panel setup
  public ProfilesListOverviewPanelPresenter createOverviewItemlistPanel(ConsoleWebMessages i18nTitleKey, Set items) {

    ProfilesListOverviewPanel plop = new ProfilesListOverviewPanel(i18nTitleKey);
    ProfilesListOverviewPanelPresenter plopPresenter = new ProfilesListOverviewPanelPresenter(this, plop, new LdifExporterService(realmService.getDefaultRealm().getConnectionDescriptor()));

    ListDataProvider<DirectoryObject> dataProvider = DataProvider.ofCollection(items);
    dataProvider.setSortComparator(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase)::compare);
    plopPresenter.setDataProvider(dataProvider);
    plopPresenter.setVisible(true);

    return plopPresenter;
  }


  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    LOGGER.debug("enter -> source={}, navigator-state=", event.getSource(), event.getNavigator().getState());
    String[] params = Optional.ofNullable(event.getParameters()).orElse("").split("/", 2);
    if (params.length > 0) {
      // handle create action
      if ("create".equals(params[0])) {
        switch (event.getViewName()) {
          case ApplicationView.NAME:
            showProfileMetadata(new Application());
            break;
          case ClientView.NAME:
            showProfileMetadata(new Client());
            break;
          case DeviceView.NAME:
            showProfileMetadata(new Device());
            break;
          case HardwaretypeView.NAME:
            showProfileMetadata(new HardwareType());
            break;
          case LocationView.NAME:
            showProfileMetadata(new Location());
            break;
          case PrinterView.NAME:
            showProfileMetadata(new Printer());
            break;
        }

      // register new client with mac-address
      } else if ("register".equals(params[0])
                && params.length == 2
                && ClientView.NAME.equals(event.getViewName())) {
        Client client = new Client();
        client.setMacAddress(params[1]);
        showProfileMetadata(client);

      // view-profile action
      } else if("edit".equals(params[0])
                && params.length == 2
                && params[1].length() > 0) {
        DirectoryObject profile = getFreshProfile(params[1]);
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
  public void showOverview() {
    try {
      ProfilesListOverviewPanelPresenter overviewPanelPresenter = createOverviewItemlistPanel(getViewTitleKey(), getAllItems());
      overviewPanelPresenter.setDeleteMandatSupplier(createDeleteMandateFunction());
      displayOverviewPanel(overviewPanelPresenter.getPanel());
    } catch(AllItemsListException e) {
      showError(e);
    }
  }

  /**
   * Overwritten by Hardware- and location type to prevent deletion of dependent items
   * @return function-supplier to check item-dependencies
   */
  protected Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
    return null;
  }

  public void showProfile(DirectoryObject profile) {
    try {
      ProfilePanel profilePanel = createProfilePanel(profile);
      ProfileReferencesPanel profileReferencesPanel = createReferencesPanel(profile);
      displayProfilePanel(profilePanel, profileReferencesPanel);
    } catch (BuildProfileException e) {
      showError(e);
    }
  }

  public void displayOverviewPanel(ProfilesListOverviewPanel overviewPanel) {
    clientSettingsVL.setVisible(false);
    clientReferencesCL.setVisible(false);
    clientCL.setVisible(false);

    actionRow.setVisible(true);
    overviewCL.removeAllComponents();
    overviewCL.addComponent(overviewPanel);
    overviewCL.setVisible(true);
  }

  /** Display the settings and references of profile, remove actions-panes */
  public void displayProfilePanel(ProfilePanel profilePanel, ProfileReferencesPanel profileReferencesPanel) {
    actionRow.setVisible(false);
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
