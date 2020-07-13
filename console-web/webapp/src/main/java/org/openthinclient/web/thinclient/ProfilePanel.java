package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

/**
 * ProfilePanel to display and edit all profile-related information
 */
public class ProfilePanel extends CssLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePanel.class);

  private VerticalLayout rows;

  private CssLayout panelCaption;
  private Button copyAction;
  private Button deleteProfileAction;

  IMessageConveyor mc;


  public ProfilePanel(String name, Class clazz) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    addStyleName(ValoTheme.LAYOUT_CARD);

    panelCaption = new CssLayout();
    panelCaption.addStyleName("settings-caption");
    Label label = new Label(name);
    panelCaption.addComponent(label);

    copyAction = new Button();
    copyAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_COPY));
    copyAction.setIcon(VaadinIcons.COPY_O);
    copyAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    copyAction.addStyleName(ValoTheme.BUTTON_SMALL);
    copyAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    panelCaption.addComponent(copyAction);

    deleteProfileAction = new Button();
    deleteProfileAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_DELETE));
    deleteProfileAction.setIcon(VaadinIcons.TRASH);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    panelCaption.addComponent(deleteProfileAction);

    addComponent(panelCaption);


    addComponent(rows = new VerticalLayout());
    rows.setMargin(false);
    rows.setSpacing(false);
    rows.setStyleName("panelRows");
    setStyleName("profilePanel");
    addStyleName("formPanel_" + clazz.getSimpleName().toLowerCase());

    addComponent(createActionsBar());

  }

  public void addPanelCaptionComponent(Component component) {
    panelCaption.addComponent(component, panelCaption.getComponentCount() - 2);
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
