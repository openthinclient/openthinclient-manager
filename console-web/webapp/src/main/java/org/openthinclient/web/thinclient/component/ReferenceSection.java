package org.openthinclient.web.thinclient.component;

import com.vaadin.ui.*;

/**
 * Only container for profile references
 */
public class ReferenceSection extends VerticalLayout implements CollapseablePanel {

  private Label infoLabel;
  private NativeButton head;

  boolean itemsVisible = false;

  public ReferenceSection(String buttonCaption) {

    setMargin(false);

    setStyleName("itemGroupPanel");
    head = new NativeButton(buttonCaption);
    head.setStyleName("headButton");
    head.setSizeFull();
    addComponent(head);

    infoLabel = new Label();

    collapseItems();
  }

  public void collapseItems() {
    itemsVisible = false;
    head.removeStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=1; i<componentCount; i++) {
      getComponent(i).setVisible(false);
    }
  }

  public void expandItems() {
    itemsVisible = true;
    head.addStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=1; i<componentCount; i++) {
      getComponent(i).setVisible(true);
    }
  }

  public void setError(String caption) {
    infoLabel.setVisible(true);
    infoLabel.setCaption(caption);
    infoLabel.removeStyleName("form_success");
    infoLabel.addStyleName("form_error");
  }

  public void setInfo(String caption) {
    infoLabel.setVisible(true);
    infoLabel.setCaption(caption);
    infoLabel.removeStyleName("form_error");
    infoLabel.addStyleName("form_success");
  }

  public boolean isItemsVisible() {
    return itemsVisible;
  }

  public NativeButton getHead() {
    return head;
  }

  public Label getInfoLabel() {
    return infoLabel;
  }

  @Override
  public String toString() {
    return "ReferencePanel: '" + infoLabel.getValue();
  }

  public void handleItemGroupVisibility(ReferenceSection view) {

  }
}
