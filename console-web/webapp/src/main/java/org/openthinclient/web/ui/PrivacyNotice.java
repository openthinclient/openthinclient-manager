package org.openthinclient.web.ui;

import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "privacy-notice", namespace = PrivacyNotice.NAMESPACE)
public class PrivacyNotice {
  public static final String NAMESPACE = "http://www.openthinclient.org/ns/manager/privacy-notice/1.0";

  private final ManagerHomeMetadata metadata = (new ManagerHomeFactory()).create().getMetadata();

  @XmlAttribute(name = "version")
  private int version = 0;
  @XmlElement(name = "de")
  private String de;
  @XmlElement(name = "en")
  private String en;

  public static PrivacyNotice load() {
    return JAXB.unmarshal(PrivacyNotice.class.getResourceAsStream("/privacynotice.xml"),
                          PrivacyNotice.class);
  }

  public String get(String lang) {
    if(lang != null && lang.equals("de")) {
      return de;
    } else {
      return en;
    }
  }

  public boolean isAcknowledged() {
    return (version <= metadata.getAcknowledgedPrivacyNoticeVersion());
  }

  public void setAcknowledged() {
    if(!isAcknowledged()) {
      metadata.setAcknowledgedPrivacyNoticeVersion(version);
      metadata.save();
    }
  }
}
