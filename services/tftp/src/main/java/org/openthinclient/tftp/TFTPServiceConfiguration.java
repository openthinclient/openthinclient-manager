package org.openthinclient.tftp;


import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationFile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@ConfigurationFile("tftp.xml")
@XmlRootElement(name = "tftp", namespace = "http://www.openthinclient.org/ns/manager/service/tftp/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class TFTPServiceConfiguration implements Configuration {

    /**
     * Default TFTP Port
     */
    public static final int DEFAULT_TFTP_PORT = 1069;

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

        @XmlAttribute(name = "provider-class")
        private Class<?> providerClass;

        @XmlAttribute(name = "basedir")
        private String basedir;

        @XmlElementWrapper(name = "options")
        @XmlElement(name = "option")
        private List<Option> options = new ArrayList<>();

        public Class<?> getProviderClass() {
            return providerClass;
        }

        public void setProviderClass(Class<?> providerClass) {
            this.providerClass = providerClass;
        }

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

        public List<Option> getOptions() {
            return options;
        }

        public void setOptions(List<Option> options) {
            this.options = options;
        }

        @XmlAccessorType(XmlAccessType.NONE)
        public static class Option {

            @XmlAttribute(name = "name")
            private String name;

            @XmlAttribute(name = "value")
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

        }
    }
}