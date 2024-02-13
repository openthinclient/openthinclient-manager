package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.openthinclient.api.ldif.export.LdifExporterService;
import org.openthinclient.common.model.ClientMetaData;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.web.thinclient.AbstractDirectoryObjectView;
import org.openthinclient.web.thinclient.component.ProfilesListOverviewPanel;
import org.openthinclient.web.thinclient.exception.ProfileNotDeletedException;
import org.openthinclient.web.thinclient.model.DeleteMandate;
import org.vaadin.viritin.button.MButton;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_BUTTON_CANCEL;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_CONFIRM_DELETE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_CONFIRM_DELETE_OBJECTS_TEXT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_CONFIRM_DELETE_OBJECT_TEXT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_DELETE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_DELETE_NOT_POSSIBLE_HEADER;

public class ProfilesListOverviewPanelPresenter {

  private AbstractDirectoryObjectView directoryObjectView;
  private ProfilesListOverviewPanel panel;
  private Registration addClickListenerRegistration = null;
  private Registration deleteClickListenerRegistration = null;
  private Registration wolClickListenerRegistration = null;
  private Registration restartClickListenerRegistration = null;
  private Registration shutdownClickListenerRegistration = null;
  private Supplier<Set<DirectoryObject>> itemsSupplier = null;
  private LdifExporterService ldifExporterService;
  private Function<DirectoryObject, DeleteMandate> deleteMandatSupplier = null;

  public ProfilesListOverviewPanelPresenter(AbstractDirectoryObjectView directoryObjectView, ProfilesListOverviewPanel panel, LdifExporterService ldifExporterService) {
    this.directoryObjectView = directoryObjectView;
    this.panel = panel;
    this.ldifExporterService = ldifExporterService;

    // set some default behaviour
    addNewButtonClickHandler(e -> UI.getCurrent().getNavigator().navigateTo(directoryObjectView.getViewName() + "/create"));
    addDeleteButtonClickHandler(this::handleDeleteAction);
    extendLdifExportButton(createResource());
    panel.setItemButtonClickedConsumer(dirObj -> UI.getCurrent().getNavigator().navigateTo(directoryObjectView.getViewName() + "/edit/" + dirObj.getName()));
  }

  private StreamResource createResource() {
    return new StreamResource((StreamResource.StreamSource) () -> {
      Set<String> dns = panel.getSelectedItems().stream()
          .map(DirectoryObject::getDn)
          .filter(s -> s.contains(ldifExporterService.getBaseDN()))
          .map(s -> s.substring(0, s.indexOf(ldifExporterService.getBaseDN())-1))
          .collect(Collectors.toSet());

      Set<LdifExporterService.State> exportResult = new HashSet<>();
      byte[] bytes = ldifExporterService.performAction(dns, exportResult::add);
      if (exportResult.contains(LdifExporterService.State.ERROR) || exportResult.contains(LdifExporterService.State.EXCEPTION)) {
        // TODO: place error-message somewhere
        return null;
      } else {
        // TODO: place success-message somewhere?
        return new ByteArrayInputStream(bytes);
      }
    }, "export.ldif");
  }

  private void handleDeleteAction(Button.ClickEvent event) {

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    Set<DirectoryObject> selectedItems = panel.getSelectedItems();

    VerticalLayout content = new VerticalLayout();
    Window window = new Window(null, content);
    window.setModal(true);
    window.center();

    boolean deletionAllowed = true;
    // hardware type und location must not be delted if they are in use
    if (deleteMandatSupplier != null) {
      StringBuilder messages = new StringBuilder();
      for (DirectoryObject directoryObject : selectedItems) {
        DeleteMandate mandate = deleteMandatSupplier.apply(directoryObject);
        boolean allowed = mandate.checkDelete();
        if (!allowed) {
          messages.append(mandate.getMessage()).append("<br>\n");
          deletionAllowed = false;
        }
      }

      if (!deletionAllowed) {
        window.setCaption(mc.getMessage(UI_COMMON_DELETE_NOT_POSSIBLE_HEADER));
        content.addComponent(new Label(messages.toString(), ContentMode.HTML));
      }
    }

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
              if (item.getClass().equals(ClientMetaData.class)) { // get the full client-profile
                item = directoryObjectView.getFreshProfile(item.getName());
              }
              try {
                directoryObjectView.delete(item);
              } catch(ProfileNotDeletedException e) {
                directoryObjectView.showError(e);
              }
            });
            // update display
            Set<DirectoryObject> allItems = itemsSupplier == null ? directoryObjectView.getAllItems() : itemsSupplier.get();
            panel.setDataProvider(DataProvider.ofCollection(allItems));
            directoryObjectView.selectItem(null);
            panel.getCheckBox().setValue(false);
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

  public void extendLdifExportButton(StreamResource myResource) {
    FileDownloader fileDownloader = new FileDownloader(myResource);
    fileDownloader.extend(panel.getLdifExportButton());
  }

  public void addWolButtonClickHandler(Consumer<Set<DirectoryObject>> clickListener) {
    Button button = panel.getWolButton();
    button.setVisible(true);
    if (wolClickListenerRegistration != null) wolClickListenerRegistration.remove();
    wolClickListenerRegistration = button.addClickListener(ev -> clickListener.accept(panel.getSelectedItems()));
  }

  public void addRestartButtonClickHandler(Consumer<Set<DirectoryObject>> clickListener) {
    Button button = panel.getRestartButton();
    button.setVisible(true);
    if (restartClickListenerRegistration != null) restartClickListenerRegistration.remove();
    restartClickListenerRegistration = button.addClickListener(ev -> clickListener.accept(panel.getSelectedItems()));
  }

  public void addShutdownButtonClickHandler(Consumer<Set<DirectoryObject>> clickListener) {
    Button button = panel.getShutdownButton();
    button.setVisible(true);
    if (shutdownClickListenerRegistration != null) shutdownClickListenerRegistration.remove();
    shutdownClickListenerRegistration = button.addClickListener(ev -> clickListener.accept(panel.getSelectedItems()));
  }

  public void setItemButtonClickedConsumer(Consumer<DirectoryObject> itemButtonClickedConsumer) {
    panel.setItemButtonClickedConsumer(itemButtonClickedConsumer);
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

  public void setDeleteMandatSupplier(Function<DirectoryObject, DeleteMandate> deleteMandatSupplier) {
    this.deleteMandatSupplier = deleteMandatSupplier;
  }

  public ProfilesListOverviewPanel getPanel() {
    return panel;
  }

}
