package org.openthinclient.syslogd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationFile;

@ConfigurationFile("syslog/service.xml")
@XmlRootElement(name = "directory", namespace = "http://www.openthinclient.org/ns/manager/service/syslog/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class SyslogServiceConfiguration implements Configuration {

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
