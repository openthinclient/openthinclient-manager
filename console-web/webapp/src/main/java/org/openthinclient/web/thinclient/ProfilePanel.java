package org.openthinclient.web.thinclient;

import com.vaadin.ui.*;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.web.thinclient.component.*;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ItemGroupPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * ProfilePanel to display and edit all profile-related information
 */
public class ProfilePanel extends Panel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePanel.class);

  private VerticalLayout rows;
  private Consumer<ItemGroupPanel> valuesWrittenConsumer;
  private Consumer<ReferencePanel> profileReferenceChanged;


  public ProfilePanel(String name, Class clazz) {

    super(name);

    setContent(rows = new VerticalLayout());
    rows.setMargin(false);
    rows.setSpacing(false);

    setWidth(95, Unit.PERCENTAGE);
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
      igppGeneral.setValuesWrittenConsumer(valuesWrittenConsumer);
      rows.addComponent(general);
    }

    root.getGroups().forEach(group -> {
      ItemGroupPanel view = new ItemGroupPanel(group);
      ItemGroupPanelPresenter igpp = new ItemGroupPanelPresenter(this, view);
      igpp.setValuesWrittenConsumer(valuesWrittenConsumer);
      rows.addComponent(view);
    });

  }

  public void showReferences(Profile profile, List<Item> items) {

    ReferencePanel panel = new ReferencePanel();
    ReferencePanelPresenter rpp = new ReferencePanelPresenter(this, panel, profile, items);
    rpp.setProfileReferenceChangedConsumer(this.profileReferenceChanged);
    rows.addComponent(panel);

  }
  /**
   * This is called by ItemGroupPanel if values are valid and written to model-bean
   * @param consumer called by ItemGroupPanel if values are written to bean
   */
  public void onProfileReferenceChanged(Consumer<ReferencePanel> consumer) {
    this.profileReferenceChanged = consumer;
  }

  /**
   * This is called by ItemGroupPanel if values are valid and written to model-bean
   * @param consumer called by ItemGroupPanel if values are written to bean
   */
  public void onValuesWritten(Consumer<ItemGroupPanel> consumer) {
    this.valuesWrittenConsumer = consumer;
  }

  /**
   * Collapse all other item-groups expect the calling itemGroup
   * @param cp
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
