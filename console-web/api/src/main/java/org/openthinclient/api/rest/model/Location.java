package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.openthinclient.api.importer.model.ProfileType;

public class Location extends AbstractProfileObject {
    @JsonProperty
    private Configuration configuration;

    public Location() {
        super(ProfileType.LOCATION);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
