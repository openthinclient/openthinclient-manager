package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "subtype",
        "name",
        "description"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
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
        if(configuration == null)
            configuration = new Configuration();
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
