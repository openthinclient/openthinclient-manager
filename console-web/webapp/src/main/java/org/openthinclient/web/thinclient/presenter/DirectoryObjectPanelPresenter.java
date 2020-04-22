package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.shared.Registration;
import com.vaadin.ui.*;
import org.openthinclient.common.model.*;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.AbstractThinclientView;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.PropertyComponent;
import org.openthinclient.web.thinclient.model.DeleteMandate;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.button.MButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

/**
 * Presenter for DirectoryObjectPanel
 */
public class DirectoryObjectPanelPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryObjectPanelPresenter.class);

  private final IMessageConveyor mc;

  AbstractThinclientView thinclientView;
  ProfilePanel view;
  DirectoryObject directoryObject;
  Function<DirectoryObject, DeleteMandate> deleteMandatSupplier;

  Registration copyClickListenerRegistration;
  Registration saveButtonRegistration;
  Registration restButtonRegistration;

  public DirectoryObjectPanelPresenter(AbstractThinclientView thinclientView, ProfilePanel view, DirectoryObject directoryObject) {

    this.thinclientView = thinclientView;
    this.view = view;
    this.directoryObject = directoryObject;

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    view.getDeleteProfileAction().addClickListener(this::handleDeleteAction);
    copyClickListenerRegistration = view.getCopyAction().addClickListener(this::handleCopyAction);
    saveButtonRegistration = view.getSaveButton().addClickListener(this::save);
    restButtonRegistration = view.getResetButton().addClickListener(this::reset);
  }

  public void expandMetaData() {
//    view.getMetaDataItemGroupPanel().expandItems();
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
            window.close();
            UI.getCurrent().removeWindow(window);
            thinclientView.navigateTo(null);
            thinclientView.selectItem(null);
          }));
      content.addComponent(hl);
    }

    UI.getCurrent().addWindow(window);
  }

  public void handleCopyAction(Button.ClickEvent event) {
    // still using LDAP stuff for copying objects
    try {

      // check if name already exists
      String newName = mc.getMessage(UI_PROFILE_PANEL_COPY_TARGET_NAME, directoryObject.getName());
      for (int i = 1; thinclientView.getFreshProfile(newName) != null; i++) {
        newName = mc.getMessage(UI_PROFILE_PANEL_COPY_TARGET_NAME_WITH_NUMBER, i, directoryObject.getName());
      }

      DirectoryObject copy = directoryObject.getClass().newInstance();
      copy.setName(newName);
      copy.setDescription(directoryObject.getDescription());
      copy.setRealm(directoryObject.getRealm());

      // application-group
      if (directoryObject instanceof ApplicationGroup) {
        ApplicationGroup applicationGroup = (ApplicationGroup) directoryObject;
        ApplicationGroup copyApplicationGroup = (ApplicationGroup) copy;
        copyApplicationGroup.setApplications(applicationGroup.getApplications());

      // user
      } else if (directoryObject instanceof User) {
        User user = (User) directoryObject;
        User copyUser = (User) copy;
        copyUser.setUserGroups(user.getUserGroups());
        copyUser.setApplicationGroups(user.getApplicationGroups());
        copyUser.setApplications(user.getApplications());
        copyUser.setPrinters(user.getPrinters());
      }

      thinclientView.save(copy);

      // display
      thinclientView.navigateTo(copy);
      thinclientView.selectItem(copy);
    } catch (Exception e) {
      // TODO: handle exception
      // save failed
      e.printStackTrace();
    }
  }

  public void setItemGroups(List<OtcPropertyGroup> groups) {

    LOGGER.debug("Create properties for " + groups.stream().map(OtcPropertyGroup::getLabel).collect(Collectors.toList()));

    VerticalLayout rows = view.getRows();

    // profile meta data
    OtcPropertyGroup metaData = groups.get(0);
    ItemGroupPanel metaDataIGP = new ItemGroupPanel(metaData);
    ItemGroupPanelPresenter mdIgppGeneral = new ItemGroupPanelPresenter(metaDataIGP);
    mdIgppGeneral.setValuesWrittenConsumer(metaData.getValueWrittenConsumer());
    mdIgppGeneral.applyValuesChangedConsumer(components -> setSaveButtonEnabled(true));
    rows.addComponent(metaDataIGP);

    // profile properties
    OtcPropertyGroup root = groups.get(1);
    // add properties from root group
    if (root.getOtcProperties().size() > 0) { // hässlich-1: nur weil die Schemas keine einheitliche Hirarchie haben
      ItemGroupPanel general = new ItemGroupPanel(root.getOtcProperties());
      ItemGroupPanelPresenter igppGeneral = new ItemGroupPanelPresenter(general);
      igppGeneral.setValuesWrittenConsumer(root.getValueWrittenConsumer());
      igppGeneral.applyValuesChangedConsumer(components -> setSaveButtonEnabled(true));
      rows.addComponent(general);
    }

    root.getGroups().forEach(group -> {
      ItemGroupPanel view = new ItemGroupPanel(group);
      ItemGroupPanelPresenter igpp = new ItemGroupPanelPresenter(view);
      igpp.setValuesWrittenConsumer(group.getValueWrittenConsumer());
      igpp.applyValuesChangedConsumer(components -> setSaveButtonEnabled(true));
      rows.addComponent(view);
    });

  }

  public void setPanelMetaInformation(List<Component> components) {
    view.setPanelMetaInformation(components);
    view.showMetaInformation();
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

  public void setDeleteMandate(Function<DirectoryObject, DeleteMandate> deleteMandatSupplier) {
    this.deleteMandatSupplier = deleteMandatSupplier;
  }

  public void setSaveButtonEnabled(boolean enabled) {
    view.getSaveButton().setEnabled(enabled);
  }

  /**
   * Returns all ItemGroupPanel from the view
   * @return
   */
  public List<ItemGroupPanel> getItemGroupPanels() {
    List<ItemGroupPanel> igpList = new ArrayList<>();
    VerticalLayout rows = view.getRows();
    for(int i = 0; i < rows.getComponentCount(); i++) {
      igpList.add((ItemGroupPanel) rows.getComponent(i));
    }
    return igpList;
  }

  public void setInfo(String message) {
    view.setInfo(message);
  }

  public void setError(String message) {
    view.setError(message);
  }

  // Click listeners for the buttons
  void save(Button.ClickEvent event) {

      view.setInfo("");

    final List<String> errors = new ArrayList<>();
    getItemGroupPanels().forEach(igp -> {
      igp.emptyValidationMessages();
      for (PropertyComponent bc : igp.propertyComponents()) {

        if (bc.getBinder().writeBeanIfValid(bc.getBinder().getBean())) {
          LOGGER.debug("Bean valid " + bc.getBinder().getBean());
        } else {
          BinderValidationStatus<?> validate = bc.getBinder().validate();
          String errorText = validate.getFieldValidationStatuses()
              .stream().filter(BindingValidationStatus::isError)
              .map(BindingValidationStatus::getMessage)
              .map(Optional::get)
              .distinct()
              .collect(Collectors.joining(", "));
          errors.add(errorText);

          OtcProperty bean = (OtcProperty) bc.getBinder().getBean();
          igp.setValidationMessage(bean.getKey(), errorText);
        }
      }
    });

    if (errors.isEmpty()) {
      valuesWrittenConsumer.accept(view);
    } else {
      view.setError(mc.getMessage(UI_COMMON_NOT_SAVED));
    }
  }

  Consumer<ProfilePanel>  valuesWrittenConsumer;
  public void onValuesWritten(Consumer<ProfilePanel> consumer) {
    this.valuesWrittenConsumer = consumer;
  }

  // clear fields by setting null
  void reset(Button.ClickEvent event) {
    view.setInfo("");
    getItemGroupPanels().forEach(igp -> {
      igp.emptyValidationMessages();
      igp.propertyComponents().forEach(propertyComponent -> {
        OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
        bean.getConfiguration().setValue(bean.getInitialValue()); // TODO: JNE initial-value = default aber nicht vorheriger Wert
        propertyComponent.getBinder().readBean(bean);
      });
    });
  }

  public void replaceCopyClickListener(Button.ClickListener bc) {
    this.copyClickListenerRegistration.remove();
    view.getCopyAction().addClickListener(bc);
  }

  public void setDisabledMode() {
    saveButtonRegistration.remove();
    restButtonRegistration.remove();
    view.setDisabledMode();
  }
}
