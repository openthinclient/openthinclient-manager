package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationGroupService;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ApplicationGroupView.NAME)
// @SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_APPLICATION_GROUP_HEADER", order = 91)
@ThemeIcon("icon/applicationgroup-white.svg")
public final class ApplicationGroupView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationGroupView.class);

  public static final String NAME = "applicationgroup_view";

  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;

   private final IMessageConveyor mc;

   public ApplicationGroupView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_APPLICATIONGROUP_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateApplicationGroupAction();
   }

   @PostConstruct
   private void setup() {
     // // setItems(getAllItems());
   }

  @Override
  public HashSet getAllItems() {
    return (HashSet) applicationGroupService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(ApplicationGroup.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(ApplicationGroup.class);
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) {

    ProfilePanel profilePanel = new ProfilePanel(directoryObject.getName(), directoryObject.getClass());
    OtcPropertyGroup configuration = createUserMetadataPropertyGroup((ApplicationGroup) directoryObject);
    addProfileNameAlreadyExistsValidator(configuration);

    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, directoryObject);
    // put property-group to panel
    ppp.setItemGroups(Arrays.asList(configuration, new OtcPropertyGroup(null, null)));
    // set MetaInformation
//    ppp.setPanelMetaInformation(createDefaultMetaInformationComponents(directoryObject));
    // Save handler, for each property we need to call dedicated setter
    ppp.onValuesWritten(profilePanel1 -> saveProfile(directoryObject, ppp));
//    ppp.onValuesWritten(profilePanel1 -> {
//
//      ppp.propertyComponents().forEach(propertyComponent -> {
//        OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
//        String key   = bean.getKey();
//        String value = bean.getConfiguration().getValue();
//        switch (key) {
//          case "name": directoryObject.setName(value); break;
//          case "description": directoryObject.setDescription(value); break;
//        }
//      });
//
//      // save
//      boolean success = saveProfile(directoryObject, ppp);
//      // TODO: update view after save
////      if (success) {
////       setItems(getAllItems()); // refresh item list
////        selectItem(applicationGroup);
////      }
//
//    });

    ApplicationGroup applicationGroup = (ApplicationGroup) directoryObject;
    showReference(profilePanel, applicationGroup.getApplications(), mc.getMessage(UI_APPLICATION_HEADER),
                  applicationService.findAll(), Application.class,
                  values -> saveApplicationGroupReference(applicationGroup, values), null, false);

    // sub-groups disabled MANGER-358
    //    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    //    allApplicationGroups.remove(applicationGroup); // do not allow to add this applicationGroup to this applicationGroup
    //    showReference(profilePanel, applicationGroup.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER),
    //        allApplicationGroups, ApplicationGroup.class,
    //        values -> saveApplicationGroup2GroupReference(applicationGroup, values),
    //        getApplicationsForApplicationGroupFunction(applicationGroup), false
    //    );

    return profilePanel;
  }

  /**
   * Save application-group assignments (off applications) at application-directory-object (NOT at application-group-object)
   * @param applicationGroup
   * @param values
   */
  private void saveApplicationGroupReference(ApplicationGroup applicationGroup, List<Item> values) {

    List<Application> oldValues = applicationService.findAll().stream().filter(application -> application.getMembers().contains(applicationGroup)).collect(Collectors.toList());
    LOGGER.debug(applicationGroup.getName() + " applications: {}", oldValues);

    oldValues.forEach(oldItem -> {
      if (values.stream().anyMatch(a -> a.getName().equals(oldItem.getName()))) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from applicationGroup: " + oldItem);
        if (oldItem.getMembers().contains(applicationGroup)) {
          oldItem.getMembers().remove(applicationGroup);
          applicationService.save(oldItem);
        } else {
          LOGGER.info("ApplicationGroup (to remove) not found in members of " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
        Application application = applicationService.findByName(newValue.getName());
        if (application != null) {
           if (!oldValues.contains(application)) {
              LOGGER.info("Add ApplicationGroup to members of: " + newValue);
              applicationGroup.getApplications().add(application); // mandatory, otherwise it doesn't work
              application.getMembers().add(applicationGroup);
              applicationService.save(application);
          }
        } else {
          LOGGER.info("Application not found for " + newValue);
        }
    });

  }

  /**
   * Save application-group group-assignments (off application-groups) at application-group-object
   * @param applicationGroup
   * @param values
   */
  private void saveApplicationGroup2GroupReference(ApplicationGroup applicationGroup, List<Item> values) {

    ApplicationGroup group = applicationGroupService.findByName(applicationGroup.getName());
    Set<ApplicationGroup> oldValues = group.getApplicationGroups();
    LOGGER.debug(applicationGroup.getName() + " old application-groups: {}", oldValues);

    oldValues.forEach(oldItem -> {
      if (values.stream().anyMatch(a -> a.getName().equals(oldItem.getName()))) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from applicationGroup: " + oldItem);
        if (oldItem.getMembers().contains(applicationGroup)) {
          oldItem.getMembers().remove(applicationGroup);
          applicationGroupService.save(oldItem);
        } else {
          LOGGER.info("ApplicationGroup (to remove) not found in members of " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
      ApplicationGroup applicationGroup1 = applicationGroupService.findByName(newValue.getName());
      if (applicationGroup1 != null) {
        if (!oldValues.contains(applicationGroup1)) {
          LOGGER.info("Add ApplicationGroup to members of: " + newValue);
          applicationGroup.getApplicationGroups().add(applicationGroup1);
          applicationGroup1.getMembers().add(applicationGroup);
          applicationGroupService.save(applicationGroup1);
        }
      } else {
        LOGGER.info("Application not found for " + newValue);
      }
    });

  }

  private OtcPropertyGroup createUserMetadataPropertyGroup(ApplicationGroup applicationGroup) {

    OtcPropertyGroup configuration = new OtcPropertyGroup(null);
//    configuration.setCollapseOnDisplay(false); // false is default
    configuration.setDisplayHeaderLabel(false);

    // Name
    OtcTextProperty name = new OtcTextProperty(mc.getMessage(UI_APPLICATIONGROUP_HEADER), mc.getMessage(UI_APPLICATIONGROUP_TIP), "name", applicationGroup.getName());
    ItemConfiguration nameConfiguration = new ItemConfiguration("name", applicationGroup.getName());
    nameConfiguration.addValidator(new StringLengthValidator(mc.getMessage(UI_PROFILE_NAME_VALIDATOR), 1, 255));
    name.setConfiguration(nameConfiguration);
    configuration.addProperty(name);

    // Description
    OtcTextProperty desc = new OtcTextProperty(mc.getMessage(UI_COMMON_DESCRIPTION_LABEL), null, "description", applicationGroup.getDescription());
    ItemConfiguration descConfig = new ItemConfiguration("description", applicationGroup.getDescription());
    desc.setConfiguration(descConfig);
    configuration.addProperty(desc);

    return configuration;
  }


  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) applicationGroupService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
    LOGGER.info("Save: " + profile);
    applicationGroupService.save((ApplicationGroup) profile);
  }


  /**
   * Supplier for ApplicationGroup Members of given client and supplied item as ApplicationGroup
   * @param applicationGroup Client which has ApplicationGroups
   * @return List of members mapped to Item-list or empty list
   */
  private Function<Item, List<Item>> getApplicationsForApplicationGroupFunction(ApplicationGroup applicationGroup) {
    return item -> {
      Optional<ApplicationGroup> first = applicationGroup.getApplicationGroups().stream().filter(ag -> ag.getName().equals(item.getName())).findFirst();
      if (first.isPresent()) {
        Stream<? extends DirectoryObject> stream = first.get().getApplications().stream()
            .sorted(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase));
        return stream.map(m -> new Item(m.getName(), Item.Type.APPLICATION)).collect(Collectors.toList());
      } else {
        return new ArrayList<>();
      }
    };
  }

  public void showProfileMetadata(ApplicationGroup profile) {

    OtcPropertyGroup propertyGroup = createUserMetadataPropertyGroup(profile);

    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_APPLICATIONGROUP_HEADER), profile.getClass());
//    profilePanel.hideMetaInformation();
    // put property-group to panel
    // show metadata properties, default is hidden
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, profile);
    ppp.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup(null, null)));
    ppp.expandMetaData();
    ppp.hideCopyButton();
//    ppp.hideEditButton();
    ppp.hideDeleteButton();

    showProfileMetadataPanel(profilePanel);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    if (event.getParameters() != null) {
      // split at "/", add each part as a label
      String[] params = event.getParameters().split("/");

      // handle create action
      if (params.length == 1 && params[0].equals("create")) {
        switch (event.getViewName()) {
          case ApplicationGroupView.NAME: showProfileMetadata(new ApplicationGroup()); break;
        }
      }
    }
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }
}
