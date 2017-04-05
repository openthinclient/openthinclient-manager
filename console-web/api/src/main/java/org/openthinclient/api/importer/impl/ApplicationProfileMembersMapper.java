package org.openthinclient.api.importer.impl;

import org.openthinclient.common.model.ClientGroup;
import org.openthinclient.common.model.DirectoryObject;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * ApplicationProfileMembersMapper map Application-JSON-object 'members-attribute to
 * org.openthinclient.common.model.Application.members
 */
@Component
public class ApplicationProfileMembersMapper {

    public Set<DirectoryObject> asSetOfDirectoryObject(Set<String> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(s -> {
                    ClientGroup clientGroup = new ClientGroup();
                    clientGroup.setName("uniquemember");
                    clientGroup.setDn(s);
                    return clientGroup;
                }).collect(Collectors.toSet());
    }

    public Set<String> asSetOfString(Set<DirectoryObject> source) {
        if (source == null) {
            return null;
        }
        return source.stream().map(directoryObject -> directoryObject.getDn()).collect(Collectors.toSet());
    }
}
