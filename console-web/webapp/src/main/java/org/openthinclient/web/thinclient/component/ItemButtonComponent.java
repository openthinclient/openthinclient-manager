package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemButtonComponent extends CssLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemButtonComponent.class);
  private Button itemButton;

  public ItemButtonComponent(Item item, boolean isReadOnly) {
    addStyleName("referenceItem");

    Button referencedItemsButton = new Button(item.getName());
    referencedItemsButton.addStyleName(ValoTheme.BUTTON_LINK);
    referencedItemsButton.addStyleName("referenceItemLink");
    referencedItemsButton.addClickListener(e -> {
      Navigator navigator = UI.getCurrent().getNavigator();
      String navigationState = item.getType().name().toLowerCase() + "_view/edit/" + item.getName();
      LOGGER.info("Navigate to " + navigationState);
      navigator.navigateTo(navigationState);
    });
    addComponent(referencedItemsButton);

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
