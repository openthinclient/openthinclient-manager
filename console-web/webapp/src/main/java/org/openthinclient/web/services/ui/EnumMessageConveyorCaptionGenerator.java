package org.openthinclient.web.services.ui;

import org.vaadin.viritin.fields.AbstractCaptionGenerator;

import java.util.HashMap;
import java.util.Map;

import ch.qos.cal10n.MessageConveyor;

/**
 * This class generates captions using the {@link ch.qos.cal10n.MessageConveyor} and an appropriate
 * enum.
 */
public class EnumMessageConveyorCaptionGenerator<T extends Enum, M extends Enum> extends AbstractCaptionGenerator<T> {

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
  public String generateCaption(T option) {

    final M caption = captions.get(option);
    if (caption != null)
      return conveyor.getMessage(caption);
    return "" + option;

  }
}
