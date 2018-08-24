package org.openthinclient.web.thinclient;

import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import java.util.List;
import java.util.stream.Collectors;
import org.openthinclient.web.thinclient.component.CollapseablePanel;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.ReferencePanel;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ItemGroupPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferenceComponentPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProfilePanel to display and edit all profile-related information
 */
public class ProfilePanel extends Panel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePanel.class);

  private VerticalLayout rows;
  private ReferencePanel panel = null;

  public ProfilePanel(String name, Class clazz) {

    super(name);

    setContent(rows = new VerticalLayout());
    rows.setMargin(false);
    rows.setSpacing(false);

//    setWidth(95, Unit.PERCENTAGE);
    setStyleName("profilePanel");
    addStyleName("formPanel_" + clazz.getSimpleName());

  }

  public void setItemGroups(List<OtcPropertyGroup> groups) {

    LOGGER.debug("Create properties for " + groups.stream().map(OtcPropertyGroup::getLabel).collect(Collectors.toList()));

    OtcPropertyGroup root = groups.get(0);

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

  public ReferenceComponentPresenter addReferences(String label, List<Item> allItems, List<Item> referencedItems) {

    if (panel == null) {
      rows.addComponent(panel = new ReferencePanel());
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


}
