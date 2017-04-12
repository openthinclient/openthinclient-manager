package org.openthinclient.tftp;


import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationFile;
import org.openthinclient.tftp.tftpd.TFTPServer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@ConfigurationFile("tftp.xml")
@XmlRootElement(name = "tftp", namespace = "http://www.openthinclient.org/ns/manager/service/tftp/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class TFTPServiceConfiguration implements Configuration {

    /**
     * Default TFTP Port
     */
    public static final int DEFAULT_TFTP_PORT = TFTPServer.DEFAULT_TFTP_PORT;

    @XmlElement
    private int tftpPort = DEFAULT_TFTP_PORT;

    @XmlElementWrapper(name = "exports")
    @XmlElement(name = "export")
    private List<Export> exports = new ArrayList<>();


    public int getTftpPort() {
        return tftpPort;
    }

    public void setTftpPort(int tftpPort) {
        this.tftpPort = tftpPort;
    }

    public List<Export> getExports() {
        return exports;
    }

    public void setExports(List<Export> exports) {
        this.exports = exports;
    }


    @XmlAccessorType(XmlAccessType.NONE)
    public static class Export {

        @XmlAttribute(name = "prefix")
        private String prefix;

        @XmlAttribute(name = "basedir")
        private String basedir;

        public String getBasedir() {
            return basedir;
        }

        public void setBasedir(String basedir) {
            this.basedir = basedir;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
}