package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.model.Item;

import java.util.HashMap;
import java.util.Map;

public class ReferencesComponent extends VerticalLayout {

  private CssLayout referenceLine;
  private ComboBox<Item> itemComboBox;
  private Button multiSelectPopupBtn;
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

    itemComboBox = new ComboBox<>();
    itemComboBox.setItemCaptionGenerator(Item::getName);
    itemComboBox.setEmptySelectionAllowed(false);
    referenceLine.addComponent(itemComboBox);

    multiSelectPopupBtn = new Button();
    multiSelectPopupBtn.addStyleName("multiSelectPopupButton");
    multiSelectPopupBtn.setIcon(VaadinIcons.LIST_UL);
    multiSelectPopupBtn.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    referenceLine.addComponent(multiSelectPopupBtn);

    addComponent(referenceLine);

  }

  public ComboBox<Item> getItemComboBox() {
    return itemComboBox;
  }

  public CssLayout getReferenceLine() {
    return referenceLine;
  }

  public ItemButtonComponent addItemComponent(String name) {
    ItemButtonComponent buttonComponent = new ItemButtonComponent(name);
    itemComponents.put(name, buttonComponent);
    referenceLine.addComponent(buttonComponent, referenceLine.getComponentCount() - 2);
    return buttonComponent;
  }

  public void removeItemComponent(String name) {
    if (itemComponents.containsKey(name)) {
      referenceLine.removeComponent(itemComponents.get(name));
      itemComponents.remove(name);
    }
  }

  public Button getMultiSelectPopupBtn() {
    return multiSelectPopupBtn;
  }

  /**
   * add custom content below reference-line
   * @param name a Label for reference line
   * @param components additional components
   */
  public void addReferenceSublineComponents(String name, Component... components) {

    CssLayout referenceContentLine = new CssLayout();
    referenceContentLine.setId(name);
    referenceContentLine.setStyleName("referenceLine");
    referenceContentLine.addStyleName("subline-content");
    addComponent(referenceContentLine);

    referenceContentLine.addComponent(new Label(name));
    referenceContentLine.addComponents(components);
  }

  /**
   * remove content below reference-line
   * @param name the Label, used for reference line component id
   */
  public void removeReferenceSublineComponent(String name) {
    int componentCount = getComponentCount();
    for (int i=0; i<componentCount; i++) {
      Component component = getComponent(i);
      if (component.getId() != null && component.getId().equals(name)) {
        removeComponent(component);
        break;
      }
    }
  }
}
