package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

public class ItemButtonComponent extends CssLayout {

  Button itemButton;

  public ItemButtonComponent(String name, boolean isReadOnly) {
    addStyleName("referenceItem");
    addComponent(new Label(name));

    if (!isReadOnly) {
      itemButton = new Button();
      itemButton.setIcon(VaadinIcons.CLOSE_CIRCLE_O);
      itemButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      itemButton.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
      itemButton.addStyleName("referenceItemIconButton");
      itemButton.setVisible(false);
      addComponent(itemButton);
    }
  }

  public void addClickListener(Button.ClickListener clickListener) {
    if(itemButton != null) {
      itemButton.setVisible(true);
      itemButton.addClickListener(clickListener);
    }
  }
}
