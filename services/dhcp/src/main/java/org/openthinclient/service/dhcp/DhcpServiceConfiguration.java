package org.openthinclient.service.dhcp;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationFile;


@ConfigurationFile("dhcp/service.xml")
@XmlRootElement(name = "directory", namespace = "http://www.openthinclient.org/ns/manager/service/dhcp/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class DhcpServiceConfiguration implements Configuration {


}
