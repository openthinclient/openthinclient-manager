package org.openthinclient.web.thinclient;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.component.CollapseablePanel;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.ReferencePanel;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ItemGroupPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferenceComponentPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProfilePanel to display and edit all profile-related information
 */
public class ProfilePanel extends CssLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePanel.class);

  private VerticalLayout rows;
  private ReferencePanel panel = null;

  private Button editAction;
  private Button copyAction;
  private Button deleteProfileAction;

  private ItemGroupPanel metaDataIGP;


  public ProfilePanel(String name, Class clazz) {

    addStyleName(ValoTheme.LAYOUT_CARD);

    HorizontalLayout panelCaption = new HorizontalLayout();
    panelCaption.addStyleName("v-panel-caption");
    panelCaption.setWidth("100%");
    // panelCaption.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
    Label label = new Label(name);
    panelCaption.addComponent(label);
    panelCaption.setExpandRatio(label, 1);

    editAction = new Button();
    editAction.setIcon(VaadinIcons.PENCIL);
    editAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    editAction.addStyleName(ValoTheme.BUTTON_SMALL);
    editAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    panelCaption.addComponent(editAction);

    copyAction = new Button();
    copyAction.setIcon(VaadinIcons.COPY_O);
    copyAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    copyAction.addStyleName(ValoTheme.BUTTON_SMALL);
    copyAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    panelCaption.addComponent(copyAction);

    deleteProfileAction = new Button();
    deleteProfileAction.setIcon(VaadinIcons.BAN);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
//    deleteProfileAction.addStyleName(ValoTheme.BUTTON_SMALL);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    panelCaption.addComponent(deleteProfileAction);

    addComponent(panelCaption);

    addComponent(rows = new VerticalLayout());
    rows.setMargin(false);
    rows.setSpacing(false);
    setStyleName("profilePanel");
    addStyleName("formPanel_" + clazz.getSimpleName().toLowerCase());

  }

  public void setItemGroups(List<OtcPropertyGroup> groups) {

    LOGGER.debug("Create properties for " + groups.stream().map(OtcPropertyGroup::getLabel).collect(Collectors.toList()));

    // profile meta data
    OtcPropertyGroup metaData = groups.get(0);
    metaDataIGP = new ItemGroupPanel(metaData);
    metaDataIGP.collapseItems();
    ItemGroupPanelPresenter mdIgppGeneral = new ItemGroupPanelPresenter(this, metaDataIGP);
    mdIgppGeneral.setValuesWrittenConsumer(metaData.getValueWrittenConsumer());
    rows.addComponent(metaDataIGP);

    // profile properties
    OtcPropertyGroup root = groups.get(1);
    // default group without sub-groups
    if (root.getOtcProperties().size() > 0) { // hässlich-1: nur weil die Schemas keine einheitliche Hirarchie haben
      ItemGroupPanel general = new ItemGroupPanel(root);
      ItemGroupPanelPresenter igppGeneral = new ItemGroupPanelPresenter(this, general);
      igppGeneral.setValuesWrittenConsumer(root.getValueWrittenConsumer());
      rows.addComponent(general);
    }

    root.getGroups().forEach(group -> {
      ItemGroupPanel view = new ItemGroupPanel(group);
      ItemGroupPanelPresenter igpp = new ItemGroupPanelPresenter(this, view);
      igpp.setValuesWrittenConsumer(group.getValueWrittenConsumer());
      rows.addComponent(view);
    });

  }

  public ReferenceComponentPresenter addReferences(String label, String buttonCaption, List<Item> allItems, List<Item> referencedItems) {

    if (panel == null) {
      rows.addComponent(panel = new ReferencePanel(buttonCaption));
      ReferencePanelPresenter rpp = new ReferencePanelPresenter(this, panel);
    }

    ReferencesComponent rc = new ReferencesComponent(label);
    ReferenceComponentPresenter rcp = new ReferenceComponentPresenter(rc, allItems, referencedItems);
    panel.addComponent(rc);

    return rcp;
  }

  /**
   * Collapse all other item-groups expect the calling itemGroup
   * @param cp CollapseablePanel
   */
  public void handleItemGroupVisibility(CollapseablePanel cp) {
    rows.forEach(component -> {
      CollapseablePanel igp = (CollapseablePanel) component;
      if (!igp.equals(cp)) {
          igp.collapseItems();
      }
    });
  }

  public Button getEditAction() {
    return editAction;
  }

  public Button getCopyAction() {
    return copyAction;
  }

  public Button getDeleteProfileAction() {
    return deleteProfileAction;
  }

  public ItemGroupPanel getMetaDataItemGroupPanel() {
    return metaDataIGP;
  }
}
