package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractProfileObject {

    @JsonProperty
    private String description;
    @JsonProperty
    private String name;
    @JsonProperty
    private String subtype;
    @JsonProperty
    private Configuration configuration;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
