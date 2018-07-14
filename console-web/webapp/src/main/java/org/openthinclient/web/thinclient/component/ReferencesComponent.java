package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.model.Item;
import org.vaadin.spring.sidebar.annotation.VaadinFontIcon;

import javax.swing.*;

public class ReferencesComponent extends  VerticalLayout {

  private HorizontalLayout referenceLine;
  private ComboBox<Item> clientsComboBox;

  public ReferencesComponent(String labelText) {

      setMargin(false);

      // headline
    Label label = new Label(labelText);
    label.setStyleName("referenceLabel");
    addComponent(label);

    // components
    referenceLine = new HorizontalLayout();
    referenceLine.setStyleName("referenceLine");

    clientsComboBox = new ComboBox<>();
    clientsComboBox.setItemCaptionGenerator(Item::getName);
    clientsComboBox.setEmptySelectionAllowed(true);
    referenceLine.addComponent(clientsComboBox);

    addComponent(referenceLine);

  }

  public ComboBox<Item> getClientsComboBox() {
    return clientsComboBox;
  }

  public HorizontalLayout getReferenceLine() {
    return referenceLine;
  }

  public void addItemComponent(String name) {

    CssLayout components = new CssLayout();
    components.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
    Button disabled = new Button(name);
    disabled.setEnabled(false);
    disabled.setStyleName("referenceItemDisabledButton");
    components.addComponent(disabled);

    Button itemButton = new Button();
    itemButton.setIcon(VaadinIcons.CLOSE_SMALL);
    itemButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    itemButton.setStyleName("referenceItemIconButton");
    components.addComponent(itemButton);

    referenceLine.addComponent(components, referenceLine.getComponentCount() - 1);
  }
}
