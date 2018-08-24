package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
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
import org.openthinclient.web.ui.OtcView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CLIENT_HEADER;

public abstract class ThinclientView extends OtcView {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThinclientView.class);

  private IMessageConveyor mc;
   private VerticalLayout right;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   private Grid<Profile> itemGrid;

  public ThinclientView(ConsoleWebMessages i18nTitleKey, EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
    super(i18nTitleKey, eventBus, notificationService);
    mc = new MessageConveyor(UI.getCurrent().getLocale());

   }

   public void init() {

     HorizontalSplitPanel main = new HorizontalSplitPanel();
     main.addStyleNames("thinclients");
     main.setSizeFull();
     main.setSplitPosition(250, Unit.PIXELS);

     // left selection grid
     VerticalLayout left = new VerticalLayout();
     left.setMargin(new MarginInfo(false, false, false, false));
     left.addStyleName("profileItemSelectionBar");
     left.setHeight(100, Unit.PERCENTAGE);
     main.setFirstComponent(left);

     TextField filter = new TextField();
     filter.addStyleNames("profileItemFilter");
     filter.setPlaceholder("Filter");
//     filter.setIcon(VaadinIcons.FILTER);
//     filter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
     left.addComponent(filter);

     itemGrid = new Grid<>();
     itemGrid.addStyleNames("profileSelectionGrid");
     itemGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
     itemGrid.addColumn(Profile::getName);
     itemGrid.addSelectionListener(selectionEvent -> showContent(selectionEvent.getFirstSelectedItem()));
     itemGrid.setSizeFull();
     itemGrid.removeHeaderRow(0);
     // Profile-Type based colors
     // itemGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());
     left.addComponent(itemGrid);
     left.setExpandRatio(itemGrid, 1);

     // right main content
     right = new VerticalLayout();
     right.setMargin(new MarginInfo(false, false, false, false));
     main.setSecondComponent(right);
     showContent(Optional.empty());

     Responsive.makeResponsive(main);

     setPanelContent(main);
   }

   public void setItems(HashSet items) {
     itemGrid.setDataProvider(DataProvider.ofCollection(items));
   }

   public abstract ProfilePanel createProfilePanel(Profile item);

   private void showContent(Optional<Profile> selectedItems) {

     if (selectedItems.isPresent()) {

       right.removeAllComponents();

       Profile profile = getFreshProfile(selectedItems.get());
       ProfilePanel profilePanel = createProfilePanel(profile);
       right.addComponent(profilePanel);

     } else {
       right.removeAllComponents();
       Label emptyScreenHint = new Label(
                  VaadinIcons.SELECT.getHtml() + "&nbsp;&nbsp;&nbsp;" + mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SELECT) + "<br><br>" +
                       VaadinIcons.FILTER.getHtml() +  "&nbsp;&nbsp;&nbsp;" +  mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_FILTER),
               ContentMode.HTML);
       emptyScreenHint.setStyleName("emptyScreenHint");
       right.addComponent(emptyScreenHint);
     }

  }

  public void showError(BuildProfileException e) {
    right.removeAllComponents();
    Label emptyScreenHint = new Label(
        VaadinIcons.WARNING.getHtml() + "&nbsp;&nbsp;&nbsp;" + mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ERROR) + e.getMessage(),
        ContentMode.HTML);
    emptyScreenHint.setStyleName("errorScreenHint");
    right.addComponent(emptyScreenHint);
  }

  public abstract <T extends Profile> T getFreshProfile(T profile);

  // show device associations
  public void showDeviceAssociations(Set<Device> all, AssociatedObjectsProvider profile, ProfilePanel profilePanel, Set<? extends DirectoryObject> members) {
    List<Item> allDevices = builder.createItems(all);
    List<Item> deviceMembers = builder.createFilteredItemsFromDO(members, Device.class);
    ReferenceComponentPresenter presenter = profilePanel.addReferences(mc.getMessage(ConsoleWebMessages.UI_ASSOCIATED_DEVICES_HEADER),
                                                                       mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION),
                                                                       allDevices, deviceMembers);
    presenter.setProfileReferenceChangedConsumer(values -> saveAssociations(profile, values, all, Device.class));
  }

  // show references
  public void showReference(Profile profile, ProfilePanel profilePanel, Set<? extends DirectoryObject> members,
                             String title, Set<? extends DirectoryObject> allObjects, Class clazz) {
    List<Item> memberItems = builder.createFilteredItemsFromDO(members, clazz);
    ReferenceComponentPresenter presenter = profilePanel.addReferences(title, mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ASSOCIATION), builder.createItems(allObjects), memberItems);
    presenter.setProfileReferenceChangedConsumer(values -> saveReference(profile, values, allObjects, clazz));
  }

  /**
   *
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
   *
   * @param profile to be changed
   * @param values the state of value to be saved
   * @param clazz subset of member-types which has been modified
   */
  private <T extends DirectoryObject> void saveReference(Profile profile, List<Item> values, Set<T> profileAndDirectoryObjects, Class<T> clazz) {

    Set<T> members;
    if (profile instanceof Application) {
      members = (Set<T>) ((Application) profile).getMembers();

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
      if (!oldValues.contains(newValue)) {
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

  public abstract void save(Profile profile) throws Exception;

  public void saveProfile(Profile profile, ItemGroupPanel panel) {
    try {
      save(profile);

      LOGGER.info("Profile saved {}", profile);
      if (panel != null) {
        panel.setInfo(mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SAVE_SUCCESS));
      }
    } catch (Exception e) {
      LOGGER.error("Cannot save profile", e);
      if (panel != null) {
        panel.setError(mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_SAVE_ERROR) + e.getMessage());
      }
    }
  }

  public void saveValues(ItemGroupPanel itemGroupPanel, Profile profile) {

    LOGGER.info("Save profile: " + profile);

    // write values back from bean to profile
    itemGroupPanel.propertyComponents().stream()
            .map(propertyComponent -> (OtcProperty) propertyComponent.getBinder().getBean())
            .collect(Collectors.toList())
            .forEach(otcProperty -> {
              ItemConfiguration bean = otcProperty.getConfiguration();
              String org = profile.getValue(bean.getKey());
              String current = bean.getValue();
              if (current != null && !StringUtils.equals(org, current)) {
                LOGGER.info("Apply value for " + bean.getKey() + "=" + org + " with new value '" + current + "'");
                profile.setValue(bean.getKey(), bean.getValue());
              } else {
                LOGGER.info("Unchanged " + bean.getKey() + "=" + org);
              }
    });

    saveProfile(profile, itemGroupPanel);

  }

}
