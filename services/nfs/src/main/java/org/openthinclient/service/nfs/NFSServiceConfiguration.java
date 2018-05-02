package org.openthinclient.service.nfs;

import org.openthinclient.service.common.ServiceConfiguration;
import org.openthinclient.service.common.home.ConfigurationFile;

import java.io.File;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@ConfigurationFile("nfs/service.xml")
@XmlRootElement(name = "nfs", namespace = "http://www.openthinclient.org/ns/manager/service/nfs/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class NFSServiceConfiguration implements ServiceConfiguration {

	@XmlElement
	private int nfsPort = 2069; // use default port
	@XmlElement
	private int nfsProgramNumber;
	@XmlElement
	private int mountdPort; // use default port
	@XmlElement
	private int mountdProgramNumber;
	@XmlElement
	private int portmapPort; // use default port
	@XmlElement
	private int portmapProgramNumber;
	@XmlElement
	private int flushInterval = 300; // flush every 5 minutes

	@XmlElementWrapper(name = "exports")
	@XmlElement(name = "export")
	private Exports exports = new Exports();

  @XmlTransient
  @ConfigurationFile("nfs-paths.db")
	private File pathDBLocation;

	@XmlElement(defaultValue = "true")
	private boolean autostart = true;

	@Override
	public boolean isAutostartEnabled() {
		return autostart;
	}

	public void setAutostartEnabled(boolean autostart) {
		this.autostart = autostart;
	}

	/**
	 * @return the nfsPort
	 */
	public int getNfsPort() {
		return nfsPort;
	}
	/**
	 * @param nfsPort the nfsPort to set
	 */
	public void setNfsPort(int nfsPort) {
		this.nfsPort = nfsPort;
	}
	/**
	 * @return the nfsProgramNumber
	 */
	public int getNfsProgramNumber() {
		return nfsProgramNumber;
	}
	/**
	 * @param nfsProgramNumber the nfsProgramNumber to set
	 */
	public void setNfsProgramNumber(int nfsProgramNumber) {
		this.nfsProgramNumber = nfsProgramNumber;
	}
	/**
	 * @return the mountdPort
	 */
	public int getMountdPort() {
		return mountdPort;
	}
	/**
	 * @param mountdPort the mountdPort to set
	 */
	public void setMountdPort(int mountdPort) {
		this.mountdPort = mountdPort;
	}
	/**
	 * @return the mountdProgramNumber
	 */
	public int getMountdProgramNumber() {
		return mountdProgramNumber;
	}
	/**
	 * @param mountdProgramNumber the mountdProgramNumber to set
	 */
	public void setMountdProgramNumber(int mountdProgramNumber) {
		this.mountdProgramNumber = mountdProgramNumber;
	}
	/**
	 * @return the portmapPort
	 */
	public int getPortmapPort() {
		return portmapPort;
	}
	/**
	 * @param portmapPort the portmapPort to set
	 */
	public void setPortmapPort(int portmapPort) {
		this.portmapPort = portmapPort;
	}
	/**
	 * @return the portmapProgramNumber
	 */
	public int getPortmapProgramNumber() {
		return portmapProgramNumber;
	}
	/**
	 * @param portmapProgramNumber the portmapProgramNumber to set
	 */
	public void setPortmapProgramNumber(int portmapProgramNumber) {
		this.portmapProgramNumber = portmapProgramNumber;
	}
	/**
	 * @return the flushInterval
	 */
	public int getFlushInterval() {
		return flushInterval;
	}
	/**
	 * @param flushInterval the flushInterval to set
	 */
	public void setFlushInterval(int flushInterval) {
		this.flushInterval = flushInterval;
	}
	/**
	 * @return the pathDBLocation
	 */
	public File getPathDBLocation() {
		return pathDBLocation;
	}
	/**
	 * @param pathDBLocation the pathDBLocation to set
	 */
	public void setPathDBLocation(File pathDBLocation) {
		this.pathDBLocation = pathDBLocation;
	}

	public Exports getExports() {
		return exports;
	}

	public void setExports(Exports exports) {
		this.exports = exports;
	}
}
