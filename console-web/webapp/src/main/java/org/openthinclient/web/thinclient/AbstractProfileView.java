package org.openthinclient.web.thinclient;

import org.apache.commons.codec.digest.Sha2Crypt;
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
                String key = otcProperty.getKey();
                String current = otcProperty.getConfiguration().getValue();
                if (current != null && current.isEmpty()) current = null;

                // Special case: Hashed passwords can only be set
                if (otcProperty instanceof OtcPasswordProperty
                    && ((OtcPasswordProperty) otcProperty).isHashed()
                ) {
                  if (current != null) {
                    profile.setValue(
                        key, Sha2Crypt.sha512Crypt(current.getBytes()));
                  }
                  return;
                }

                String orig;
                if (key.equals("name")) {
                    orig = profile.getName();
                } else if (key.equals("description")) {
                    orig = profile.getDescription();
                } else if (key.equals("type") && profile.getRealm() != null) {
                    orig = profile.getSchema(profile.getRealm()).getName();
                } else {
                    orig = profile.getValueLocal(key);
                }

                if (StringUtils.equals(orig, current)) return;  // Nothing to do

                if (current != null) {
                  switch (key) {
                    case "name":
                      profile.setName(current);
                      break;
                    case "description":
                      profile.setDescription(current);
                      break;
                    case "type":
                      LOGGER.warn("Aborted item type change!");
                      break;
                    default:
                      profile.setValue(key, current);
                  }
                } else {
                  if (key.equals("description")) {
                      profile.setDescription(null);
                  } else {
                      profile.removeValue(key);
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
}
