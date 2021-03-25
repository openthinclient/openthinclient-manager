package org.openthinclient.web.thinclient;

import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPasswordProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_PANEL_NEW_PROFILE_HEADER;


import org.openthinclient.common.model.Profile;

public abstract class AbstractProfileView<P extends Profile> extends AbstractDirectoryObjectView<P>{

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public void showProfileMetadata(P profile) {
        showProfileMetadataPanel(createProfileMetadataPanel(profile));
    }

  /**
   * Creates a ProfilePanel for metadata of new Profile with Save-Handling
   * @param profile the new profile
   * @return ProfilePanel
   */
  protected ProfilePanel createProfileMetadataPanel(P profile) {

    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_PROFILE_HEADER), profile.getClass());

    OtcPropertyGroup group = createOtcMetaDataPropertyGroup(profile);

    // show metadata properties, default is hidden
    ProfilePanelPresenter ppp = new ProfilePanelPresenter(this, profilePanel, profile);
    ppp.hideCopyButton();
    ppp.hideDeleteButton();

    // put property-group to panel
    ppp.setItemGroups(Arrays.asList(group, new OtcPropertyGroup()));
    ppp.onValuesWritten(profilePanel1 -> saveValues(ppp, profile));

    return profilePanel;
  }

  private OtcPropertyGroup createOtcMetaDataPropertyGroup(P profile) {

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

  /**
   * Set form-values to profile
   * @param profilePanelPresenter ProfilePanelPresenter contains ItemGroupPanels with form components
   * @param profile Profile to set the values
   */
  public void saveValues(ProfilePanelPresenter profilePanelPresenter, P profile) {

    LOGGER.debug("Save values for profile: " + profile);
    profilePanelPresenter.getItemGroupPanels().forEach(itemGroupPanel -> {
        // write values back from bean to profile
        itemGroupPanel.propertyComponents().stream()
            .map(propertyComponent -> (OtcProperty) propertyComponent.getBinder().getBean())
            .collect(Collectors.toList())
            .forEach(otcProperty -> {
                boolean isPasswordProperty = otcProperty instanceof OtcPasswordProperty;
                ItemConfiguration bean = otcProperty.getConfiguration();
                String propertyKey = otcProperty.getKey();
                String orig;
                if (propertyKey.equals("name")) {
                    orig = profile.getName();
                } else if (propertyKey.equals("description")) {
                    orig = profile.getDescription();
                } else if (propertyKey.equals("type") && profile.getRealm() != null) {
                    orig = profile.getSchema(profile.getRealm()).getName();
                } else {
                    orig = profile.getValueLocal(propertyKey);
                }
                String current = bean.getValue() == null || bean.getValue().length() == 0 ? null : bean.getValue();

                if (!StringUtils.equals(orig, current)) {
                    if (current != null) {
                    LOGGER.debug(" Apply value for " + propertyKey + "=" + (isPasswordProperty ? "***" : orig) + " with new value '" + (isPasswordProperty ? "***" : current) + "'");
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
                        LOGGER.debug(" Apply null value for description");
                        profile.setDescription(null);
                    } else {
                        LOGGER.debug(" Remove empty value for " + propertyKey);
                        profile.removeValue(propertyKey);
                    }
                    }
                } else {
                    LOGGER.debug(" Unchanged " + propertyKey + "=" + orig);
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
}
