package org.openthinclient.pkgmgr.db;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.net.URL;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@Entity
@Table(name = "otc_source")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class Source {

    @Id
    @GeneratedValue
    @XmlTransient
    private Long id;

    @Column(name = "ENABLED")
    @XmlAttribute
    private boolean enabled;
    @Column(name = "DESCRIPTION")
    @Lob
    @XmlElement
    private String description;
    @Column(name = "URL")
    @XmlElement
    private URL url;

    @Column(name = "last_updated")
    @XmlTransient
    private LocalDateTime lastUpdated;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "CHAR(20)")
    @XmlTransient
    private Status status = Status.ENABLED;    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("id", id)
            .append("url", url)
            .append("enabled", enabled)
            .append("status", status)
            .append("lastUpdated", lastUpdated)
            .append("description", description)
            .toString();
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((url == null) ? 0 : url.hashCode());
      return result;
    }

    /**
     * Returns true, if source.id and source.url are equal
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Source other = (Source) obj;
      if (id == null) {
        if (other.id != null)
          return false;
      } else if (!id.equals(other.id))
        return false;
      if (url == null) {
        if (other.url != null)
          return false;
      } else if (!url.equals(other.url))
        return false;
      return true;
    }

    public Status getStatus() {
      return status;
    }

    public void setStatus(Status status) {
      this.status = status;
    }
    
    /**
     * Marks the status of source
     */
    public enum Status {
      /**
       * The source could be enabled
       */
      ENABLED,
      /**
       * The source was deleted, but still exists
       */
      DISABLED
    }
    
}
