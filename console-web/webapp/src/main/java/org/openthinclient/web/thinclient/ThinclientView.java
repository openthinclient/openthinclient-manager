package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.dashboard.DashboardView;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencesComponentPresenter;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public abstract class ThinclientView extends Panel implements View {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  public static final ThemeResource PACKAGES = new ThemeResource("icon/packages.svg");
  public static final ThemeResource DEVICE   = new ThemeResource("icon/display.svg");
  public static final ThemeResource LOCATION = new ThemeResource("icon/place.svg");
  public static final ThemeResource HARDWARE = new ThemeResource("icon/drive.svg");
  public static final ThemeResource USER     = new ThemeResource("icon/user.svg");
  public static final ThemeResource PRINTER  = new ThemeResource("icon/printer.svg");
  public static final ThemeResource CLIENT   = new ThemeResource("icon/logo.svg");
  public static final ThemeResource APPLICATIONGROUP = new ThemeResource("icon/applicationgroup.svg");

  private IMessageConveyor mc;
  private VerticalLayout right;
  private final HorizontalLayout actionRow;
  protected ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();
//  private HorizontalSplitPanel main;

//  private Grid<DirectoryObject> itemGrid;
  private Label filterStatus;

  public ThinclientView(ConsoleWebMessages i18nTitleKey, EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     mc = new MessageConveyor(UI.getCurrent().getLocale());
     eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(i18nTitleKey)));

     setStyleName("thinclientview");

//     main = new HorizontalSplitPanel();
//     main.addStyleNames("thinclients");
//     main.setSplitPosition(250, Unit.PIXELS);
//     main.setSizeFull();

     // left selection grid
//     VerticalLayout left = new VerticalLayout();
//     left.setSpacing(false);
//     left.setMargin(new MarginInfo(false, false, false, false));
//     left.addStyleName("profileItemSelectionBar");
//     main.setFirstComponent(left);
//
//
//     TextField filter = new TextField();
//     filter.addStyleNames("profileItemFilter");
//     filter.setPlaceholder("Filter");
//     filter.setIcon(VaadinIcons.FILTER);
//     filter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
//     filter.addValueChangeListener(this::onFilterTextChange);
//     left.addComponent(filter);
//     filterStatus = new Label();
//     filterStatus.addStyleName("profileItemFilterStatus");
//     left.addComponent(filterStatus);

//     itemGrid = new Grid<>();
//     itemGrid.addStyleNames("profileSelectionGrid");
//     itemGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
//     itemGrid.addColumn(DirectoryObject::getName);
//     itemGrid.addSelectionListener(selectionEvent -> showContent(selectionEvent.getFirstSelectedItem()));
//     itemGrid.removeHeaderRow(0);
//     itemGrid.setSizeFull();
//     itemGrid.setHeightMode(com.vaadin.shared.ui.grid.HeightMode.UNDEFINED);
//     // Profile-Type based style
//     itemGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());
//     left.addComponent(itemGrid);

     // no effect:
//     left.setExpandRatio(filter, 0.1f);
//     left.setExpandRatio(filterStatus, 0.1f);
//     left.setExpandRatio(itemGrid, 4);

     // right main content
     CssLayout view = new CssLayout();
     view.setStyleName("responsive");
     view.setResponsive(true);
//     main.setSecondComponent(view);

     // action row
     actionRow = new HorizontalLayout();
     view.addComponent(actionRow);
     Responsive.makeResponsive(actionRow);

     // thinclient settings
     right = new VerticalLayout();
     right.setMargin(new MarginInfo(false, false, false, false));
     right.setSpacing(false);

     view.addComponents(actionRow, right);

//     showContent(Optional.empty());

//     setContent(main);
     setContent(view);
  }

  public abstract ProfilePanel createProfilePanel(DirectoryObject item) throws BuildProfileException;

  public abstract HashSet getAllItems() throws AllItemsListException;

  public abstract Schema getSchema(String value);

  public abstract String[] getSchemaNames();

  public abstract String getViewName();

  public abstract <T extends DirectoryObject> T getFreshProfile(String profileName);

  public abstract void save(DirectoryObject profile) throws ProfileNotSavedException;

    /**
     * Display action panel with given label, icon and click-handler
     * @param title
     * @param theme
     * @param listener
     */
  public void addActionPanel(String title, ThemeResource theme, Button.ClickListener listener) {

    Panel panel = new Panel();
    panel.addStyleName("thinclient-action-panel");

    VerticalLayout panelContent = new VerticalLayout();
    panelContent.setSpacing(false);
    panelContent.setMargin(false);
    Button action = new Button();
    action.setIcon(theme);
    action.addStyleName("thinclient-action-panel-icon");
    action.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    action.addClickListener(listener);
    panelContent.addComponent(action);
    Label titleLabel = new Label(title);
    titleLabel.setStyleName("header-title");
    panelContent.addComponent(titleLabel);
    panel.setContent(panelContent);

    actionRow.addComponent(panel);
  }

