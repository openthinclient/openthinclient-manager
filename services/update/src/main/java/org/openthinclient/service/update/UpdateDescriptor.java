package org.openthinclient.service.update;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UpdateDescriptor of an install4J updates.xml
 */
@XmlRootElement
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateDescriptor {

    @XmlElement(name = "entry")
    private List<UpdateEntry> entries = new ArrayList<>();

    public List<UpdateEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<UpdateEntry> entries) {
        this.entries = entries;
    }

    /**
     * @return the newVersion-entry of first available entry in UpdateDescriptorList, or null
     */
    public String getNewVersion() {
        if (entries != null && entries.size() > 0) {
            return entries.get(0).getNewVersion();
        }
        return null;
    }
}
