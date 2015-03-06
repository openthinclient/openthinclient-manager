package org.openthinclient.pkgmgr;

import org.openthinclient.service.common.home.ConfigurationDirectory;
import org.openthinclient.service.common.home.ConfigurationFile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.File;

@ConfigurationFile("package-manager.xml")
@XmlRootElement(name = "package-manager", namespace = "http://www.openthinclient.org/ns/manager/service/package-manager/1.0")
@XmlAccessorType(XmlAccessType.NONE)
public class PackageManagerConfiguration {


  @ConfigurationDirectory("nfs/root")
  private File installDir;
  @ConfigurationDirectory("nfs/root/var/cache")
  private File workingDir;
  @ConfigurationDirectory("nfs/root/var/cache/archives")
  private File archivesDir;
  @ConfigurationDirectory("nfs/root/var/cache/archives/testinstall")
  private File testinstallDir;
  @ConfigurationDirectory("nfs/root/var/cache/archives/partial")
  private File partialDir;
  @ConfigurationDirectory("nfs/root/var/cache/lists")
  private File listsDir;
  @ConfigurationFile("nfs/root/var/db/package.db")
  private File packageDB;
  @ConfigurationFile("nfs/root/var/cache/lists/cache.db")
  private File cacheDB;
  @ConfigurationFile("nfs/root/etc/sources.list")
  private File sourcesList;
  @ConfigurationFile("nfs/root/var/cache/old")
  private File installOldDir;
  @ConfigurationFile("nfs/root/var/cache/old/remove.db")
  private File oldDB;

  // FIXME now there is really need for clarification about this property
  @XmlElement(name="remove-it-really")
  private boolean removeIt;
  @ConfigurationFile("nfs/root/var/cache/archives/archives.db")
  private File archivesDB;

  @XmlElement(name="proxy")
  private ProxyConfiguration proxyConfiguration;


  public File getInstallDir() {
    return installDir;
  }

  public void setInstallDir(File installDir) {
    this.installDir = installDir;
  }

  public File getWorkingDir() {
    return workingDir;
  }

  public void setWorkingDir(File workingDir) {
    this.workingDir = workingDir;
  }

  public File getArchivesDir() {
    return archivesDir;
  }

  public void setArchivesDir(File archivesDir) {
    this.archivesDir = archivesDir;
  }

  public File getTestinstallDir() {
    return testinstallDir;
  }

  public void setTestinstallDir(File testinstallDir) {
    this.testinstallDir = testinstallDir;
  }

  public File getPartialDir() {
    return partialDir;
  }

  public void setPartialDir(File partialDir) {
    this.partialDir = partialDir;
  }

  public File getListsDir() {
    return listsDir;
  }

  public void setListsDir(File listsDir) {
    this.listsDir = listsDir;
  }

  public File getPackageDB() {
    return packageDB;
  }

  public void setPackageDB(File packageDB) {
    this.packageDB = packageDB;
  }

  public File getCacheDB() {
    return cacheDB;
  }

  public void setCacheDB(File cacheDB) {
    this.cacheDB = cacheDB;
  }

  public File getSourcesList() {
    return sourcesList;
  }

  public void setSourcesList(File sourcesList) {
    this.sourcesList = sourcesList;
  }

  public File getInstallOldDir() {
    return installOldDir;
  }

  public void setInstallOldDir(File installOldDir) {
    this.installOldDir = installOldDir;
  }

  public File getOldDB() {
    return oldDB;
  }

  public void setOldDB(File oldDB) {
    this.oldDB = oldDB;
  }

  public boolean isRemoveIt() {
    return removeIt;
  }

  public void setRemoveIt(boolean removeIt) {
    this.removeIt = removeIt;
  }

  public File getArchivesDB() {
    return archivesDB;
  }

  public void setArchivesDB(File archivesDB) {
    this.archivesDB = archivesDB;
  }

  public ProxyConfiguration getProxyConfiguration() {
    return proxyConfiguration;
  }

  public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
    this.proxyConfiguration = proxyConfiguration;
  }

  @XmlType
  @XmlAccessorType(XmlAccessType.NONE)
  public static class ProxyConfiguration {

    @XmlElement(name="port")
    private int port;
    @XmlElement(name="user")
    private String user;
    @XmlElement(name="password")
    private String password;
    @XmlElement(name="host")
    private String host;

    @XmlAttribute(name="enabled")
    private boolean enabled;

    public void setPort(int port) {
      this.port = port;
    }

    public int getPort() {
      return port;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getUser() {
      return user;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPassword() {
      return password;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getHost() {
      return host;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }
}
