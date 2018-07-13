package org.openthinclient.web.thinclient.component;

import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.*;
import org.openthinclient.common.model.Client;
import org.openthinclient.web.filebrowser.FileBrowserView;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manage profile references
 */
public class ReferencePanel extends VerticalLayout implements CollapseablePanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencePanel.class);

  private Label infoLabel;
  private NativeButton head;
  private HorizontalLayout referenceLine;
  private ComboBox<Item> clientsComboBox;

  boolean itemsVisible = false;

  public ReferencePanel() {

    setMargin(false);

    setStyleName("itemGroupPanel");
    head = new NativeButton("Zuordnung");
    head.setStyleName("headButton");
    head.setSizeFull();
    addComponent(head);

    infoLabel = new Label();

    addComponent(buildThinclients());

    collapseItems();
  }

  private Component buildThinclients() {

    VerticalLayout vl = new VerticalLayout();
    vl.setMargin(false);

    // headline
    vl.addComponent(new Label("Thinclients"));
    // components
    referenceLine = new HorizontalLayout();
    referenceLine.addComponent(new Button("Mister X"));
    referenceLine.addComponent(new Button("Mister Ypsilon"));

    clientsComboBox = new ComboBox<>();
    clientsComboBox.setPlaceholder("Find client");
    clientsComboBox.setItemCaptionGenerator(Item::getName);
    clientsComboBox.setEmptySelectionAllowed(false);
    referenceLine.addComponent(clientsComboBox);
    
    vl.addComponent(referenceLine);

    return vl;
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


  public ComboBox<Item> getClientsComboBox() {
    return clientsComboBox;
  }

  public HorizontalLayout getReferenceLine() {
    return referenceLine;
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
}
