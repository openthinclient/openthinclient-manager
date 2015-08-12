package org.openthinclient.service.apacheds;

import org.apache.directory.server.jndi.ServerContextFactory;
import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationDirectory;
import org.openthinclient.service.common.home.ConfigurationFile;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@ConfigurationFile("directory/service.xml")
@XmlRootElement(name = "directory", namespace = "http://www.openthinclient.org/ns/manager/service/directory/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class DirectoryServiceConfiguration implements Configuration {


  /**
   * Default LDAP Listen Port
   */
  public static final int DEFAULT_LDAP_PORT = 10389;

  /**
   * Default LDAPS (SSL) Port
   */
  public static final int DEFAULT_LDAPS_PORT = 10636;

  // ~ Instance fields
  // --------------------------------------------------------

  @XmlElement
  private boolean embeddedServerEnabled = true;

  @ConfigurationDirectory("work")
  private File embeddedWkDir;

  @ConfigurationDirectory("ldif")
  private File embeddedLDIFDir;

  @XmlElement
  private int embeddedLdapPort = DEFAULT_LDAP_PORT;

  @XmlElement
  private int embeddedLdapsPort = DEFAULT_LDAPS_PORT;

  @XmlElement
  private String embeddedCustomRootPartitionName = "dc=openthinclient,dc=org";

  @XmlElement
  private String contextProviderURL = "uid=admin,ou=system";

  @XmlElement
  private String contextSecurityAuthentication = "simple";

  @XmlElement
  private String contextSecurityCredentials = System.getProperty("ContextSecurityCredentials", "secret");

  @XmlElement
  private String contextSecurityPrincipal = "uid=admin,ou=system";

  @XmlElement
  private boolean embeddedAnonymousAccess = false;

  @XmlElement
  private boolean embeddedLdapNetworkingSupport = false;

  @XmlElement
  private String contextFactory = ServerContextFactory.class.getName();

  // FIXME is this required at all? If yes, create a nicer model representation
  @XmlAnyElement
  private Element additionalEnv = null;

  // FIXME is this required at all? If yes, create a nicer model representation
  @XmlAnyElement
  private Element ldifFilters = null;

  @XmlElement
  private boolean accessControlEnabled = false;

  @XmlElement
  private boolean enableNtp = false;

  @XmlElement
  private boolean enableKerberos = false;

  @XmlElement
  private boolean enableChangePassword = false;

  public boolean isEmbeddedServerEnabled() {
    return embeddedServerEnabled;
  }

  public void setEmbeddedServerEnabled(boolean embeddedServerEnabled) {
    this.embeddedServerEnabled = embeddedServerEnabled;
  }

  public File getEmbeddedWkDir() {
    return embeddedWkDir;
  }

  public void setEmbeddedWkDir(File embeddedWkDir) {
    this.embeddedWkDir = embeddedWkDir;
  }

  public File getEmbeddedLDIFDir() {
    return embeddedLDIFDir;
  }

  public void setEmbeddedLDIFDir(File embeddedLDIFDir) {
    this.embeddedLDIFDir = embeddedLDIFDir;
  }

  public int getEmbeddedLdapPort() {
    return embeddedLdapPort;
  }

  public void setEmbeddedLdapPort(int embeddedLdapPort) {
    this.embeddedLdapPort = embeddedLdapPort;
  }

  public int getEmbeddedLdapsPort() {
    return embeddedLdapsPort;
  }

  public void setEmbeddedLdapsPort(int embeddedLdapsPort) {
    this.embeddedLdapsPort = embeddedLdapsPort;
  }

  public String getEmbeddedCustomRootPartitionName() {
    return embeddedCustomRootPartitionName;
  }

  public void setEmbeddedCustomRootPartitionName(String embeddedCustomRootPartitionName) {
    this.embeddedCustomRootPartitionName = embeddedCustomRootPartitionName;
  }

  public String getContextProviderURL() {
    return contextProviderURL;
  }

  public void setContextProviderURL(String contextProviderURL) {
    this.contextProviderURL = contextProviderURL;
  }

  public String getContextSecurityAuthentication() {
    return contextSecurityAuthentication;
  }

  public void setContextSecurityAuthentication(String contextSecurityAuthentication) {
    this.contextSecurityAuthentication = contextSecurityAuthentication;
  }

  public String getContextSecurityCredentials() {
    return contextSecurityCredentials;
  }

  public void setContextSecurityCredentials(String contextSecurityCredentials) {
    this.contextSecurityCredentials = contextSecurityCredentials;
  }

  public String getContextSecurityPrincipal() {
    return contextSecurityPrincipal;
  }

  public void setContextSecurityPrincipal(String contextSecurityPrincipal) {
    this.contextSecurityPrincipal = contextSecurityPrincipal;
  }

  public boolean isEmbeddedAnonymousAccess() {
    return embeddedAnonymousAccess;
  }

  public void setEmbeddedAnonymousAccess(boolean embeddedAnonymousAccess) {
    this.embeddedAnonymousAccess = embeddedAnonymousAccess;
  }

  public boolean isEmbeddedLdapNetworkingSupport() {
    return embeddedLdapNetworkingSupport;
  }

  public void setEmbeddedLdapNetworkingSupport(boolean embeddedLdapNetworkingSupport) {
    this.embeddedLdapNetworkingSupport = embeddedLdapNetworkingSupport;
  }

  public String getContextFactory() {
    return contextFactory;
  }

  public void setContextFactory(String contextFactory) {
    this.contextFactory = contextFactory;
  }

  public Element getAdditionalEnv() {
    return additionalEnv;
  }

  public void setAdditionalEnv(Element additionalEnv) {
    this.additionalEnv = additionalEnv;
  }

  public Element getLdifFilters() {
    return ldifFilters;
  }

  public void setLdifFilters(Element ldifFilters) {
    this.ldifFilters = ldifFilters;
  }

  public boolean isAccessControlEnabled() {
    return accessControlEnabled;
  }

  public void setAccessControlEnabled(boolean accessControlEnabled) {
    this.accessControlEnabled = accessControlEnabled;
  }

  public boolean isEnableNtp() {
    return enableNtp;
  }

  public void setEnableNtp(boolean enableNtp) {
    this.enableNtp = enableNtp;
  }

  public boolean isEnableKerberos() {
    return enableKerberos;
  }

  public void setEnableKerberos(boolean enableKerberos) {
    this.enableKerberos = enableKerberos;
  }

  public boolean isEnableChangePassword() {
    return enableChangePassword;
  }

  public void setEnableChangePassword(boolean enableChangePassword) {
    this.enableChangePassword = enableChangePassword;
  }
}
