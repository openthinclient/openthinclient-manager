package org.openthinclient.web.services.ui;

import ch.qos.cal10n.IMessageConveyor;
import com.vaadin.ui.ItemCaptionGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * This class generates captions using the {@link ch.qos.cal10n.IMessageConveyor} and an appropriate
 * enum.
 */
public class EnumMessageConveyorCaptionGenerator<T extends Enum, M extends Enum> implements ItemCaptionGenerator<T> {

  private final Map<T, M> captions;
  private final IMessageConveyor conveyor;

  public EnumMessageConveyorCaptionGenerator(IMessageConveyor conveyor) {
    this.conveyor = conveyor;
    captions = new HashMap<>();
  }

  public EnumMessageConveyorCaptionGenerator<T, M> addMapping(T option, M message) {
    captions.put(option, message);
    return this;
  }

  @Override
  public String apply(T item) {
    final M caption = captions.get(item);
    if (caption != null)
      return conveyor.getMessage(caption);
    return "" + item;
  }
}
