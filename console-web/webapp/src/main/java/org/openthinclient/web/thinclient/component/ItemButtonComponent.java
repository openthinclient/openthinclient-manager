package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ItemButtonComponent extends CssLayout {

  Button itemButton;

  public ItemButtonComponent(String name, boolean isReadOnly) {

    addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
    Button disabled = new Button(name);
    disabled.setEnabled(false);
    disabled.setStyleName("referenceItemDisabledButton");
    addComponent(disabled);

    itemButton = new Button();
    itemButton.setIcon(VaadinIcons.TRASH);
    itemButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    itemButton.setStyleName("referenceItemIconButton");
    itemButton.setVisible(false);
    if (!isReadOnly) {
      addComponent(itemButton);
    }

  }

  public void addClickListener(Button.ClickListener clickListener) {
    itemButton.setVisible(true);
    itemButton.addClickListener(clickListener);
  }
}
