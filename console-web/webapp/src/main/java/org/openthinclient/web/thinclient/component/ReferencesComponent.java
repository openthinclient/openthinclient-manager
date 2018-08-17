package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.model.Item;
import org.vaadin.spring.sidebar.annotation.VaadinFontIcon;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class ReferencesComponent extends  VerticalLayout {

  private CssLayout referenceLine;
  private ComboBox<Item> clientsComboBox;
  private Map<String, CssLayout> itemComponents = new HashMap<>();

  public ReferencesComponent(String labelText) {

      setMargin(false);

      // headline
    Label label = new Label(labelText);
    label.setStyleName("referenceLabel");
    addComponent(label);

    // components
    referenceLine = new CssLayout();
    referenceLine.setStyleName("referenceLine");

    clientsComboBox = new ComboBox<>();
    clientsComboBox.setItemCaptionGenerator(Item::getName);
    clientsComboBox.setEmptySelectionAllowed(false);
    referenceLine.addComponent(clientsComboBox);

    addComponent(referenceLine);

  }

  public ComboBox<Item> getClientsComboBox() {
    return clientsComboBox;
  }

  public CssLayout getReferenceLine() {
    return referenceLine;
  }

  public Button addItemComponent(String name) {

    CssLayout itemComponent = new CssLayout();
    itemComponent.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
    Button disabled = new Button(name);
    disabled.setEnabled(false);
    disabled.setStyleName("referenceItemDisabledButton");
    itemComponent.addComponent(disabled);

    Button itemButton = new Button();
    itemButton.setIcon(VaadinIcons.CLOSE_SMALL);
    itemButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    itemButton.setStyleName("referenceItemIconButton");
    itemComponent.addComponent(itemButton);

    itemComponents.put(name, itemComponent);

    referenceLine.addComponent(itemComponent, referenceLine.getComponentCount() - 1);

    return itemButton;
  }

  public void removeItemComponent(String name) {
    if (itemComponents.containsKey(name)) {
      referenceLine.removeComponent(itemComponents.get(name));
      itemComponents.remove(name);
    }
  }
}