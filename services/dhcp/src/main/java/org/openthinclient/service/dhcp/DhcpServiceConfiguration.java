package org.openthinclient.service.dhcp;

import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationFile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@ConfigurationFile("dhcp/service.xml")
@XmlRootElement(name = "dhcp", namespace = "http://www.openthinclient.org/ns/manager/service/dhcp/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class DhcpServiceConfiguration implements Configuration {

  @XmlElement
  private final PXE pxe = new PXE();
  @XmlElement(name = "track-unrecognized-clients")
  private boolean trackUnrecognizedPXEClients = true;

  public PXE getPxe() {
    return pxe;
  }

  public boolean isTrackUnrecognizedPXEClients() {
    return trackUnrecognizedPXEClients;
  }

  public void setTrackUnrecognizedPXEClients(boolean trackUnrecognizedPXEClients) {
    this.trackUnrecognizedPXEClients = trackUnrecognizedPXEClients;
  }

  @Override
  public String toString() {
    return "DhcpServiceConfiguration{" +
            "pxe=" + pxe +
            ", trackUnrecognizedPXEClients=" + trackUnrecognizedPXEClients +
            '}';
  }

  public enum PXEType {
    @XmlEnumValue("eavesdropping")
    EAVESDROPPING,
    @XmlEnumValue("single-homed-broadcast")
    SINGLE_HOMED_BROADCAST,
    @XmlEnumValue("single-homed")
    SINGLE_HOMED,
    @XmlEnumValue("bind-to-address")
    BIND_TO_ADDRESS,
    @XmlEnumValue("auto")
    AUTO

  }

  public enum PXEPolicy {
    @XmlEnumValue("any-client")
    ANY_CLIENT,
    @XmlEnumValue("only-configured")
    ONLY_CONFIGURED
  }

  @XmlType
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class PXE {
    @XmlElement(name = "type")
    private PXEType type = PXEType.AUTO;

    @XmlElement
    private PXEPolicy policy = PXEPolicy.ONLY_CONFIGURED;

    public PXEPolicy getPolicy() {
      return policy;
    }

    public void setPolicy(PXEPolicy policy) {
      this.policy = policy;
    }

    public PXEType getType() {
      return type;
    }

    public void setType(PXEType type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return "PXE{" +
              "type=" + type +
              ", policy=" + policy +
              '}';
    }
  }
}
