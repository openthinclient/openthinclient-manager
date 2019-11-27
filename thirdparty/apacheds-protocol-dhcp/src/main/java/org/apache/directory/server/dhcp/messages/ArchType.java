package org.apache.directory.server.dhcp.messages;

import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;

public enum ArchType {
    UNKNOWN,
    BIOS,
    UEFI32,
    UEFI64;

    public static ArchType fromMessage(DhcpMessage message) {
        VendorClassIdentifier vci = (VendorClassIdentifier) message
                        .getOptions().get(VendorClassIdentifier.class);
        if(null != vci) {
            String[] vciParts = vci.getString().split(":");
            if(vciParts.length > 1 && vciParts[0].equals("PXEClient")) {
                if(vciParts.length > 2) {
                    if(vciParts[2].equals("00006")) {
                        return UEFI32;
                    } else if(vciParts[2].equals("00007") || vciParts[2].equals("00009")) {
                        return UEFI64;
                    }
                }
                return BIOS;
            }
        }
        return UNKNOWN;
    }

    public static boolean isPXEClient(DhcpMessage message) {
        return fromMessage(message) != UNKNOWN;
    }

    public static boolean isUEFI(DhcpMessage message) {
        ArchType archType = fromMessage(message);
        return archType == UEFI32 || archType == UEFI64;
    }
}
