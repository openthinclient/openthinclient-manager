package org.openthinclient.service.update;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * UpdateEntry of install4J updates.xml
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateEntry {

    @XmlAttribute
    private String updatableVersionMin;

    @XmlAttribute
    private String updatableVersionMax;

    @XmlAttribute
    private String newVersion;

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public String getUpdatableVersionMax() {
        return updatableVersionMax;
    }

    public void setUpdatableVersionMax(String updatableVersionMax) {
        this.updatableVersionMax = updatableVersionMax;
    }

    public String getUpdatableVersionMin() {
        return updatableVersionMin;
    }

    public void setUpdatableVersionMin(String updatableVersionMin) {
        this.updatableVersionMin = updatableVersionMin;
    }
}
