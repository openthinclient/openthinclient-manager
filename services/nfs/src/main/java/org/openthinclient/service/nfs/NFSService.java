/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.service.nfs;

import org.openthinclient.service.common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v3.MountServer;
import org.dcache.nfs.v3.NfsServerV3;
import org.dcache.nfs.v4.MDSOperationFactory;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.dcache.vfs4j.LocalVFS;

public class NFSService implements Service<NFSServiceConfiguration>,NFS {
	private static final Logger logger = LoggerFactory.getLogger(NFSService.class);
	private NFSServiceConfiguration configuration;
	
	@Override
	public void setConfiguration(NFSServiceConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public NFSServiceConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public Class<NFSServiceConfiguration> getConfigurationClass() {
		return NFSServiceConfiguration.class;
	}	
	@Override
	public void startService() throws Exception {
		// Launches NFS v4 server
		// Usage example: https://sea-region.github.com/kofemann/vfs4j/blob/master/src/main/java/org/dcache/vfs4j/NfsMain.java
		// Attention: configuration has not been implemented yet
		try {
			String nfs_path = System.getProperty("manager.home", System.getProperty("user.dir")) + "/nfs";
			String exports_data = "";
			exports_data += "/root *(ro,all_squash,anonuid=1000,anongid=1000,nopnfs)\n";
			exports_data += "/openthinclient *(ro,all_squash,anonuid=1000,anongid=1000,nopnfs)\n";
			exports_data += "/home *(rw,all_squash,anonuid=1000,anongid=1000,nopnfs)\n";
			String optinal_exports_path = nfs_path + "/exports";

			logger.info("Using NFS root path \"" + nfs_path + "\"...");
			VirtualFileSystem vfs = new LocalVFS(new File(nfs_path));

			File exports_file = new File(optinal_exports_path);
			logger.info("Looking for optional exports-file \"" + optinal_exports_path + "\"...");
			ExportFile exports;
			if (!exports_file.exists()) {
				exports = new ExportFile(new StringReader(exports_data));
			} else {
				logger.info("Optional exports-file exists. Parsing...");
				exports = new ExportFile(exports_file.toURI());
			}

			logger.info("Creating RPC server...");
			OncRpcSvc rpc_server = new OncRpcSvcBuilder()
				.withPort(2049)
				.withTCP()
				.withAutoPublish()
				.withWorkerThreadIoStrategy()
				.build();

			logger.info("Creating MOUNTD server...");
			MountServer mountd_server = new MountServer(exports, vfs);

			logger.info("Creating NFSv3 server...");
			NfsServerV3 nfs3_server = new NfsServerV3(exports, vfs);

			logger.info("Creating NFSv4 server...");
			NFSServerV41 nfs4_server = new NFSServerV41.Builder()
				.withExportFile(exports)
				.withVfs(vfs)
				.withOperationFactory(new MDSOperationFactory())
				.build();

			logger.info("Registering MOUNTD, NFSv3, NFSv4 in RPC...");
			rpc_server.register(new OncRpcProgram(100003, 4), nfs4_server);
			rpc_server.register(new OncRpcProgram(100003, 3), nfs3_server);
			rpc_server.register(new OncRpcProgram(100005, 3), mountd_server);

			logger.info("Launching RPC server...");
			rpc_server.start();
			logger.info("Done.");
		} catch (java.io.IOException e) {
			logger.error("Exception during NFS launch: " + e);
		}
	}

	@Override
	public void stopService() throws Exception {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
	}

	@Override
  public void addExport(String exportSpec) throws UnknownHostException {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
	}

	@Override
  public void addExport(NFSExport export) {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
	}

	@Override
  public boolean removeExport(String name) {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
		return false;
	}

	public void setExports(org.w3c.dom.Element o) {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
	}

	@Override
  public boolean moveLocalFile(File from, File to) {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
		return false;
	}

	@Override
  public boolean moveMoreFiles(HashMap<File, File> fromToMap) {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
		return false;
	}

	@Override
  public boolean removeFilesFromNFS(List<File> fileList) {
		logger.error(Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + " has not been implemented yet");
		return false;
	}

	public static void main(String[] args) throws Exception {
		logger.info("Launching " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + "...");

		final NFSService self = new NFSService();
		self.startService();

		System.err.println("NFS server is ready. Press ENTER to terminate.");
		System.in.read();
	}
}
