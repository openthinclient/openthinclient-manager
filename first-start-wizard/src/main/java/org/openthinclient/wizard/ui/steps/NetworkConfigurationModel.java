package org.openthinclient.wizard.ui.steps;

import com.vaadin.data.Item;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.data.util.BeanItem;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;

public class NetworkConfigurationModel {

  public final EnumStateProperty directConnectionProperty = new EnumStateProperty(Type.DIRECT);
  public final EnumStateProperty proxyConnectionProperty = new EnumStateProperty(Type.PROXY);
  public final EnumStateProperty noConnectionProperty = new EnumStateProperty(Type.DISABLED);
  private Type type = Type.DIRECT;
  private BeanItem<NetworkConfiguration.ProxyConfiguration> proxyConfigurationItem = new BeanItem<>(new NetworkConfiguration.ProxyConfiguration());


  public Item getProxyConfiguration() {
    return proxyConfigurationItem;
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
    }


    @Override
    public Class<? extends Boolean> getType() {
      return Boolean.class;
    }
  }
}