//  public void setItems(HashSet items) {
//     List groupedItems = ProfilePropertiesBuilder.createGroupedItems(items);
//     long groupHeader = groupedItems.stream().filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
//     ListDataProvider dataProvider = DataProvider.ofCollection(groupedItems);
//     itemGrid.setDataProvider(dataProvider);
//     filterStatus.setCaption((dataProvider.getItems().size() - groupHeader) + "/" + items.size());
//  }

  // TODO: GridItem-Filter
//  private void onFilterTextChange(HasValue.ValueChangeEvent<String> event) {
//    ListDataProvider<DirectoryObject> dataProvider = (ListDataProvider<DirectoryObject>) itemGrid.getDataProvider();
//    long groupHeader = dataProvider.getItems().stream().filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
//    dataProvider.setFilter(directoryObject -> {
//      if (directoryObject instanceof ProfilePropertiesBuilder.MenuGroupProfile) {
//        return true;
//      } else {
//        return caseInsensitiveContains(directoryObject.getName(), event.getValue());
//      }
//    });
//    long filteredGroupHeader = dataProvider.fetch(new Query<>()).filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
//    filterStatus.setCaption((dataProvider.size(new Query<>())-filteredGroupHeader) + "/" + (dataProvider.getItems().size()-groupHeader));
//  }

  private Boolean caseInsensitiveContains(String where, String what) {
    return where.toLowerCase().contains(what.toLowerCase());
  }

//  public void selectItem(DirectoryObject item) {
//    itemGrid.select(item);
//  }
//
//  public DirectoryObject getSelectedItem() {
//    return itemGrid.getSelectedItems().iterator().next();
//  }

//  private void showContent(Optional<DirectoryObject> selectedItems) {
//
//    //  do nothing
//    if (selectedItems.isPresent() && selectedItems.get() instanceof ProfilePropertiesBuilder.MenuGroupProfile) {
//      return;
//    }
//
//    // navigate to item
//    if (selectedItems.isPresent()) {
//      Navigator navigator = UI.getCurrent().getNavigator();
//      navigator.navigateTo( getViewName() + "/" + selectedItems.get().getName());
//    }

