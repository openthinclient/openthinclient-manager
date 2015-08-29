package org.openthinclient.webconsole.app;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

/**
* Created by francois on 20.07.14.
*/
public class BrandingComponent extends CssLayout {

  public BrandingComponent() {
    addStyleName("branding");
    Label logo = new Label("<span>QuickTickets</span> Dashboard", ContentMode.HTML);
    logo.setSizeUndefined();
    addComponent(logo);
    // addComponent(new Image(null, new
    // ThemeResource(
    // "img/branding.png")));
  }
}
