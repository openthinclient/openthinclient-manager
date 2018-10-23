package org.openthinclient.web.thinclient.presenter;

import com.vaadin.ui.Button;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ThinclientView;

import java.util.Optional;
import java.util.Set;

/**
 * Presenter for ProfilePanel
 */
public class ProfilePanelPresenter {

  ThinclientView thinclientView;
  ProfilePanel view;
  Profile profile;

  public ProfilePanelPresenter(ThinclientView thinclientView, ProfilePanel view, Profile profile) {

    this.thinclientView = thinclientView;
    this.view = view;
    this.profile = profile;

    view.getCopyAction().addClickListener(this::handleCopyAction);
    view.getEditAction().addClickListener(this::handleEditAction);
    view.getDeleteProfileAction().addClickListener(this::handleDeleteAction);
  }

  public void expandMetaData() {
    view.getMetaDataItemGroupPanel().expandItems();
  }

  public void handleEditAction(Button.ClickEvent event) {
    // handle meta data visibility separately
    if (view.getMetaDataItemGroupPanel().isItemsVisible()) {
      view.getMetaDataItemGroupPanel().collapseItems();
    } else {
      view.getMetaDataItemGroupPanel().expandItems();
    }
    // close all others
    view.handleItemGroupVisibility(view.getMetaDataItemGroupPanel());
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

  public void handleDeleteAction(Button.ClickEvent event) {

    // TODO: confirmdialog
    Realm realm = profile.getRealm();
    try {
      realm.getDirectory().delete(profile);
    } catch (DirectoryException e) {
      // TODO: handle exception
      // delete failed
      e.printStackTrace();
    }

    // update display
    thinclientView.setItems(thinclientView.getAllItems());
    thinclientView.selectItem(null);
  }
}
