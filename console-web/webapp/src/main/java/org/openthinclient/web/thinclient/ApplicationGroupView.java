package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ApplicationGroupView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_APPLICATION_GROUP_HEADER", order = 91)
@ThemeIcon("icon/applicationgroup.svg")
public final class ApplicationGroupView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationGroupView.class);

  public static final String NAME = "applicationgroup_view";

  @Autowired
  private PrinterService printerService;
  @Autowired
  private UserService userService;
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private SchemaProvider schemaProvider;

   private final IMessageConveyor mc;

   public ApplicationGroupView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_APPLICATIONGROUP_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateApplicationGroupAction();
   }

   @PostConstruct
   private void setup() {
     setItems(getAllItems());
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

    // put property-group to panel
    profilePanel.setItemGroups(Arrays.asList(configuration, new OtcPropertyGroup(null, null)));
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, directoryObject);
    ppp.hideCopyButton();
    // set MetaInformation
    ppp.setPanelMetaInformation(createDefaultMetaInformationComponents(directoryObject));

    ApplicationGroup applicationGroup = (ApplicationGroup) directoryObject;
    showReference(profilePanel, applicationGroup.getApplications(), mc.getMessage(UI_APPLICATION_HEADER),
                  applicationService.findAll(), Application.class,
                  values -> saveApplicationGroupReference(applicationGroup, values), null);

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

  private OtcPropertyGroup createUserMetadataPropertyGroup(ApplicationGroup applicationGroup) {

    OtcPropertyGroup configuration = new OtcPropertyGroup(null);
    configuration.setCollapseOnDisplay(false);
    configuration.setDisplayHeaderLabel(false);

    // Name
    OtcTextProperty name = new OtcTextProperty(mc.getMessage(UI_APPLICATIONGROUP_HEADER), "Anwendungsname", "name", applicationGroup.getName());
    ItemConfiguration nameConfiguration = new ItemConfiguration("name", applicationGroup.getName());
    nameConfiguration.addValidator(new StringLengthValidator("Name muss mindesten 5 Zeichen lang sein.", 5, 15));
    name.setConfiguration(nameConfiguration);
    configuration.addProperty(name);

    // Description
    OtcTextProperty desc = new OtcTextProperty("Beschreibung", null, "description", applicationGroup.getDescription());
    ItemConfiguration descConfig = new ItemConfiguration("description", applicationGroup.getDescription());
    desc.setConfiguration(descConfig);
    configuration.addProperty(desc);

    // Save handler, for each property we need to call dedicated setter
    configuration.onValueWritten(ipg -> {
      ipg.propertyComponents().forEach(propertyComponent -> {
        OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
        String key   = bean.getKey();
        String value = bean.getConfiguration().getValue();
        switch (key) {
          case "name": applicationGroup.setName(value); break;
          case "description": applicationGroup.setDescription(value); break;
        }
      });

      // save
      boolean success = saveProfile(applicationGroup, ipg);
      // update view
      if (success) {
        setItems(getAllItems()); // refresh item list
        selectItem(applicationGroup);
      }

    });
    return configuration;
  }


  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) applicationGroupService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
    applicationGroupService.save((ApplicationGroup) profile);
  }

  public void showProfileMetadata(ApplicationGroup profile) {

    OtcPropertyGroup propertyGroup = createUserMetadataPropertyGroup(profile);

    ProfilePanel profilePanel = new ProfilePanel("Neue Anwendungsgruppe", profile.getClass());
    profilePanel.hideMetaInformation();
    // put property-group to panel
    profilePanel.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup(null, null)));
    // show metadata properties, default is hidden
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, profile);
    ppp.expandMetaData();
    ppp.hideCopyButton();
    ppp.hideEditButton();
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

}
