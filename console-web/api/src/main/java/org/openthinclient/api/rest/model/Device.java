package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.openthinclient.api.importer.model.ProfileType;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Device extends AbstractProfileObject {

  /** set of uniquemember */
  private Set<String> members = new HashSet<>();

  public Device() {
    super(ProfileType.DEVICE);
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
