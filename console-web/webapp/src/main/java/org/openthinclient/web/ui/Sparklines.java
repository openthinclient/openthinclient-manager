package org.openthinclient.web.ui;

import com.vaadin.server.Responsive;
import com.vaadin.ui.CssLayout;

public class Sparklines extends CssLayout {
  public Sparklines() {
    addStyleName("sparks");
    setWidth("100%");
    Responsive.makeResponsive(this);
  }
}
