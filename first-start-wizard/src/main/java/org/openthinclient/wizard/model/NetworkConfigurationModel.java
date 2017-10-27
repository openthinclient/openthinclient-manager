package org.openthinclient.wizard.model;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;

public class NetworkConfigurationModel {

  private final EnumStateProperty directConnectionProperty = new EnumStateProperty(Type.DIRECT);
  private final EnumStateProperty proxyConnectionProperty = new EnumStateProperty(Type.PROXY);
  private final EnumStateProperty noConnectionProperty = new EnumStateProperty(Type.DISABLED);
  private final NetworkConfiguration.ProxyConfiguration proxyConfigurationItem = new NetworkConfiguration.ProxyConfiguration();
  private Type type = Type.DIRECT;

  public NetworkConfigurationModel() {
    getProxyConfiguration().setPort(80);
  }

  public NetworkConfiguration.ProxyConfiguration getProxyConfiguration() {
    return proxyConfigurationItem;
  }

  public Boolean getDirectConnectionProperty() {
    return directConnectionProperty.getValue();
  }

  public void enableDirectConnectionProperty() {
    directConnectionProperty.setValue(true);
  }

  public Boolean getProxyConnectionProperty() {
    return proxyConnectionProperty.getValue();
  }

  public void enableProxyConnectionProperty() {
    proxyConnectionProperty.setValue(true);
  }

  public Boolean getNoConnectionProperty() {
    return noConnectionProperty.getValue();
  }

  public enum Type {
    DISABLED,
    PROXY,
    DIRECT
  }

  private class EnumStateProperty  {
    private final Type ourType;

    public EnumStateProperty(Type type) {
      this.ourType = type;
    }

    public Boolean getValue() {
      return type == ourType;
    }

    public void setValue(Boolean newValue) {
      type = ourType;

      // FIXME this is a fast way to realize this. It would be nicer to have a more mature model
//      directConnectionProperty.fireValueChange();
//      proxyConnectionProperty.fireValueChange();
//      noConnectionProperty.fireValueChange();

      // speciality: if a proxy configuration shall be used, enable the appropriate setting on the proxy configuration bean
      getProxyConfiguration().setEnabled(proxyConnectionProperty.getValue());
    }
  }
}
