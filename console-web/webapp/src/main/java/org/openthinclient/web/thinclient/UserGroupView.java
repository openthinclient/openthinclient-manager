package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = UserGroupView.NAME, ui = ManagerUI.class)
public final class UserGroupView extends AbstractThinclientGroupView {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserGroupView.class);

  public static final String NAME = "usergroup_view";
  public static final String ICON = "icon/applicationgroup-white.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_USERGROUP_HEADER;

  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private UserService userService;
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;

  private boolean secondaryDirectory = false;

  @PostConstruct
  public void setup() {
    secondaryDirectory = "secondary".equals(getRealmService().getDefaultRealm().getValue("UserGroupSettings.DirectoryVersion"));
    addStyleName(UserView.NAME);
  }

  @Override
  public Set getAllItems() {
    return userGroupService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(UserGroup.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(UserGroup.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(DirectoryObject item) {
    UserGroup userGroup = (UserGroup) item;
    Set<User> members = userGroup.getMembers();

    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(UserGroup.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<User> allUsers = userService.findAll();
    getRealmService().findAllRealms().forEach(realm ->
      allUsers.removeAll(realm.getAdministrators().getMembers())
    );
    refPresenter.showReference(members, mc.getMessage(UI_USER_HEADER),
                                allUsers, User.class,
                                values -> saveReference(userGroup, values, allUsers, User.class),
                                null, secondaryDirectory);

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(userGroup.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER),
                                allApplicationGroups, ApplicationGroup.class,
                                values -> saveReference(userGroup, values, allApplicationGroups, ApplicationGroup.class));

    Set<Application> allApplicatios = applicationService.findAll();
    refPresenter.showReference(userGroup.getApplications(), mc.getMessage(UI_APPLICATION_HEADER),
                                allApplicatios, Application.class,
                                values -> saveReference(userGroup, values, allApplicatios, Application.class));

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(userGroup.getPrinters(), mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters, Printer.class,
                                values -> saveReference(userGroup, values, allPrinters, Printer.class));

    return referencesPanel;

  }

  @Override
  protected OtcPropertyGroup createMetadataPropertyGroup(DirectoryObject directoryObject, boolean isNew) {
    OtcPropertyGroup group = super.createMetadataPropertyGroup(directoryObject, isNew);
    if(secondaryDirectory) {
      group.getOtcProperties().forEach(p -> p.getConfiguration().disable());
    }
    return group;
  }

  @Override
  public ProfilePanel createProfilePanel(DirectoryObject directoryObject, boolean isNew) {
    return super.createProfilePanel(directoryObject, isNew || secondaryDirectory);
  }

  @Override
  public UserGroup getFreshProfile(String name) {
     return userGroupService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
    LOGGER.info("Save: " + profile);
    userGroupService.save((UserGroup) profile);
    Audit.logSave(profile);
  }

  @Override
  public Client getClient(String name) {
    return clientService.findByName(name);
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public String getParentViewName() {
    return UserView.NAME;
  }

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }
}
