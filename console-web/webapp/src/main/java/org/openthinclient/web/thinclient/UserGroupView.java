package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = UserGroupView.NAME, ui = ManagerUI.class)
public final class UserGroupView extends AbstractGroupView<UserGroup> {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserGroupView.class);

  public static final String NAME = "usergroup_view";
  public static final ConsoleWebMessages TITLE_KEY = UI_USERGROUP_HEADER;

  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private UserService userService;
  @Autowired
  private UserGroupService userGroupService;

  private boolean secondaryDirectory = false;

  @PostConstruct
  public void setup() {
    secondaryDirectory = getRealmService().getDefaultRealm().isSecondaryConfigured();
    addStyleName(UserView.NAME);
  }

  @Override
  protected String getSubtitle() {
    return mc.getMessage(UI_USERGROUP);
  }

  @Override
  public Set<UserGroup> getAllItems() {
    return userGroupService.findAll();
  }

  @Override
  protected Class<UserGroup> getItemClass() {
    return UserGroup.class;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(UserGroup userGroup) {
    Set<User> members = userGroup.getMembers();

    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(UserGroup.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<User> allUsers = userService.findAll();
    Consumer<List<Item>> profileReferenceChangeConsumer = null;
    if(!secondaryDirectory) {
      profileReferenceChangeConsumer = values -> saveReference(userGroup, values, allUsers, User.class);
    }
    refPresenter.showReference(members, User.class,
                                mc.getMessage(UI_USER_HEADER),
                                allUsers,
                                profileReferenceChangeConsumer);

    Set<Application> allApplications = applicationService.findAll();
    refPresenter.showReference(userGroup.getApplications(),
                                mc.getMessage(UI_APPLICATION_HEADER),
                                allApplications,
                                values -> saveReference(userGroup, values, allApplications, Application.class));

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(userGroup.getApplicationGroups(),
                                mc.getMessage(UI_APPLICATIONGROUP_HEADER),
                                allApplicationGroups,
                                values -> saveReference(userGroup, values, allApplicationGroups, ApplicationGroup.class),
                                getApplicationsForApplicationGroupFunction(userGroup));

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(userGroup.getPrinters(),
                                mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters,
                                values -> saveReference(userGroup, values, allPrinters, Printer.class));

    return referencesPanel;

  }

  private Function<Item, List<Item>> getApplicationsForApplicationGroupFunction(UserGroup userGroup) {
    return item -> userGroup.getApplicationGroups().stream()
                    .filter(group -> group.getName().equals(item.getName()))
                    .findFirst()
                    .map(group -> ProfilePropertiesBuilder.createItems(group.getApplications()))
                    .orElse(Collections.emptyList());
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
  public ProfilePanel createProfilePanel(UserGroup profile, boolean isNew) {
    return super.createProfilePanel(profile, !secondaryDirectory && isNew);
  }

  @Override
  protected UserGroup newProfile() {
    return new UserGroup();
  }

  @Override
  public UserGroup getFreshProfile(String name) {
     return userGroupService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(UserGroup profile, Class<D> clazz) {
    if (clazz == User.class) {
      return (Set<D>)profile.getMembers();
    } else if (clazz == ApplicationGroup.class) {
      return (Set<D>)profile.getApplicationGroups();
    } else if (clazz == Application.class) {
      return (Set<D>)profile.getApplications();
    } else if (clazz == Printer.class) {
      return (Set<D>)profile.getPrinters();
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public void save(UserGroup profile) {
    LOGGER.info("Save: " + profile);
    userGroupService.save(profile);
    Audit.logSave(profile);
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
}
