package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Device;
import org.openthinclient.ldap.DirectoryException;

public interface DeviceService extends DirectoryObjectService<Device> {
    void delete(Device device) throws DirectoryException;
}
