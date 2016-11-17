package org.openthinclient.web.ui;

import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

public class ViewHeader extends HorizontalLayout {

  public static final String TITLE_ID = "dashboard-title";
  private final HorizontalLayout tools;
  private final Label titleLabel;

  public ViewHeader() {

    addStyleName("viewheader");
    setSpacing(true);

    titleLabel = new Label();
    titleLabel.setId(TITLE_ID);
    titleLabel.setSizeUndefined();
    titleLabel.addStyleName(ValoTheme.LABEL_H1);
    titleLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);
    addComponent(titleLabel);

    this.tools = new HorizontalLayout();
    this.tools.setSpacing(true);
    this.tools.addStyleName("toolbar");
    addComponent(this.tools);
  }

  public ViewHeader(String title) {
    this();
    setTitle(title);
  }

  public void addTool(Component component) {
    tools.addComponent(component);
  }
  public void addTools(Component... components) {
    tools.addComponents(components);
  }

  public void removeTool(Component component) {
    tools.removeComponent(component);
  }

  public String getTitle() {
    return titleLabel.getValue();
  }

  public void setTitle(String title) {
    titleLabel.setValue(title);
  }
}
