package org.openthinclient.manager.util.http.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class NetworkConfiguration {

  @XmlElement
  private boolean disabled;

  @XmlElement
  private ProxyConfiguration proxy;

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public ProxyConfiguration getProxy() {
    return proxy;
  }

  public void setProxy(ProxyConfiguration proxy) {
    this.proxy = proxy;
  }

  @XmlType
  @XmlAccessorType(XmlAccessType.NONE)
  public static class ProxyConfiguration implements Cloneable {

    @XmlElement(name="port")
    private int port;
    @XmlElement(name="user")
    private String user;
    @XmlElement(name="password")
    private String password;
    @XmlElement(name="host")
    private String host;

    @XmlAttribute(name="enabled")
    private boolean enabled;

    public void setPort(int port) {
      this.port = port;
    }

    public int getPort() {
      return port;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getUser() {
      return user;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPassword() {
      return password;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getHost() {
      return host;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public ProxyConfiguration clone() {
      try {
        return (ProxyConfiguration) super.clone();
      } catch (CloneNotSupportedException e) {
        // won't happen as we're implementing Cloneable.
        return null;
      }
    }
  }
}
