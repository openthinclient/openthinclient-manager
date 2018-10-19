package org.openthinclient.web.thinclient.presenter;

import com.vaadin.ui.Button;
import org.openthinclient.common.model.Profile;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ThinclientView;

/**
 * Presenter for ProfilePanel
 */
public class ProfilePanelPresenter {

  ThinclientView thinclientView;
  ProfilePanel profilePanel;
  Profile profile;

  public ProfilePanelPresenter(ThinclientView thinclientView, ProfilePanel view, Profile profile) {

    this.thinclientView = thinclientView;
    this.profilePanel = view;
    this.profile = profile;

    view.getCopyAction().addClickListener(this::handleCopyAction);
    view.getEditAction().addClickListener(this::handleEditAction);
    view.getDeleteProfileAction().addClickListener(this::handleDeleteAction);
  }

  public void handleEditAction(Button.ClickEvent event) {
    thinclientView.showProfileMetaData(profile);
  }

  public void handleCopyAction(Button.ClickEvent event) {
    // damn!! still using LDAP stuff for copying objects:
    // 1. create new object with same name and SAVE
    // 2. copying attributes


  }

  public void handleDeleteAction(Button.ClickEvent event) {

  }
}
