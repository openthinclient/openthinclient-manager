package org.openthinclient.tftp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;

import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.service.store.ClientBootData;

public class PXEConfigTFTProviderTest {

  private static final String DEFAULT_TEMPLATE =
          "DEFAULT tcos\n" + //
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

  private static final String RESOLVED_TEMPLATE =
          "DEFAULT tcos\n" + //
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

  public static final class Config {
    public Config(Path managerHome, Path tftpHome, String fastTemplate, String safeTemplate) {
      this.managerHome = managerHome;
      this.tftpHome = tftpHome;
      this.fastTemplate = fastTemplate;
      this.safeTemplate = safeTemplate;
    }

    public final Path managerHome;
    public final Path tftpHome;
    public final String fastTemplate;
    public final String safeTemplate;
  }

  private static Config config;

  @BeforeClass
  public static void setUp() throws Exception {
    Path managerHome = Paths.get("target", "test-data", PXEConfigTFTProviderTest.class.getSimpleName());

    Files.createDirectories(managerHome);

    final Path tftpHome = managerHome.resolve(Paths.get("nfs", "root", "tftp"));

    Files.createDirectories(tftpHome);

    // setup some template files
    final String fastTemplate = "template.txt";
    final String safeTemplate = "another-template.txt";

    final Path fastTemplatePath = tftpHome.resolve(fastTemplate);
    final Path safeTemplatePath = tftpHome.resolve(safeTemplate);

    Files.write(fastTemplatePath, "DEFAULT_TEMPLATE".getBytes());
    Files.write(safeTemplatePath, "NON_DEFAULT_TEMPLATE".getBytes());

    config = new Config(managerHome, tftpHome, fastTemplate, safeTemplate);
  }

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
  public void testCompressTemplate() throws Exception {

    final PXEConfigTFTProvider provider = createProvider();
    final String result = provider.resolveVariables(
        DEFAULT_TEMPLATE,
        new ClientBootData("cn=mock", "192.0.2.120", createProperties()),
        "192.0.2.1");

    if (!RESOLVED_TEMPLATE.equals(result)) {
      fail(String.format(
            "Comparision failure! Expected\n%s\n\nBut gor:\n%s\n",
            RESOLVED_TEMPLATE, result));
    }
  }

  private PXEConfigTFTProvider createProvider() throws DirectoryException {
    return new PXEConfigTFTProvider(config.tftpHome, config.fastTemplate, config.fastTemplate);
  }

  @Test
  public void testResolveTemplateWithConfiguredTemplate() throws Exception {
    final PXEConfigTFTProvider provider = createProvider();

    List<Map<String, String>> props = createProperties();
    props.get(0).put("BootOptions.BootLoaderTemplate", "another-template.txt");
    props.get(0).put("BootOptions.BootMode", "safe");
    System.out.println(String.format("THIS:\n%s", props));
    final ClientBootData bootData = new ClientBootData( "cn=mock",
                                                        "192.0.2.120",
                                                        props);
    final Path path = provider.getTemplatePath(bootData);

    assertEquals(config.tftpHome.resolve(config.safeTemplate), path);
  }

  private List<Map<String, String>> createProperties() {
    List<Map<String, String>> props = new ArrayList<>();
    Map<String, String> subProps = new HashMap<>();
    subProps.put("BootOptions.KernelName", "BOOTOPTIONS_KERNELNAME");
    subProps.put("BootOptions.InitrdName", "BOOTOPTIONS_INITRDNAME");
    subProps.put("BootOptions.FirmwareImage", "BOOTOPTIONS_FIRMWAREIMAGE");
    props.add(subProps);

    subProps = new HashMap<>(subProps);
    subProps.put("BootOptions.KernelName", "this should be overwritten");
    subProps.put("BootOptions.FirmwareImage", "this should be overwritten");
    subProps.put("BootOptions.Debug", "BOOTOPTIONS_DEBUG");
    subProps.put("BootOptions.ExtraOptions", "BOOTOPTIONS_EXTRAOPTIONS");
    subProps.put("BootOptions.FirmwareImage", "BOOTOPTIONS_FIRMWAREIMAGE");
    subProps.put("BootOptions.GpuModule", "BOOTOPTIONS_GPUMODULE");
    subProps.put("BootOptions.InitrdName", "BOOTOPTIONS_INITRDNAME");
    subProps.put("BootOptions.KernelName", "BOOTOPTIONS_KERNELNAME");
    subProps.put("BootOptions.NFSRootPath", "BOOTOPTIONS_NFSROOTPATH");
    props.add(subProps);

    subProps = new HashMap<>(subProps);
    subProps.put("BootOptions.InitrdName", "this should be replaces");
    subProps.put("BootOptions.FirmwareImage", "this should be replaces");
    subProps.put("BootOptions.KernelName", "this should be replaces");
    subProps.put("BootOptions.NFSRootPath", "this should be replaces");
    subProps.put("BootOptions.NFSRootserver", "BOOTOPTIONS_NFSROOTSERVER");
    subProps.put("Directory.Primary.LDAPURLs", "DIRECTORY_PRIMARY_LDAPURLS");
    subProps.put("Directory.Primary.ReadOnly.Secret", "DIRECTORY_PRIMARY_READONLY.SECRET");
    subProps.put("Directory.Primary.ReadOnly.Principal", "DIRECTORY_PRIMARY_READONLY_PRINCIPAL");
    subProps.put("HomeOptions.NFSHomePath", "HOMEOPTIONS_NFSHOMEPATH");
    subProps.put("HomeOptions.NFSHomeserver", "HOMEOPTIONS_NFSHOMESERVER");
    subProps.put("VerbosityOptions.SplashImage", "VERBOSITYOPTIONS_SPLASHIMAGE");
    subProps.put("VerbosityOptions.Verbosity", "VERBOSITYOPTIONS_VERBOSITY");
    props.add(subProps);

    return props;
  }

}
