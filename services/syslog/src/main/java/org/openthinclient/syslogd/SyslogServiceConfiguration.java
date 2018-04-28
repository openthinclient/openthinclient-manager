package org.openthinclient.syslogd;

import org.openthinclient.service.common.ServiceConfiguration;
import org.openthinclient.service.common.home.ConfigurationFile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@ConfigurationFile("syslog/service.xml")
@XmlRootElement(name = "directory", namespace = "http://www.openthinclient.org/ns/manager/service/syslog/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class SyslogServiceConfiguration implements ServiceConfiguration {

    /**
     * Default Syslog Port
     */
    public static final int DEFAULT_SYSLOG_PORT = 0;
  
    @XmlElement
    private int syslogPort = DEFAULT_SYSLOG_PORT;

	public int getSyslogPort() {
		return syslogPort;
	}

	public void setSyslogPort(int syslogPort) {
		this.syslogPort = syslogPort;
	}
	
}
