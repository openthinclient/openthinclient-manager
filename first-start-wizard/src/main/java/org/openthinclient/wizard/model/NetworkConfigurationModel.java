package org.openthinclient.wizard.model;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.data.util.BeanItem;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;

public class NetworkConfigurationModel {

  private final EnumStateProperty directConnectionProperty = new EnumStateProperty(Type.DIRECT);
  private final EnumStateProperty proxyConnectionProperty = new EnumStateProperty(Type.PROXY);
  private final EnumStateProperty noConnectionProperty = new EnumStateProperty(Type.DISABLED);
  private final BeanItem<NetworkConfiguration.ProxyConfiguration> proxyConfigurationItem = new BeanItem<>(new NetworkConfiguration.ProxyConfiguration());
  private Type type = Type.DIRECT;

  public NetworkConfigurationModel() {
    getProxyConfiguration().setHost("proxyhost");
    getProxyConfiguration().setPort(80);
  }

  public Item getProxyConfigurationItem() {
    return proxyConfigurationItem;
  }

  public NetworkConfiguration.ProxyConfiguration getProxyConfiguration() {
    return proxyConfigurationItem.getBean();
  }

  public Property<Boolean> getDirectConnectionProperty() {
    return directConnectionProperty;
  }

  public Property<Boolean> getProxyConnectionProperty() {
    return proxyConnectionProperty;
  }

  public Property<Boolean> getNoConnectionProperty() {
    return noConnectionProperty;
  }

  public enum Type {
    DISABLED,
    PROXY,
    DIRECT
  }

  private class EnumStateProperty extends AbstractProperty<Boolean> {
    private final Type ourType;

    public EnumStateProperty(Type type) {
      this.ourType = type;
    }

    @Override
    public Boolean getValue() {
      return type == ourType;
    }

    @Override
    public void setValue(Boolean newValue) throws ReadOnlyException {
      type = ourType;

      // FIXME this is a fast way to realize this. It would be nicer to have a more mature model
      directConnectionProperty.fireValueChange();
      proxyConnectionProperty.fireValueChange();
      noConnectionProperty.fireValueChange();

      // speciality: if a proxy configuration shall be used, enable the appropriate setting on the proxy configuration bean
      getProxyConfiguration().setEnabled(proxyConnectionProperty.getValue());
    }


    @Override
    public Class<? extends Boolean> getType() {
      return Boolean.class;
    }
  }
}
