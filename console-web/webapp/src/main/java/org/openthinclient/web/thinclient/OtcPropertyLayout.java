package org.openthinclient.web.thinclient;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class OtcPropertyLayout {

  VerticalLayout rows;
  List<PropertyComponent> propertyComponents = new ArrayList();

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

    propertyComponents.add(component);
  }

  public void addComponents(PropertyComponent... components) {
    Arrays.asList(components).forEach(this::addPropertyRow);
  }

  public void addComponent(Component component) {
    rows.addComponent(component);
  }

  public int getComponentCount() {
    return propertyComponents.size();
  }

  public Component getComponent(int i) {
    return propertyComponents.get(i).getComponent();
  }

  public Component getContent() {
    return rows;
  }
}
