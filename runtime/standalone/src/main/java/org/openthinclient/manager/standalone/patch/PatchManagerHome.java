package org.openthinclient.manager.standalone.patch;

import org.openthinclient.common.directory.ACLUtils;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;


public class PatchManagerHome {
  private static final Logger LOGGER = LoggerFactory.getLogger(PatchManagerHome.class);
  private static final String[] PW_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-".split("");
  private static final SecureRandom RNG = new SecureRandom();

  private ManagerHome managerHome;
  private DirectoryServiceConfiguration configuration;

  public PatchManagerHome(ManagerHome managerHome) {
    this.managerHome = managerHome;
    configuration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
  }

  public void apply() {
    if (configuration.isEmbeddedServerEnabled() && !configuration.isAccessControlEnabled()) {
      LOGGER.info("Applying LDAP security patch.");
      try {
        DirectoryService service = new DirectoryService();
        service.setConfiguration(configuration);
        service.startService();

        String password = Stream.generate(()->PW_CHARS[RNG.nextInt(PW_CHARS.length)])
                                .limit(32)
                                .collect(Collectors.joining());
        service.changedEmbeddedAdminPassword(configuration.getContextSecurityCredentials(), password);
        applyACLs();
        configuration.setAccessControlEnabled(true);
        managerHome.save(DirectoryServiceConfiguration.class);
        LOGGER.info("LDAP security patch succesfully applied.");
      } catch (Exception ex) {
        LOGGER.error("LDAP security patch failed", ex);
      }
    }
  }

  private void applyACLs() throws NamingException {
    LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
    lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
    lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
    lcd.setCallbackHandler(new UsernamePasswordHandler(configuration.getContextSecurityPrincipal(),
            configuration.getContextSecurityCredentials().toCharArray()));

    lcd.setBaseDN(String.format("ou=%s,%s", configuration.getPrimaryOU(), configuration.getEmbeddedCustomRootPartitionName()));
    LdapContext ctx = lcd.createDirectoryFacade().createDirContext();
    try {
      ACLUtils aclUtils = new ACLUtils(ctx);
      aclUtils.makeACSA("");
      aclUtils.enableSearchForAllUsers("");
      aclUtils.enableAdminUsers("");
    } finally {
      ctx.close();
    }
  }
}
