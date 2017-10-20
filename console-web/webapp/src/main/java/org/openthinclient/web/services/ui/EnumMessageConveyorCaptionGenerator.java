package org.openthinclient.web.services.ui;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.ItemCaptionGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * This class generates captions using the {@link ch.qos.cal10n.MessageConveyor} and an appropriate
 * enum.
 */
public class EnumMessageConveyorCaptionGenerator<T extends Enum, M extends Enum> implements ItemCaptionGenerator<T> {

  private final Map<T, M> captions;
  private final MessageConveyor conveyor;

  public EnumMessageConveyorCaptionGenerator(MessageConveyor conveyor) {
    this.conveyor = conveyor;
    captions = new HashMap<>();
  }

  public void addMapping(T option, M message) {
    captions.put(option, message);
  }

  @Override
  public String apply(T item) {
    final M caption = captions.get(item);
    if (caption != null)
      return conveyor.getMessage(caption);
    return "" + item;
  }
}
