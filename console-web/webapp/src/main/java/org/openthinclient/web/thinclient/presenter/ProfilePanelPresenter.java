package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.*;
import org.openthinclient.common.model.*;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ThinclientView;
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

  @Override
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
        copyClient.setClientGroups(client.getClientGroups());
        copyClient.setApplicationGroups(client.getApplicationGroups());
        copyClient.setApplications(client.getApplications());
        copyClient.setPrinters(client.getPrinters());

      // location
      } else if (profile instanceof Location) {
        Location location = (Location) profile;
        Location copyLocation= (Location) copy;
        copyLocation.setPrinters(location.getPrinters());

      // hardwaretype
      } else if (profile instanceof HardwareType) {
        HardwareType hardwareType = (HardwareType) profile;
        HardwareType copyHardwareType= (HardwareType) copy;
        copyHardwareType.setDevices(hardwareType.getDevices());
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
