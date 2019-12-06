package org.openthinclient.web.thinclient;

import com.vaadin.navigator.ViewChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPasswordProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;

import java.util.*;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public abstract class AbstractThinclientGroupView extends AbstractThinclientView {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  public AbstractThinclientGroupView(ConsoleWebMessages i18nTitleKey, EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
    super(i18nTitleKey, eventBus, notificationService);
  }

  /**
   * Set form-values to profile
   * @param profilePanelPresenter DirectoryObjectPanelPresenter contains ItemGroupPanels with form components
   * @param profile DirectoryObject to set the values
   */
  public void saveValues(DirectoryObjectPanelPresenter profilePanelPresenter, DirectoryObject profile) {

    LOGGER.info("Save values for group: " + profile);

    profilePanelPresenter.getItemGroupPanels().forEach(itemGroupPanel -> {

        // write values back from bean to profile
        itemGroupPanel.propertyComponents().stream()
        .map(propertyComponent -> (OtcProperty) propertyComponent.getBinder().getBean())
        .collect(Collectors.toList())
        .forEach(otcProperty -> {
            ItemConfiguration bean = otcProperty.getConfiguration();
            String propertyKey = otcProperty.getKey();
            String org = null;
            if (propertyKey.equals("name"))  {
              org = profile.getName();
            } else if (propertyKey.equals("description")) {
              org = profile.getDescription();
            } else {
              LOGGER.warn("Unexpected key {} for group {} will be ignored!", propertyKey, profile.getClass().getName());
            }
            String current = bean.getValue() == null || bean.getValue().length() == 0 ? null : bean.getValue();
            if (current != null && !StringUtils.equals(org, current)) {
              switch (propertyKey) {
              case "name":
                profile.setName(current);
                break;
              case "description":
                profile.setDescription(current);
                break;
              }
            } else {
              if (propertyKey.equals("description")) {
                LOGGER.info(" Apply null value for description");
                profile.setDescription(null);
              }
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

  public void showProfileMetadata(DirectoryObject directoryObject) {
    showProfileMetadataPanel(createProfilePanel(directoryObject, true));
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) {
    return createProfilePanel(directoryObject, false);
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject, boolean isNew) {
    String title = isNew? mc.getMessage(UI_PROFILE_PANEL_NEW_GROUP_HEADER) : directoryObject.getName();
    ProfilePanel profilePanel = new ProfilePanel(title, directoryObject.getClass());
    DirectoryObjectPanelPresenter presenter = new DirectoryObjectPanelPresenter(this, profilePanel, directoryObject);

    OtcPropertyGroup propertyGroup = createMetadataPropertyGroup(directoryObject, isNew);
    presenter.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup(null, null)));

    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, directoryObject));
    if (isNew) {
      presenter.hideCopyButton();
      presenter.hideDeleteButton();
    }

    return profilePanel;
  }

  protected OtcPropertyGroup createMetadataPropertyGroup(DirectoryObject directoryObject, boolean isNew) {

    OtcPropertyGroup group = builder.createDirectoryObjectMetaDataGroup(directoryObject);

    if (isNew) {
      addProfileNameAlreadyExistsValidator(group);
    }

    return group;
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    LOGGER.debug("enter -> source={}, navigator-state=", event.getSource(), event.getNavigator().getState());
    String[] params = Optional.ofNullable(event.getParameters()).orElse("").split("/", 2);
    if (params.length > 0) {
      // handle create action
      if ("create".equals(params[0])) {
        switch (event.getViewName()) {
        case ApplicationGroupView.NAME:
            showProfileMetadata(new ApplicationGroup());
            break;
          case UserGroupView.NAME:
            showProfileMetadata(new UserGroup());
            break;
        }
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

}
