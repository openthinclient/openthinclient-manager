package org.openthinclient.web.thinclient.presenter;

import org.openthinclient.web.thinclient.component.ItemGroupPanel;

import java.util.function.Consumer;

public class ItemGroupPanelPresenter {

  private ItemGroupPanel view;

  public ItemGroupPanelPresenter(ItemGroupPanel view) {
    this.view = view;
  }

  public void applyValuesChangedConsumer(Consumer<ItemGroupPanel> valueChangedConsumer) {
    view.propertyComponents().forEach(propertyComponent ->
        propertyComponent.getBinder().addValueChangeListener(e -> valueChangedConsumer.accept(view))
    );
  }
}
