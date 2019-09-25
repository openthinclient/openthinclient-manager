package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.model.Item;

import java.util.*;

public class ReferencesComponent extends CssLayout {

  private CssLayout referenceLine;
//  private ComboBox<Item> itemComboBox;
  private Button multiSelectPopupBtn;
  private NavigableMap<String, ItemButtonComponent> itemComponents = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private Map<String, CssLayout> sublineComponents = new HashMap<>();

  public ReferencesComponent(String labelText) {
    addStyleName("referenceComponent");

    multiSelectPopupBtn = new Button();
    multiSelectPopupBtn.addStyleName("multiSelectPopupButton");
    multiSelectPopupBtn.setIcon(VaadinIcons.PLUS_CIRCLE_O);
    multiSelectPopupBtn.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    multiSelectPopupBtn.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);

    CssLayout hl = new CssLayout();
    hl.addStyleName("referenceComponentCaption");
    Label label = new Label(labelText);
    label.addStyleName("referenceLabel");
    hl.addComponents(label, multiSelectPopupBtn);
    addComponent(hl);

    // components
    referenceLine = new CssLayout();
    referenceLine.addStyleName("referenceLine");

    addComponent(referenceLine);

  }

  public ItemButtonComponent addItemComponent(String name, boolean isReadOnly) {
    ItemButtonComponent buttonComponent = new ItemButtonComponent(name, isReadOnly);
    itemComponents.put(name, buttonComponent);
    addComponentSorted(name, buttonComponent);
    return buttonComponent;
  }

  public void removeItemComponent(String name) {
    if (itemComponents.containsKey(name)) {
      referenceLine.removeComponent(itemComponents.remove(name));
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
    addStyleName("has-subline-content");
    CssLayout referenceContentLine = new CssLayout();
    referenceContentLine.addComponents(components);
    referenceContentLine.setStyleName("referenceLine");
    referenceContentLine.addStyleName("subline-content");

    sublineComponents.put(name, referenceContentLine);
    addComponentSorted(name, referenceContentLine);
  }

  /**
   * remove content below reference-line
   * @param name the Label, used for reference line component id
   */
  public void removeReferenceSublineComponent(String name) {
    if (sublineComponents.containsKey(name)) {
      referenceLine.removeComponent(sublineComponents.remove(name));
    }
  }

  private void addComponentSorted(String name, Component component) {
    Map.Entry<String, ItemButtonComponent> nextItemEntry = itemComponents.higherEntry(name);
    if (nextItemEntry != null) {
      referenceLine.addComponent(component, referenceLine.getComponentIndex(nextItemEntry.getValue()));
    } else {
      referenceLine.addComponent(component);
    }
  }
}
