package org.openthinclient.service.nfs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationFile;

@ConfigurationFile("nfs/service.xml")
@XmlRootElement(name = "directory", namespace = "http://www.openthinclient.org/ns/manager/service/nfs/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class NFSServiceConfiguration implements Configuration {

	@XmlElement
	private int nfsPort = 0; // use default port
	@XmlElement
	private int nfsProgramNumber = 0;
	@XmlElement
	private int mountdPort = 0; // use default port
	@XmlElement
	private int mountdProgramNumber = 0;
	@XmlElement
	private int portmapPort = 0; // use default port
	@XmlElement
	private int portmapProgramNumber = 0;
	@XmlElement
	private int flushInterval = 300; // flush every 5 minutes
	@XmlElement
	private String pathDBLocation;
	
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
	public String getPathDBLocation() {
		return pathDBLocation;
	}
	/**
	 * @param pathDBLocation the pathDBLocation to set
	 */
	public void setPathDBLocation(String pathDBLocation) {
		this.pathDBLocation = pathDBLocation;
	}
	
}
