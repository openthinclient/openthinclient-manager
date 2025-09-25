package org.openthinclient.manager.standalone.patch;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.openthinclient.common.directory.ACLUtils;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import org.openthinclient.service.nfs.NFSServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;


public class PatchManagerHome {
  private static final Logger LOGGER = LoggerFactory.getLogger(PatchManagerHome.class);
  private static final String[] PW_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-".split("");
  private static final SecureRandom RNG = new SecureRandom();
  private static final ComparableVersion v2019_1 =
      new ComparableVersion("2019.1");
  private static final ComparableVersion v2024_1_4 =
      new ComparableVersion("2024.1.4");

  private ManagerHome managerHome;


  public PatchManagerHome(ManagerHome managerHome) {
    this.managerHome = managerHome;
  }

  public void apply() {
    ManagerHomeMetadata managerHomeMeta = managerHome.getMetadata();

    ComparableVersion lastHomeUpdateVersion = new ComparableVersion(managerHomeMeta.getLastHomeUpdateVersion());

    if (lastHomeUpdateVersion.compareTo(v2019_1) <= 0) {
      applyLDAPSecurityPatch();
    }

    if (lastHomeUpdateVersion.compareTo(v2024_1_4) <= 0) {
      makeNFSRootExportReadOnly();
    }

    // Save successful update version, so we don't do it again
    try {
      Properties props = new Properties();
      props.load(
          PatchManagerHome.class.getResourceAsStream("/application.properties"));
      managerHomeMeta.setLastHomeUpdateVersion(
          props.getProperty("application.version"));
      managerHomeMeta.save();
    } catch (Exception ex) {
      LOGGER.error("Could not save last home update version", ex);
    }


  }


  private void applyLDAPSecurityPatch() {
    LOGGER.info("Applying LDAP security patch.");
    DirectoryServiceConfiguration configuration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
    if (configuration.isEmbeddedServerEnabled()
          && !configuration.isAccessControlEnabled()) {
      try {
        DirectoryService service = new DirectoryService();
        service.setConfiguration(configuration);
        service.startService();

        String password = Stream.generate(()->PW_CHARS[RNG.nextInt(PW_CHARS.length)])
                                .limit(32)
                                .collect(Collectors.joining());
        service.changedEmbeddedAdminPassword(configuration.getContextSecurityCredentials(), password);
        applyACLs(configuration);
        configuration.setAccessControlEnabled(true);
        managerHome.save(DirectoryServiceConfiguration.class);
        LOGGER.info("LDAP security patch succesfully applied.");
      } catch (Exception ex) {
        LOGGER.error("LDAP security patch failed", ex);
      }
    }
  }

  private void applyACLs(DirectoryServiceConfiguration configuration)
  throws NamingException {
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


  private void makeNFSRootExportReadOnly() {
    LOGGER.info("Making NFS root export read-only.");
    managerHome.getConfiguration(NFSServiceConfiguration.class)
        .getExports().stream()
        .filter(export -> export.getName().equals("/openthinclient"))
        .forEach(export -> export.getGroups().get(0).setReadOnly(true));
    managerHome.save(NFSServiceConfiguration.class);
  }
}
