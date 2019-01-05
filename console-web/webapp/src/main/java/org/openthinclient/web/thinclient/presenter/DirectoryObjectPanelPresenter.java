package org.openthinclient.web.thinclient.presenter;

import com.vaadin.ui.*;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ThinclientView;
import org.vaadin.viritin.button.MButton;

import java.util.Set;

/**
 * Presenter for DirectoryObjectPanel
 */
public class DirectoryObjectPanelPresenter {

  ThinclientView thinclientView;
  ProfilePanel view;
  DirectoryObject directoryObject;

  public DirectoryObjectPanelPresenter(ThinclientView thinclientView, ProfilePanel view, DirectoryObject directoryObject) {

    this.thinclientView = thinclientView;
    this.view = view;
    this.directoryObject = directoryObject;

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

  public void handleDeleteAction(Button.ClickEvent event) {

    VerticalLayout content = new VerticalLayout();
    Window window = new Window("Löschen bestätigen", content);
    window.setModal(true);
    window.setPositionX(200);
    window.setPositionY(50);

    content.addComponent(new Label("Wollen Sie das Profile " + directoryObject.getName() + " löschen?"));
    HorizontalLayout hl = new HorizontalLayout();
    hl.addComponents(new MButton("Cancel", event1 -> window.close()),
                     new MButton("Löschen", event1 -> {

                       Realm realm = directoryObject.getRealm();
                       try {
                         realm.getDirectory().delete(directoryObject);
                       } catch (DirectoryException e) {
                         // TODO: handle exception
                         // delete failed
                         e.printStackTrace();
                       }

                       // update display
                       thinclientView.setItems(thinclientView.getAllItems());
                       window.close();
                       UI.getCurrent().removeWindow(window);
                     }));
    content.addComponent(hl);

    UI.getCurrent().addWindow(window);

  }

  public void hideCopyButton() {
    view.getCopyAction().setVisible(false);
  }

  public void addPanelCaptionComponent(Component component) {
    view.addPanelCaptionComponent(component);
  }

  public void hideDeleteButton() {
    view.getDeleteProfileAction().setVisible(false);
  }

  public void hideEditButton() {
    view.getEditAction().setVisible(false);
  }
}
