package org.openthinclient.web.thinclient.property;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.web.thinclient.model.ItemConfiguration;

public class OtcMacProperty extends OtcProperty {

  private ItemConfiguration config;
  private UnrecognizedClientService unrecognizedClientService;

  public OtcMacProperty(String label, String tip, String key, String initialValue, String defaultValue, UnrecognizedClientService unrecognizedClientService) {
    super(label, tip, key, initialValue, defaultValue);
    this.unrecognizedClientService = unrecognizedClientService;
  }

  @Override
  public void setConfiguration(ItemConfiguration bean) {
    this.config = bean;
  }

  @Override
  public ItemConfiguration getConfiguration() {
    return config;
  }

  public String getValue() {
    return config.getValue();
  }

  public void setValue(String value) {
    this.config.setValue(value);
  }

  public List<String> getOptions() {
    return unrecognizedClientService.findAll().stream().map(UnrecognizedClient::getMacAddress).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("label", getLabel())
        .append("key", getKey())
        .append("value", getValue())
        .append("initialValue", getInitialValue())
        .append("defaultSchemaValue", getDefaultSchemaValue())
        .toString();
  }
}
