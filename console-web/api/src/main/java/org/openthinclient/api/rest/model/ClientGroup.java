package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openthinclient.api.importer.model.ProfileType;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientGroup extends AbstractProfileObject {

    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<String, Object>();
    /** set of uniquemember */
    private Set<String> members = new HashSet<>();

    /**
     * (Required)
     */
    @JsonProperty("id")
    @NotNull
    private String id;

    public ClientGroup() {
        super(ProfileType.CLIENT_GROUP);
    }

    /**
     * (Required)
     *
     * @return The id
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * (Required)
     *
     * @param id The id
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public ClientGroup withId(String id) {
        this.id = id;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void addMember(String member) {
        this.members.add(member);
    }

    public Set<String> getMembers() {
        return members;
    }

    public void addMembers(Set<String> members) {
        this.members.addAll(members);
    }
}
