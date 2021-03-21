package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.model.Item;

import java.util.*;

public class ReferencesComponent extends CssLayout {

  private CssLayout referenceLine;
  private Button referenceComponentCaption;
  private NavigableMap<String, ItemButtonComponent> itemComponents = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private Map<String, CssLayout> sublineComponents = new HashMap<>();

  public ReferencesComponent(String labelText) {
    addStyleName("referenceComponent");

    referenceComponentCaption = new Button(labelText);
    referenceComponentCaption.addStyleNames("referenceComponentCaption");
    referenceComponentCaption.setIcon(VaadinIcons.COG_O);
    referenceComponentCaption.addStyleName(ValoTheme.BUTTON_BORDERLESS);
    addComponent(referenceComponentCaption);

    // components
    referenceLine = new CssLayout();
    referenceLine.addStyleName("referenceLine");

    addComponent(referenceLine);

  }

  public ItemButtonComponent addItemComponent(Item item) {
    ItemButtonComponent buttonComponent = new ItemButtonComponent(item);
    itemComponents.put(item.getName(), buttonComponent);
    addComponentSorted(item.getName(), buttonComponent);
    return buttonComponent;
  }

  public void removeItemComponent(String name) {
    if (itemComponents.containsKey(name)) {
      referenceLine.removeComponent(itemComponents.remove(name));
    }
  }

  public Button getMultiSelectPopupBtn() {
    return referenceComponentCaption;
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
