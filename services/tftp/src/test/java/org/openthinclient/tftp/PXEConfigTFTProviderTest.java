package org.openthinclient.tftp;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.DirectoryException;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PXEConfigTFTProviderTest {

  private static final String DEFAULT_TEMPLATE = "DEFAULT tcos\n" + //
          "\n" + //
          "LABEL tcos\n" + //
          "\tKERNEL\t${BootOptions.KernelName}\n" + //
          "\tINITRD\t${BootOptions.InitrdName},${BootOptions.KernelName}-modules.img${BootOptions.FirmwareImage}\n" + //
          "\tAPPEND\tnfsroot=${BootOptions.NFSRootserver}:${BootOptions.NFSRootPath} \\\n" + //
          "\t\tnfshome=${HomeOptions.NFSHomeserver}:${HomeOptions.NFSHomePath} \\\n" + //
          "\t\tldapurl=${Directory.Primary.LDAPURLs}????bindname=${urlencoded:Directory.Primary.ReadOnly.Principal},X-BINDPW=${base64:Directory.Primary.ReadOnly.Secret} \\\n" + //
          "\t\tro max_loop=256 \\\n" + //
          "\t\t${VerbosityOptions.Verbosity} \\\n" + //
          "\t\t${BootOptions.GpuModule} \\\n" + //
          "\t\tsplashImage=${VerbosityOptions.SplashImage} \\\n" + //
          "\t\t${BootOptions.Debug} \\\n" + //
          "\t\t${BootOptions.ExtraOptions}\n" + //
          "\tIPAPPEND 1\n" + //
          "\n" + //
          "\n" + //
          "##\n";

  private static final String RESOLVED_TEMPLATE = "DEFAULT tcos\n" + //
          "\n" + //
          "LABEL tcos\n" + //
          "\tKERNEL\tBOOTOPTIONS_KERNELNAME\n" + //
          "\tINITRD\tBOOTOPTIONS_INITRDNAME,BOOTOPTIONS_KERNELNAME-modules.imgBOOTOPTIONS_FIRMWAREIMAGE\n" + //
          "\tAPPEND\tnfsroot=BOOTOPTIONS_NFSROOTSERVER:BOOTOPTIONS_NFSROOTPATH \\\n" + //
          "\t\tnfshome=HOMEOPTIONS_NFSHOMESERVER:HOMEOPTIONS_NFSHOMEPATH \\\n" + //
          "\t\tldapurl=DIRECTORY_PRIMARY_LDAPURLS????bindname=DIRECTORY_PRIMARY_READONLY_PRINCIPAL,X-BINDPW=RElSRUNUT1JZX1BSSU1BUllfUkVBRE9OTFkuU0VDUkVU \\\n" + //
          "\t\tro max_loop=256 \\\n" + //
          "\t\tVERBOSITYOPTIONS_VERBOSITY \\\n" + //
          "\t\tBOOTOPTIONS_GPUMODULE \\\n" + //
          "\t\tsplashImage=VERBOSITYOPTIONS_SPLASHIMAGE \\\n" + //
          "\t\tBOOTOPTIONS_DEBUG \\\n" + //
          "\t\tBOOTOPTIONS_EXTRAOPTIONS\n" + //
          "\tIPAPPEND 1\n" + //
          "\n" + //
          "\n" + //
          "##\n";

  private static final String COMPRESSED_TEMPLATE = "DEFAULT tcos\n" + //
          "\n" + //
          "LABEL tcos\n" + //
          " KERNEL BOOTOPTIONS_KERNELNAME\n" + //
          " INITRD BOOTOPTIONS_INITRDNAME,BOOTOPTIONS_KERNELNAME-modules.imgBOOTOPTIONS_FIRMWAREIMAGE\n" + //
          " APPEND nfsroot=BOOTOPTIONS_NFSROOTSERVER:BOOTOPTIONS_NFSROOTPATH nfshome=HOMEOPTIONS_NFSHOMESERVER:HOMEOPTIONS_NFSHOMEPATH ldapurl=DIRECTORY_PRIMARY_LDAPURLS????bindname=DIRECTORY_PRIMARY_READONLY_PRINCIPAL,X-BINDPW=RElSRUNUT1JZX1BSSU1BUllfUkVBRE9OTFkuU0VDUkVU ro max_loop=256 VERBOSITYOPTIONS_VERBOSITY BOOTOPTIONS_GPUMODULE splashImage=VERBOSITYOPTIONS_SPLASHIMAGE BOOTOPTIONS_DEBUG BOOTOPTIONS_EXTRAOPTIONS\n" + //
          " IPAPPEND 1\n" + //
          "\n" + //
          "\n" + //
          "##\n";

  @Test
  public void testPatternMatchesAllVariables() throws Exception {

    final Matcher m = PXEConfigTFTProvider.TEMPLATE_REPLACEMENT_PATTERN.matcher(DEFAULT_TEMPLATE);

    final Set<String> variables = new TreeSet<>();
    while (m.find()) {
      variables.add(m.group(1));
    }

    assertThat(variables, CoreMatchers.hasItem("BootOptions.Debug"));
    assertThat(variables, CoreMatchers.hasItem("BootOptions.ExtraOptions"));
    assertThat(variables, CoreMatchers.hasItem("BootOptions.FirmwareImage"));
    assertThat(variables, CoreMatchers.hasItem("BootOptions.GpuModule"));
    assertThat(variables, CoreMatchers.hasItem("BootOptions.InitrdName"));
    assertThat(variables, CoreMatchers.hasItem("BootOptions.KernelName"));
    assertThat(variables, CoreMatchers.hasItem("BootOptions.NFSRootPath"));
    assertThat(variables, CoreMatchers.hasItem("BootOptions.NFSRootserver"));
    assertThat(variables, CoreMatchers.hasItem("Directory.Primary.LDAPURLs"));
    assertThat(variables, CoreMatchers.hasItem("HomeOptions.NFSHomePath"));
    assertThat(variables, CoreMatchers.hasItem("HomeOptions.NFSHomeserver"));
    assertThat(variables, CoreMatchers.hasItem("VerbosityOptions.SplashImage"));
    assertThat(variables, CoreMatchers.hasItem("VerbosityOptions.Verbosity"));
    assertThat(variables, CoreMatchers.hasItem("base64:Directory.Primary.ReadOnly.Secret"));
    assertThat(variables, CoreMatchers.hasItem("urlencoded:Directory.Primary.ReadOnly.Principal"));
  }

  @Test
  public void testProcessTheDefaultTemplate() throws Exception {

    final PXEConfigTFTProvider provider = createProvider();

    final MapPropertySource propertySource = createPropertySource();

    final String processed = provider.resolveVariables(DEFAULT_TEMPLATE, propertySource);


    assertEquals(RESOLVED_TEMPLATE, processed);
  }

  @Test
  public void testCompressTemplate() throws Exception {

    final PXEConfigTFTProvider provider = createProvider();
    final MapPropertySource propertySource = createPropertySource();
    final String processed = provider.resolveVariables(DEFAULT_TEMPLATE, propertySource);

    final String result = provider.compressTemplate(processed);


    assertEquals(COMPRESSED_TEMPLATE, result);
  }

  private PXEConfigTFTProvider createProvider() throws DirectoryException {
    return new PXEConfigTFTProvider(null, new RealmService() {
      @Override
      public Realm getDefaultRealm() {
        return null;
      }

      @Override
      public Set<Realm> findAllRealms() {
        return Collections.emptySet();
      }

      @Override
      public void reload() {

      }
    }, new ClientService() {
      @Override
      public Set<Client> findByHwAddress(String hwAddressString) {
        return null;
      }

      @Override
      public Set<Client> findAll() {
        return null;
      }

      @Override
      public Client getDefaultClient() {
        return null;
      }

      @Override
      public Client findByName(String name) {
        return null;
      }

      @Override
      public void save(Client object) {

      }
    }, null);
  }

  private MapPropertySource createPropertySource() {
    final MapPropertySource propertySource = new MapPropertySource("testProperties", new HashMap<>());
    propertySource.getSource().put("BootOptions.KernelName", "KERNEL_NAME");
    propertySource.getSource().put("BootOptions.InitrdName", "INITRD_NAME");
    propertySource.getSource().put("BootOptions.FirmwareImage", "INITRD_NAME");

    propertySource.getSource().put("BootOptions.Debug", "BOOTOPTIONS_DEBUG");
    propertySource.getSource().put("BootOptions.ExtraOptions", "BOOTOPTIONS_EXTRAOPTIONS");
    propertySource.getSource().put("BootOptions.FirmwareImage", "BOOTOPTIONS_FIRMWAREIMAGE");
    propertySource.getSource().put("BootOptions.GpuModule", "BOOTOPTIONS_GPUMODULE");
    propertySource.getSource().put("BootOptions.InitrdName", "BOOTOPTIONS_INITRDNAME");
    propertySource.getSource().put("BootOptions.KernelName", "BOOTOPTIONS_KERNELNAME");
    propertySource.getSource().put("BootOptions.NFSRootPath", "BOOTOPTIONS_NFSROOTPATH");
    propertySource.getSource().put("BootOptions.NFSRootserver", "BOOTOPTIONS_NFSROOTSERVER");
    propertySource.getSource().put("Directory.Primary.LDAPURLs", "DIRECTORY_PRIMARY_LDAPURLS");
    propertySource.getSource().put("Directory.Primary.ReadOnly.Secret", "DIRECTORY_PRIMARY_READONLY.SECRET");
    propertySource.getSource().put("Directory.Primary.ReadOnly.Principal", "DIRECTORY_PRIMARY_READONLY_PRINCIPAL");
    propertySource.getSource().put("HomeOptions.NFSHomePath", "HOMEOPTIONS_NFSHOMEPATH");
    propertySource.getSource().put("HomeOptions.NFSHomeserver", "HOMEOPTIONS_NFSHOMESERVER");
    propertySource.getSource().put("VerbosityOptions.SplashImage", "VERBOSITYOPTIONS_SPLASHIMAGE");
    propertySource.getSource().put("VerbosityOptions.Verbosity", "VERBOSITYOPTIONS_VERBOSITY");
    return propertySource;
  }
}