package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

/**
 * ProfilePanel to display and edit all profile-related information
 */
public class ProfilePanel extends CssLayout {

  private VerticalLayout rows;

  private CssLayout panelCaption;
  private AbstractLayout panelButtons;
  private Button copyAction;
  private Button deleteProfileAction;
  private Button contextInfoButton;
  private Label contextInfoLabel;

  IMessageConveyor mc;


  public ProfilePanel(String name, Class<? extends DirectoryObject> clazz) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    addStyleName(ValoTheme.LAYOUT_CARD);

    panelCaption = new CssLayout();
    panelCaption.addStyleName("settings-caption");

    Label label = new Label(name);
    panelCaption.addComponent(label);

    panelButtons = new CssLayout();
    panelButtons.addStyleName("panelButtons");
    panelCaption.addComponent(panelButtons);

    copyAction = addPanelButton(VaadinIcons.COPY_O, UI_PROFILE_PANEL_BUTTON_ALT_TEXT_COPY);

    deleteProfileAction = addPanelButton(VaadinIcons.TRASH, UI_PROFILE_PANEL_BUTTON_ALT_TEXT_DELETE);

    contextInfoButton = addPanelButton(null);
    contextInfoButton.addStyleName("context-info-button");
    contextInfoButton.setVisible(false);

    contextInfoLabel = new Label(null, ContentMode.HTML);
    contextInfoLabel.addStyleName("context-info-label");
    panelCaption.addComponent(contextInfoLabel);

    addComponent(panelCaption);

    addComponent(rows = new VerticalLayout());
    rows.setMargin(false);
    rows.setSpacing(false);
    rows.setStyleName("panelRows");
    setStyleName("profilePanel");
    addStyleName("formPanel_" + clazz.getSimpleName().toLowerCase());

    addComponent(createActionsBar());
  }

  public Button addPanelButton(Resource icon) {
    return addPanelButton(icon, null);
  }

  public Button addPanelButton(Resource icon, ConsoleWebMessages description) {
    Button button = new Button();
    if(description != null) {
      button.setDescription(mc.getMessage(description));
    }
    button.setIcon(icon);
    button.addStyleNames(ValoTheme.BUTTON_BORDERLESS_COLORED, ValoTheme.BUTTON_ICON_ONLY);
    panelCaption.addComponent(button);
    return button;
  }

  public void addPanelCaptionComponent(Component component) {
    panelButtons.addComponent(component);
  }

  private Label infoLabel;
  private NativeButton save;
  private NativeButton reset;

  private HorizontalLayout createActionsBar() {

    // Button bar
    save = new NativeButton(mc.getMessage(UI_BUTTON_SAVE));
    save.addStyleName("profile_save");
    save.setEnabled(false);

    reset = new NativeButton(mc.getMessage(UI_BUTTON_RESET));
    reset.addStyleName("profile_reset");
    infoLabel = new Label();
    infoLabel.setCaption("");
    infoLabel.setVisible(true);
    infoLabel.setStyleName("propertyLabel");
    infoLabel.addStyleName("itemGroupInfoLabel");

    HorizontalLayout actions = new HorizontalLayout();
    actions.setSizeFull();
    actions.addComponents(reset, save);

    HorizontalLayout proprow = new HorizontalLayout();
    proprow.setStyleName("property-action");
    proprow.addComponent(infoLabel);
    proprow.addComponent(actions);
    return proprow;

  }

  public void setError(String caption) {
    infoLabel.setCaption(caption);
    infoLabel.removeStyleName("form_success");
    infoLabel.addStyleName("form_error");
    infoLabel.setVisible(true);
  }

  public void setInfo(String caption) {
    infoLabel.setCaption(caption);
    infoLabel.removeStyleName("form_error");
    infoLabel.addStyleName("form_success");
    infoLabel.setVisible(true);
  }

  public void setContextInfo(String tip) {
    contextInfoLabel.setValue(tip);
    contextInfoButton.setVisible(tip != null);
  }

  public Button getCopyAction() {
    return copyAction;
  }

  public Button getDeleteProfileAction() {
    return deleteProfileAction;
  }

  public VerticalLayout getRows() {
    return rows;
  }

  public Button getSaveButton() {
    return save;
  }

  public Button getResetButton() {
    return reset;
  }

  public void setDisabledMode() {
    save.setEnabled(false);
    reset.setEnabled(false);
  }
}