//    right.removeAllComponents();
//
//    if (selectedItems.isPresent()) {
//     DirectoryObject directoryObject = getFreshProfile(selectedItems.get().getName());
//      try {
//        ProfilePanel profilePanel = createProfilePanel(directoryObject);
//        right.addComponent(profilePanel);
//      } catch (BuildProfileException e) {
//        showError(e);
//      }
//    } else {
//     Label emptyScreenHint = new Label(
//                VaadinIcons.SELECT.getHtml() + "&nbsp;&nbsp;&nbsp;" + mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SELECT) + "<br><br>" +
//                     VaadinIcons.FILTER.getHtml() +  "&nbsp;&nbsp;&nbsp;" +  mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_FILTER),
//                 ContentMode.HTML);
//     emptyScreenHint.setStyleName("emptyScreenHint");
//     right.addComponent(emptyScreenHint);
//    }
//  }

  public void showError(Exception e) {
    right.removeAllComponents();
    Label emptyScreenHint = new Label(
        VaadinIcons.WARNING.getHtml() + "&nbsp;&nbsp;&nbsp;" + mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ERROR) + e.getMessage(),
        ContentMode.HTML);
    emptyScreenHint.setStyleName("errorScreenHint");
    right.addComponent(emptyScreenHint);
  }

  // show device associations
  public void showDeviceAssociations(Set<Device> all, AssociatedObjectsProvider profile, ProfilePanel profilePanel, Set<? extends DirectoryObject> members) {
    List<Item> allDevices = builder.createItems(all);
    List<Item> deviceMembers = builder.createFilteredItemsFromDO(members, Device.class);
    ReferencesComponentPresenter presenter = profilePanel.addReferences(mc.getMessage(ConsoleWebMessages.UI_ASSOCIATED_DEVICES_HEADER),
                                                                       mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION),
                                                                       allDevices, deviceMembers, false);
    presenter.setProfileReferenceChangedConsumer(values -> saveAssociations(profile, values, all, Device.class));
  }

  /**
   * default method to show references and handle changes
   * @param profilePanel - ProfilePanel where this references will be added
   * @param members - DirectoryObject which will be shown as Buttons
   * @param title - Title of reference line
   * @param allObjects - all available DirectoryObjects of a type, this item can be selected in single- or multi-selection-box
   * @param clazz - Class of DirectoryObjects
   */
  public void showReference(DirectoryObject profile, ProfilePanel profilePanel, Set<? extends DirectoryObject> members,
                            String title, Set<? extends DirectoryObject> allObjects, Class clazz) {
    showReference(profilePanel, members, title, allObjects, clazz, values -> saveReference(profile, values, allObjects, clazz),null, false);
  }

  /**
   * show references and handle changes
   * @param profilePanel - ProfilePanel where this references will be added
   * @param members - DirectoryObject which will be shown as Buttons
   * @param title - Title of reference line
   * @param allObjects - all available DirectoryObjects of a type, this item can be selected in single- or multi-selection-box
   * @param clazz - Class of DirectoryObjects
   * @param profileReferenceChangeConsumer - consumer to call after changing a reference, i.e. 'save'-action
   * @param memberSupplier - supplier for members of given Item
   * @param isReadOnly - display items in readonly mode
   */
  public void showReference(ProfilePanel profilePanel,
                            Set<? extends DirectoryObject> members,
                            String title,
                            Set<? extends DirectoryObject> allObjects,
                            Class clazz,
                            Consumer<List<Item>> profileReferenceChangeConsumer,
                            Function<Item, List<Item>> memberSupplier,
                            boolean isReadOnly) {

    List<Item> memberItems = builder.createFilteredItemsFromDO(members, clazz);
    ReferencesComponentPresenter presenter = profilePanel.addReferences(title, mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION), builder.createItems(allObjects), memberItems, isReadOnly);
    presenter.setProfileReferenceChangedConsumer(profileReferenceChangeConsumer);
    presenter.showSublineContent(memberSupplier);
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
  protected <T extends DirectoryObject> void saveReference(DirectoryObject profile, List<Item> values, Set<T> profileAndDirectoryObjects, Class<T> clazz) {

    Set<T> members;
    if (profile instanceof Application) {
      members = (Set<T>) ((Application) profile).getMembers();

    } else if (profile instanceof ApplicationGroup) {
      members = (Set<T>) ((ApplicationGroup) profile).getMembers();

    } else if (profile instanceof Printer) {
      members = (Set<T>) ((Printer) profile).getMembers();

    } else if (profile instanceof HardwareType) {
      // TODO: nur ThinclientGruppen werden vom LDAP als 'members' behandelt, Thinclients werden ignoriert
      Set<? extends DirectoryObject> clients = ((HardwareType) profile).getMembers();
      clients.stream().forEach(o -> {
        LOGGER.info("This class should be of Type Client.class: {}" + ((DirectoryObject) o).getClass());
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
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.stream().filter(o -> o.getName().equals(oldItem.getName())).findFirst();
        if (directoryObject.isPresent()) {
          members.remove(directoryObject.get());
        } else {
          LOGGER.info("DirectoryObject (to remove) not found for " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
      if (newValue != null && !oldValues.contains(newValue)) {
        LOGGER.info("Add newValue to members: " + newValue);
        // get values from available-values set and add to members
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.stream().filter(o -> o.getName().equals(newValue.getName())).findFirst();
        if (directoryObject.isPresent()) {
          T dirObj = (T) directoryObject.get();
          members.add(dirObj);
        } else {
          LOGGER.info("DirectoryObject not found for " + newValue);
        }
      }
    });

    saveProfile(profile, null);
  }


    /**
     * Save profile, return success status
     * @param profile Profile
     * @param panel ItemGroupPanel
     * @return true if save action completed sucessfully
     */
  public boolean saveProfile(DirectoryObject profile, ItemGroupPanel panel) {
    try {
      save(profile);
      LOGGER.info("Profile saved {}", profile);
      if (panel != null) {
        // TODO set success message
//        panel.setInfo(mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SAVE_SUCCESS));
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Cannot save profile", e);
      if (panel != null) {
        // TODO set success message
//        panel.setError(mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SAVE_ERROR) + e.getMessage());
      }
      return false;
    }
  }

  /**
   * Set form-values to profile
   * @param itemGroupPanel ItemGroupPanel contains form components
   * @param profile Profile to set the values
   */
  public void saveValues(ItemGroupPanel itemGroupPanel, Profile profile) {

    LOGGER.info("Save values for profile: " + profile);

    // write values back from bean to profile
    itemGroupPanel.propertyComponents().stream()
            .map(propertyComponent -> (OtcProperty) propertyComponent.getBinder().getBean())
            .collect(Collectors.toList())
            .forEach(otcProperty -> {
              ItemConfiguration bean = otcProperty.getConfiguration();
              String propertyKey = otcProperty.getKey();
              String org = profile.getValue(propertyKey);
              String current = bean.getValue() == null || bean.getValue().length() == 0 ? null : bean.getValue();
              if (!StringUtils.equals(org, current)) {
                if (current != null) {
                  LOGGER.info(" Apply value for " + propertyKey + "=" + org + " with new value '" + current + "'");
                  switch (propertyKey) {
                    case "name": profile.setName(current); break;
                    case "description": profile.setDescription(current); break;
                    // handle type-change is working, but disabled at UI
                    case "type": {
                      profile.setSchema(getSchema(current));
                      profile.getProperties().setName("profile");
                      profile.getProperties().setDescription(current);
                      // remove old schema values
                      Schema orgSchema = getSchema(otcProperty.getInitialValue());
                      orgSchema.getChildren().forEach(o -> {
                        profile.removeValue(o.getName());
                      });
                      break;
                    }
                    default: profile.setValue(propertyKey, current); break;
                  }
                } else {
                  LOGGER.info(" Remove empty value for " + propertyKey);
                  profile.removeValue(propertyKey);
                }
              } else {
                LOGGER.info(" Unchanged " + propertyKey + "=" + org);
              }
    });

    // save
    boolean success = saveProfile(profile, itemGroupPanel);
    // update view
    if (success) {
      // TODO: refresh itemGrid after change/save
//      try {
//        // setItems(getAllItems()); // refresh item list
//        selectItem(profile);
//      } catch (AllItemsListException e) {
//        showError(e);
//      }
    }
  }

  public void showProfileMetadata(Profile profile) {
    ProfilePanel panel = createProfileMetadataPanel(profile);
    showProfileMetadataPanel(panel);
  }

  public void showProfileMetadataPanel(ProfilePanel panel) {
    right.removeAllComponents();
    right.addComponent(panel);
  }

  /**
   * Creates a ProfilePanel for metadata of new Profile with Save-Handling
   * @param profile the new profile
   * @return ProfilePanel
   */
  protected ProfilePanel createProfileMetadataPanel(Profile profile) {

    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_PROFILE_HEADER), profile.getClass());
//    profilePanel.hideMetaInformation();

    OtcPropertyGroup group = createOtcMetaDataPropertyGroup(profile);

    // put property-group to panel
    profilePanel.setItemGroups(Arrays.asList(group, new OtcPropertyGroup(null, null)));
    // show metadata properties, default is hidden
    ProfilePanelPresenter ppp = new ProfilePanelPresenter(this, profilePanel, profile);
    ppp.expandMetaData();
    ppp.hideCopyButton();
//    ppp.hideEditButton();
    ppp.hideDeleteButton();
    return profilePanel;
  }

  public OtcPropertyGroup createOtcMetaDataPropertyGroup(Profile profile) {
    OtcPropertyGroup group = builder.createProfileMetaDataGroup(getSchemaNames(), profile);
    // add custom validator to 'name'-property if name is empty - this object must be new
    if (profile.getName() == null || profile.getName().length() == 0) {
      group.getProperty("name").ifPresent(nameProperty -> {
        nameProperty.getConfiguration().getValidators().add(new AbstractValidator<String>(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS)) {
          @Override
          public ValidationResult apply(String value, ValueContext context) {
            DirectoryObject directoryObject = getFreshProfile(value);
            return directoryObject == null ? ValidationResult.ok() : ValidationResult.error(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS));
          }
        });
      });
    }
    // profile-type selector is disabled by default: enable it
    group.getProperty("type").ifPresent(otcProperty -> {
      otcProperty.getConfiguration().setRequired(true);
      otcProperty.getConfiguration().enable();
    });

    // attach save-action
    group.setValueWrittenHandlerToAll(ipg -> {
      // get manually property values
      ipg.getPropertyComponent("type").ifPresent(pc -> {
        OtcOptionProperty bean = (OtcOptionProperty) pc.getBinder().getBean();
        profile.setSchema(getSchema(bean.getValue()));
        profile.getProperties().setName("profile");
        profile.getProperties().setDescription(bean.getValue());
      });
      ipg.getPropertyComponent("name").ifPresent(pc -> {
        OtcTextProperty bean = (OtcTextProperty) pc.getBinder().getBean();
        profile.setName(bean.getValue());
      });
      ipg.getPropertyComponent("description").ifPresent(pc -> {
        OtcTextProperty bean = (OtcTextProperty) pc.getBinder().getBean();
        profile.setDescription(bean.getValue());
      });

      // save
      boolean success = saveProfile(profile, ipg);
      // update view
      if (success) {
        // TODO: refresh itemGrid after change/save
//        try {
//          // setItems(getAllItems()); // refresh item list
//          selectItem(profile);
//        } catch (AllItemsListException e) {
//          showError(e);
//        }
      }

    });
    return group;
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

  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreateApplicationAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_APPLICATION_LABEL), ThinclientView.PACKAGES, e -> UI.getCurrent().getNavigator().navigateTo(ApplicationView.NAME + "/create"));
  }
  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreateApplicationGroupAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_GROUP_LABEL), ThinclientView.APPLICATIONGROUP, e -> UI.getCurrent().getNavigator().navigateTo(ApplicationGroupView.NAME + "/create"));
  }
  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreateClientAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_CLIENT_LABEL), ThinclientView.CLIENT, e -> UI.getCurrent().getNavigator().navigateTo(ClientView.NAME + "/create"));
  }
  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreateDeviceAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_DEVICE_LABEL), ThinclientView.DEVICE, e -> UI.getCurrent().getNavigator().navigateTo(DeviceView.NAME + "/create"));
  }
  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreateHardwareTypeAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_HWTYPE_LABEL), ThinclientView.HARDWARE, e -> UI.getCurrent().getNavigator().navigateTo(HardwaretypeView.NAME + "/create"));
  }
  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreateLocationAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_LOCATION_LABEL), ThinclientView.LOCATION, e -> UI.getCurrent().getNavigator().navigateTo(LocationView.NAME + "/create"));
  }
  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreatePrinterAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_PRINTER_LABEL), ThinclientView.PRINTER, e -> UI.getCurrent().getNavigator().navigateTo(PrinterView.NAME + "/create"));
  }
  /**
   * Shortcut for adding this actionPanel to view
   */
  protected void showCreateUserAction() {
    addActionPanel(mc.getMessage(UI_THINCLIENT_ADD_USER_LABEL), ThinclientView.USER, e -> UI.getCurrent().getNavigator().navigateTo(UserView.NAME + "/create"));
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    if (event.getParameters() != null) {
      // split at "/", add each part as a label
      String[] params = event.getParameters().split("/");

      // handle create action
      if (params.length == 1 && params[0].equals("create")) {
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
      } else if (event.getViewName().equals(ClientView.NAME) && params.length == 2 && params[0].equals("register")) {
        Client client = new Client();
        client.setMacAddress(params[1]);
        showProfileMetadata(client);

        // view-profile action
      } else if (params.length == 1 && params[0].length() > 0) {
        DirectoryObject profile = getFreshProfile(params[0]);
        if (profile != null) {
//          selectItem(profile);
          try {
            ProfilePanel profilePanel = createProfilePanel(profile);
            // TODO: Übersichtsseite und Details sollten verschiedenen Views sein - oder?
            actionRow.setVisible(false);
            right.removeAllComponents();
            right.addComponent(profilePanel);
          } catch (BuildProfileException e) {
            e.printStackTrace();
          }
        } else {
          LOGGER.info("No profile found for name '" + params[0] + "'.");
        }
      }

    }
  }

}
