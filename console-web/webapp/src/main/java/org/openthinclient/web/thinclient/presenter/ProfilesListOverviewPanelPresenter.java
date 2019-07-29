package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import org.openthinclient.common.model.ClientMetaData;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.thinclient.AbstractThinclientView;
import org.openthinclient.web.thinclient.component.ProfilesListOverviewPanel;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.vaadin.viritin.button.MButton;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class ProfilesListOverviewPanelPresenter {

  private AbstractThinclientView thinclientView;
  private ProfilesListOverviewPanel panel;
  private Registration addClickListenerRegistration = null;
  private Registration deleteClickListenerRegistration = null;
  private Supplier<Set<DirectoryObject>> itemsSupplier = null;

  public ProfilesListOverviewPanelPresenter(AbstractThinclientView thinclientView, ProfilesListOverviewPanel panel) {
    this.thinclientView = thinclientView;
    this.panel = panel;

    // set some default behaviour
    addNewButtonClickHandler(e -> UI.getCurrent().getNavigator().navigateTo(thinclientView.getViewName() + "/create"));
    addDeleteButtonClickHandler(this::handleDeleteAction);
    panel.setItemButtonClickedConsumer(dirObj -> UI.getCurrent().getNavigator().navigateTo(thinclientView.getViewName() + "/" + dirObj.getName()));
  }

  private void handleDeleteAction(Button.ClickEvent event) {

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
// TODO: delete action
//    MultiSelectionModel<DirectoryObject> selectionModel = (MultiSelectionModel<DirectoryObject>) panel.getItemGrid().getSelectionModel();
//    Set<DirectoryObject> selectedItems = selectionModel.getSelectedItems();
    Set<DirectoryObject> selectedItems = panel.getSelectedItems();

    VerticalLayout content = new VerticalLayout();
    Window window = new Window(null, content);
    window.setModal(true);
    window.setPositionX(200);
    window.setPositionY(50);

    boolean deletionAllowed = true;
    // TODO: HardwareType und Location dürfen nicht gelöscht werden wenn es noch members gibt!!
//    if (deleteMandatSupplier != null) {
//      DeleteMandate mandate = deleteMandatSupplier.apply(directoryObject);
//      deletionAllowed = mandate.checkDelete();
//      if (!deletionAllowed) {
//        window.setCaption(mc.getMessage(UI_COMMON_DELETE_NOT_POSSIBLE_HEADER));
//        content.addComponent(new Label(mandate.getMessage()));
//      }
//    }

    if (deletionAllowed) {
      window.setCaption(mc.getMessage(UI_COMMON_CONFIRM_DELETE));
      if (selectedItems.size() == 1) {
        content.addComponent(new Label(mc.getMessage(UI_COMMON_CONFIRM_DELETE_OBJECT_TEXT, selectedItems.iterator().next().getName())));
      } else {
        StringJoiner sj = new StringJoiner(",<br>");
        selectedItems.forEach(item -> sj.add(item.getName()));
        content.addComponent(new Label(mc.getMessage(UI_COMMON_CONFIRM_DELETE_OBJECTS_TEXT, sj.toString()), ContentMode.HTML));
      }
      HorizontalLayout hl = new HorizontalLayout();
      hl.addComponents(new MButton(mc.getMessage(UI_BUTTON_CANCEL), event1 -> window.close()),
          new MButton(mc.getMessage(UI_COMMON_DELETE), event1 -> {
            selectedItems.forEach(item -> {
              if (item instanceof ClientMetaData) { // get the full client-profile
                item = thinclientView.getFreshProfile(item.getName());
              }
              Realm realm = item.getRealm();
              try {
                realm.getDirectory().delete(item);
              } catch (DirectoryException e) {
                thinclientView.showError(e);
              }
            });
            // update display
            try {
              Set<DirectoryObject> allItems = itemsSupplier == null ? thinclientView.getAllItems() : itemsSupplier.get();
              panel.setDataProvider(DataProvider.ofCollection(allItems));
              thinclientView.selectItem(null);
              panel.getCheckBox().setValue(false);

            } catch (AllItemsListException e) {
              thinclientView.showError(e);
            }
            window.close();
            UI.getCurrent().removeWindow(window);
          }));
      content.addComponent(hl);
    }

    if (selectedItems.size() > 0) {
      UI.getCurrent().addWindow(window);
    }

  }

  public void addNewButtonClickHandler(Button.ClickListener clickListener) {
    if (addClickListenerRegistration != null) addClickListenerRegistration.remove();
    addClickListenerRegistration = panel.getAddButton().addClickListener(clickListener);
  }

  public void addDeleteButtonClickHandler(Button.ClickListener clickListener) {
    if (deleteClickListenerRegistration != null) deleteClickListenerRegistration.remove();
    deleteClickListenerRegistration = panel.getDeleteButton().addClickListener(clickListener);
  }

  public void setItemButtonClickedConsumer(Consumer<DirectoryObject> itemButtonClickedConsumer) {
    panel.setItemButtonClickedConsumer(itemButtonClickedConsumer);
  }

  public void setDataProvider(ListDataProvider<DirectoryObject> dataProvider) {
    panel.setDataProvider(dataProvider);
  }

  public void setVisible(boolean visible) {
    panel.setVisible(visible);
  }

  public void setItemsSupplier(Supplier<Set<DirectoryObject>> itemsSupplier) {
    this.itemsSupplier = itemsSupplier;
  }

  public void disableActions() {
    panel.getCheckBox().setEnabled(false);
    if (addClickListenerRegistration != null) addClickListenerRegistration.remove();
    panel.getAddButton().setEnabled(false);
    if (deleteClickListenerRegistration != null) deleteClickListenerRegistration.remove();
    panel.getDeleteButton().setEnabled(false);
  }
}
