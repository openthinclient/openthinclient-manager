package org.openthinclient.web.thinclient.presenter;

import com.vaadin.ui.*;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ThinclientView;
import org.vaadin.viritin.button.MButton;

import java.util.Optional;
import java.util.Set;

/**
 * Presenter for ProfilePanel
 */
public class ProfilePanelPresenter extends DirectoryObjectPanelPresenter {

  private Profile profile;

  public ProfilePanelPresenter(ThinclientView thinclientView, ProfilePanel view, Profile profile) {

    super(thinclientView, view, profile);
    this.profile = profile;

    view.getCopyAction().addClickListener(this::handleCopyAction);
  }

  public void handleCopyAction(Button.ClickEvent event) {
    // damn!! still using LDAP stuff for copying objects
    try {

      // check if name already exists
      String newName = "Duplikat von " + profile.getName();
      for (int i = 1; thinclientView.getFreshProfile(newName) != null; i++) {
        newName = "Duplikat (" + i + ") von " + profile.getName();
      }

      Profile copy = profile.getClass().newInstance();
      copy.setName(newName);
      copy.setDescription(profile.getDescription());
      copy.setSchema(profile.getSchema(profile.getRealm()));

      // copy properties
      Set<String> keys = profile.getProperties().getMap().keySet();
      keys.forEach(s -> copy.setValue(s, profile.getValue(s)));

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
