package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.*;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ThinclientView;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.vaadin.viritin.button.MButton;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_PANEL_COPY_TARGET_NAME;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PROFILE_PANEL_COPY_TARGET_NAME_WITH_NUMBER;

/**
 * Presenter for ProfilePanel
 */
public class ProfilePanelPresenter extends DirectoryObjectPanelPresenter {

  private Profile profile;
  private IMessageConveyor mc;

  public ProfilePanelPresenter(ThinclientView thinclientView, ProfilePanel view, Profile profile) {

    super(thinclientView, view, profile);
    this.profile = profile;

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    view.getCopyAction().addClickListener(this::handleCopyAction);
  }

  public void handleCopyAction(Button.ClickEvent event) {
    // damn!! still using LDAP stuff for copying objects
    try {

      // check if name already exists
      String newName = mc.getMessage(UI_PROFILE_PANEL_COPY_TARGET_NAME, profile.getName());
      for (int i = 1; thinclientView.getFreshProfile(newName) != null; i++) {
        newName = mc.getMessage(UI_PROFILE_PANEL_COPY_TARGET_NAME_WITH_NUMBER, i, profile.getName());
      }

      Profile copy = profile.getClass().newInstance();
      copy.setName(newName);
      copy.setDescription(profile.getDescription());
      copy.setSchema(profile.getSchema(profile.getRealm()));

      // copy properties
      Set<String> keys = profile.getProperties().getMap().keySet();
      keys.forEach(s -> copy.setValue(s, profile.getValue(s)));

      // client
      if (profile instanceof Client) {
        Client client = (Client) profile;
        Client copyClient = (Client) copy;
        copyClient.setHardwareType(client.getHardwareType());
        copyClient.setLocation(client.getLocation());
        copyClient.setMacAddress(client.getMacAddress());
      }

      thinclientView.save(copy);

      // display
      thinclientView.setItems(thinclientView.getAllItems());
      thinclientView.selectItem(copy);
    } catch (Exception e) {
      // TODO: handle exception
      // save failed
      e.printStackTrace();
    }
  }
}
