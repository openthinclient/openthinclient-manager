package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.*;
import org.openthinclient.common.model.*;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.ThinclientView;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.openthinclient.web.thinclient.model.DeleteMandate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.button.MButton;

import java.util.List;
import java.util.function.Function;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

/**
 * Presenter for DirectoryObjectPanel
 */
public class DirectoryObjectPanelPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryObjectPanelPresenter.class);

  private final IMessageConveyor mc;

  ThinclientView thinclientView;
  ProfilePanel view;
  DirectoryObject directoryObject;
  Function<DirectoryObject, DeleteMandate> deleteMandatSupplier;

  public DirectoryObjectPanelPresenter(ThinclientView thinclientView, ProfilePanel view, DirectoryObject directoryObject) {

    this.thinclientView = thinclientView;
    this.view = view;
    this.directoryObject = directoryObject;

    mc = new MessageConveyor(UI.getCurrent().getLocale());

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
      view.showMetaInformation();
    } else {
      view.getMetaDataItemGroupPanel().expandItems();
      view.hideMetaInformation();
    }
    // close all others
    view.handleItemGroupVisibility(view.getMetaDataItemGroupPanel());
  }

  public void handleDeleteAction(Button.ClickEvent event) {

    VerticalLayout content = new VerticalLayout();
    Window window = new Window(null, content);
    window.setModal(true);
    window.setPositionX(200);
    window.setPositionY(50);

    boolean deletionAllowed = true;
    if (deleteMandatSupplier != null) {
      DeleteMandate mandate = deleteMandatSupplier.apply(directoryObject);
      deletionAllowed = mandate.checkDelete();
      if (!deletionAllowed) {
        window.setCaption(mc.getMessage(UI_COMMON_DELETE_NOT_POSSIBLE_HEADER));
        content.addComponent(new Label(mandate.getMessage()));
      }
    }

    if (deletionAllowed) {
      window.setCaption(mc.getMessage(UI_COMMON_CONFIRM_DELETE));
      content.addComponent(new Label(mc.getMessage(UI_COMMON_CONFIRM_DELETE_OBJECT_TEXT, directoryObject.getName())));
      HorizontalLayout hl = new HorizontalLayout();
      hl.addComponents(new MButton(mc.getMessage(UI_BUTTON_CANCEL), event1 -> window.close()),
          new MButton(mc.getMessage(UI_COMMON_DELETE), event1 -> {

            Realm realm = directoryObject.getRealm();
            try {
              realm.getDirectory().delete(directoryObject);
            } catch (DirectoryException e) {
              // TODO: handle exception
              // delete failed
              e.printStackTrace();
            }

            // update display
            try {
              thinclientView.setItems(thinclientView.getAllItems());
            } catch (AllItemsListException e) {
              thinclientView.showError(e);
            }
            window.close();
            UI.getCurrent().removeWindow(window);
          }));
      content.addComponent(hl);
    }

    UI.getCurrent().addWindow(window);
  }

  public void setPanelMetaInformation(List<Component> components) {
    view.setPanelMetaInformation(components);
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

  public void setDeleteMandate(Function<DirectoryObject, DeleteMandate> deleteMandatSupplier) {
    this.deleteMandatSupplier = deleteMandatSupplier;
  }
}
