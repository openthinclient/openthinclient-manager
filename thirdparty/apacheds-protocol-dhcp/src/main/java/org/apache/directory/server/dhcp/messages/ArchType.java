package org.apache.directory.server.dhcp.messages;

import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;

public enum ArchType {
    UNKNOWN,
    BIOS,
    UEFI32,
    UEFI64,
    HTTP32,
    HTTP64;

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
            } else if(vciParts.length > 2 && vciParts[0].equals("HTTPClient")) {
                if(vciParts[2].equals("00015")) {
                    return HTTP32;
                } else if (vciParts[2].equals("00016")) {
                    return HTTP64;
                }
            }
        }
        return UNKNOWN;
    }

    public boolean isPXEClient() {
        return this != UNKNOWN;
    }

    public boolean isHTTP() {
        return this == HTTP32 || this == HTTP64;
    }

    public boolean isUEFI() {
        return this == UEFI32 || this == UEFI64 || this.isHTTP();
    }

    public static boolean isUEFI(DhcpMessage message) {
        return fromMessage(message).isUEFI();
    }
}
