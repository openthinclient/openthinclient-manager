package org.openthinclient.web.thinclient;

import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public abstract class AbstractGroupView<T extends DirectoryObject> extends AbstractDirectoryObjectView<T> {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  abstract protected String getSubtitle();

  /**
   * Set form-values to profile
   * @param profilePanelPresenter DirectoryObjectPanelPresenter contains ItemGroupPanels with form components
   * @param profile DirectoryObject to set the values
   */
  public void saveValues(DirectoryObjectPanelPresenter profilePanelPresenter, T profile) {

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
            } else if (current == null || current.length() == 0) {
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

  public void showProfileMetadata(T directoryObject) {
    showProfileMetadataPanel(createProfilePanel(directoryObject, true));
  }

  public ProfilePanel createProfilePanel(T directoryObject) {
    return createProfilePanel(directoryObject, false);
  }

  public ProfilePanel createProfilePanel(T directoryObject, boolean isNew) {
    String title = isNew? mc.getMessage(UI_PROFILE_PANEL_NEW_GROUP_HEADER) : directoryObject.getName();
    String subtitle = isNew? "" : getSubtitle();
    ProfilePanel profilePanel = new ProfilePanel(title,
                                                  subtitle,
                                                  directoryObject.getClass());
    DirectoryObjectPanelPresenter presenter = new DirectoryObjectPanelPresenter(this, profilePanel, directoryObject);

    OtcPropertyGroup propertyGroup = createMetadataPropertyGroup(directoryObject, isNew);
    presenter.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup()));

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
}
