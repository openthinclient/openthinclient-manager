package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ReferencesComponentPresenter;
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
  private VerticalLayout panelMetaInformation;
//  private Button editAction;
  private Button copyAction;
  private Button deleteProfileAction;

  private ItemGroupPanel metaDataIGP;
  IMessageConveyor mc;


  public ProfilePanel(String name, Class clazz) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    addStyleName(ValoTheme.LAYOUT_CARD);

    panelCaption = new CssLayout();
    panelCaption.addStyleName("settings-caption");
    // panelCaption.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
    Label label = new Label(name);
    panelCaption.addComponent(label);

//    editAction = new Button();
//    editAction.setIcon(VaadinIcons.PENCIL);
//    editAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
//    editAction.addStyleName(ValoTheme.BUTTON_SMALL);
//    editAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
//    panelCaption.addComponent(editAction);

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
//    deleteProfileAction.addStyleName(ValoTheme.BUTTON_SMALL);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    panelCaption.addComponent(deleteProfileAction);

    addComponent(panelCaption);

    addComponent(panelMetaInformation = new VerticalLayout());
    panelMetaInformation.setVisible(false);
    panelMetaInformation.setMargin(false);
    panelMetaInformation.setSpacing(false);
    panelMetaInformation.addStyleName("panelMetaInformation");

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

//  public void setItemGroups(List<OtcPropertyGroup> groups) {

//    LOGGER.debug("Create properties for " + groups.stream().map(OtcPropertyGroup::getLabel).collect(Collectors.toList()));
//
//    // profile meta data
//    OtcPropertyGroup metaData = groups.get(0);
//    metaDataIGP = new ItemGroupPanel(metaData);
////    metaDataIGP.collapseItems();
//    ItemGroupPanelPresenter mdIgppGeneral = new ItemGroupPanelPresenter(this, metaDataIGP);
//    mdIgppGeneral.setValuesWrittenConsumer(metaData.getValueWrittenConsumer());
//    rows.addComponent(metaDataIGP);
//
//    // profile properties
//    OtcPropertyGroup root = groups.get(1);
//    // default group without sub-groups
//    if (root.getOtcProperties().size() > 0) { // hässlich-1: nur weil die Schemas keine einheitliche Hirarchie haben
//      ItemGroupPanel general = new ItemGroupPanel(root);
//      ItemGroupPanelPresenter igppGeneral = new ItemGroupPanelPresenter(this, general);
//      igppGeneral.setValuesWrittenConsumer(root.getValueWrittenConsumer());
//      rows.addComponent(general);
//    }
//
//    root.getGroups().forEach(group -> {
//      ItemGroupPanel view = new ItemGroupPanel(group);
//      ItemGroupPanelPresenter igpp = new ItemGroupPanelPresenter(this, view);
//      igpp.setValuesWrittenConsumer(group.getValueWrittenConsumer());
//      rows.addComponent(view);
//    });
//
//    rows.addComponent(buildActionsBar());
//  }

  // ---

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

    // there are property-groups without properties in schema, we don't need actions bars there
//    if (propertyComponents.size() == 0) {
//      save.setVisible(false);
//      reset.setVisible(false);
//    }
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

  /**
   * Display a set of Properties as Meta-Information at ProfilePanel
   * @param components list of Component
   */
  public void setPanelMetaInformation(List<Component> components) {
    panelMetaInformation.addComponents(components.toArray(new Component[]{}));
  }

  public Button getCopyAction() {
    return copyAction;
  }

  public Button getDeleteProfileAction() {
    return deleteProfileAction;
  }

  public void showMetaInformation() {
//    panelMetaInformation.setVisible(true);
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
