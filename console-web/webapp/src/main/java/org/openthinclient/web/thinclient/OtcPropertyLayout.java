package org.openthinclient.web.thinclient;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import java.util.Arrays;

/**
 *
 */
public class OtcPropertyLayout {

  VerticalLayout rows;

  public OtcPropertyLayout() {

    rows = new VerticalLayout();

  }

  public void addPropertyRow(PropertyComponent component) {
    HorizontalLayout vl = new HorizontalLayout();
    Label label = new Label(component.getLabel());
    label.setWidth(250, Unit.PIXELS);
    vl.addComponent(label);
    vl.addComponent(component.getComponent());
    rows.addComponent(vl);
  }

  public void addComponents(PropertyComponent... components) {
    Arrays.asList(components).forEach(this::addPropertyRow);
  }

  public void addComponent(Component component) {
    rows.addComponent(component);
  }

  public int getComponentCount() {
    return rows.getComponentCount();
  }

  public Component getComponent(int i) {
    return rows.getComponent(i);
  }

  public Component getContent() {
    return rows;
  }
}
