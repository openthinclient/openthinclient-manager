package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Location extends AbstractProfileObject {
    @JsonProperty
    private Configuration configuration;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
