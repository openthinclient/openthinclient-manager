package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.model.Item;

import java.util.HashMap;
import java.util.Map;

public class ReferencesComponent extends VerticalLayout {

  private VerticalLayout referenceLine;
//  private ComboBox<Item> itemComboBox;
  private Button multiSelectPopupBtn;
  private Map<String, CssLayout> itemComponents = new HashMap<>();

  public ReferencesComponent(String labelText) {

    setMargin(false);

    multiSelectPopupBtn = new Button();
    multiSelectPopupBtn.addStyleName("multiSelectPopupButton");
    multiSelectPopupBtn.setIcon(VaadinIcons.LIST_UL);
    multiSelectPopupBtn.addStyleName(ValoTheme.BUTTON_ICON_ONLY);

      // headline
    HorizontalLayout hl = new HorizontalLayout();
    Label label = new Label(labelText);
    label.setStyleName("referenceLabel");
    hl.addComponents(label, multiSelectPopupBtn);
    addComponent(hl);

    // components
    referenceLine = new VerticalLayout();
    referenceLine.setSpacing(false);
    referenceLine.setMargin(false);
    referenceLine.setStyleName("referenceLine");

    addComponent(referenceLine);

  }

//  public ComboBox<Item> getItemComboBox() {
//    return itemComboBox;
//  }

  public VerticalLayout getReferenceLine() {
    return referenceLine;
  }

  public ItemButtonComponent addItemComponent(String name, boolean isReadOnly) {
    ItemButtonComponent buttonComponent = new ItemButtonComponent(name, isReadOnly);
    itemComponents.put(name, buttonComponent);
    referenceLine.addComponent(buttonComponent, referenceLine.getComponentCount());
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
