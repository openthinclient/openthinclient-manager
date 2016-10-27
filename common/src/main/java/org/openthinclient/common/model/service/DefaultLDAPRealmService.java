package org.openthinclient.common.model.service;

import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;

import java.util.Set;

public class DefaultLDAPRealmService implements RealmService {
    @Override
    public Set<Realm> findAllRealms() {
        LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
        lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
        lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
        lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system",
                System.getProperty("ContextSecurityCredentials", "secret")
                        .toCharArray()));
        try {
            return LDAPDirectory.findAllRealms(lcd);
        } catch (DirectoryException e) {
            // FIXME better exception handling
            throw new RuntimeException(e);
        }

    }
}
