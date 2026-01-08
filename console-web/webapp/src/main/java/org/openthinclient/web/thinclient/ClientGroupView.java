package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ClientGroupView.NAME, ui= ManagerUI.class)
@ThemeIcon(ClientGroupView.ICON)
public final class ClientGroupView extends AbstractGroupView<ClientGroup> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientGroupView.class);

  public static final String NAME = "clientgroup_view";
  public static final String ICON = "";
  public static final ConsoleWebMessages TITLE_KEY = UI_CLIENTGROUP_HEADER;

  @Autowired
  private ClientService clientService;
  @Autowired
  private ClientGroupService clientGroupService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;

  @PostConstruct
  public void setup() {
    addStyleName(ClientView.NAME);
  }

  @Override
  protected String getSubtitle() {
    return mc.getMessage(UI_CLIENTGROUP);
  }

  @Override
  public Set<ClientGroup> getAllItems() {
    return clientGroupService.findAll();
  }

  @Override
  protected Class<ClientGroup> getItemClass() {
    return ClientGroup.class;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(ClientGroup clientGroup) {
    Set<Client> members = clientGroup.getMembers();

    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(ClientGroup.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<ClientMetaData> allClients = clientService.findAllClientMetaData();
    refPresenter.showReference(members, Client.class,
                                mc.getMessage(UI_CLIENT_HEADER),
                                allClients,
                                values -> saveReference(clientGroup, values, allClients, Client.class));

    Set<Application> allApplications = applicationService.findAll();
    refPresenter.showReference(clientGroup.getApplications(),
                                mc.getMessage(UI_APPLICATION_HEADER),
                                allApplications,
                                values -> saveReference(clientGroup, values, allApplications, Application.class));

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(clientGroup.getApplicationGroups(),
                                mc.getMessage(UI_APPLICATIONGROUP_HEADER),
                                allApplicationGroups,
                                values -> saveReference(clientGroup, values, allApplicationGroups, ApplicationGroup.class),
                                getApplicationsForApplicationGroupFunction(clientGroup));

    return referencesPanel;
  }

  private Function<Item, List<Item>> getApplicationsForApplicationGroupFunction(ClientGroup clientGroup) {
    return item -> clientGroup.getApplicationGroups().stream()
                    .filter(group -> group.getName().equals(item.getName()))
                    .findFirst()
                    .map(group -> ProfilePropertiesBuilder.createItems(group.getApplications()))
                    .orElse(Collections.emptyList());
  }

  @Override
  protected ClientGroup newProfile() {
    return new ClientGroup();
  }

  @Override
  public ClientGroup getFreshProfile(String name) {
     return clientGroupService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(ClientGroup profile, Class<D> clazz) {
    if (clazz == Client.class) {
      return (Set<D>)profile.getMembers();
    } else if (clazz == ApplicationGroup.class) {
      return (Set<D>)profile.getApplicationGroups();
    } else if (clazz == Application.class) {
      return (Set<D>)profile.getApplications();
    } else {
      return (Set<D>)profile.getClientGroups();
    }
  }

  @Override
  public void save(ClientGroup profile) {
    LOGGER.info("Save: " + profile);
    clientGroupService.save(profile);
    Audit.logSave(profile);
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public String getParentViewName() {
    return ClientView.NAME;
  }

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

}
