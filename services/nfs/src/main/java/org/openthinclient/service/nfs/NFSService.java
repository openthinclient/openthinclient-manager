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

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.apps.jportmap.OncRpcEmbeddedPortmap;
import org.acplt.oncrpc.server.OncRpcServerStub;
import org.openthinclient.mountd.ListExporter;
import org.openthinclient.mountd.MountDaemon;
import org.openthinclient.nfsd.NFSServer;
import org.openthinclient.nfsd.PathManager;
import org.openthinclient.service.common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author levigo
 */
public class NFSService implements Service<NFSServiceConfiguration>,NFS {

	private static final Logger LOG = LoggerFactory.getLogger(NFSService.class);

	private static String ATTR_SPEC = "spec";
	private static String ATTR_NAME = "name";
	private static String ATTR_ROOT = "root";

	private class RpcServerThread extends Thread {
		protected OncRpcServerStub server;
		private boolean terminateCalled = false;

		RpcServerThread(String name, OncRpcServerStub server) {
			super(name);
			this.server = server;
			setDaemon(true);
			start();
		}

		@Override
		public void run() {
			try {
				LOG.info("Starting " + getName());
				doRunServer();
				synchronized (this) {
					if (!terminateCalled)
						LOG.error(getName() + " terminated unexpectedly.");
					else
						LOG.info(getName() + " terminated");
				}
			} catch (final Throwable e) {
				LOG.error(getName() + " died with exception.", e);
			}
		}

		/**
		 * @throws OncRpcException
		 * @throws IOException
		 */
		protected void doRunServer() throws OncRpcException, IOException {
			server.run();
		}

		/**
		 * This method is supposed to be called from outside the thread.
		 */
		public void terminate() {
			synchronized (this) {
				terminateCalled = true;
			}
			LOG.debug("Stopping " + getName());
			server.stopRpcProcessing();
			try {
				this.join();
			} catch (final InterruptedException e) {
				LOG.error("Exception shutting down " + getName(), e);
			}
		}
	}

	private RpcServerThread nfsServer;

	private RpcServerThread mountDaemon;

	private RpcServerThread myPortmapper;

	private final ListExporter exporter = new ListExporter();

	private PathManager pathManager;

	private Timer flushTimer;

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

		// configure all configured exports
		exporter.getExports().addAll(getConfiguration().getExports());

		// Check for PORTMAP, if there isn't one, start embedded PM
		if (OncRpcEmbeddedPortmap.isPortmapRunning())
			LOG.info("Portmapper already running");
		else {
			LOG.info("Portmapper not found; starting PORTMAP server.");
			myPortmapper = new RpcServerThread("portmapper", new Portmapper(configuration.getPortmapPort(), configuration.getPortmapProgramNumber())) {
				@Override
				protected void doRunServer() throws OncRpcException, IOException {
					// the portmapper needs some special treatment
					this.server.run(this.server.transports);
				}
			};
		}

		pathManager = new PathManager(null != configuration.getPathDBLocation() ?
						configuration.getPathDBLocation() : null, exporter);
		nfsServer = new RpcServerThread("NFS server", new NFSServer(pathManager,
						configuration.getNfsPort(), configuration.getNfsProgramNumber()));

		mountDaemon = new RpcServerThread("mount daemon", new MountDaemon(
						pathManager, exporter, configuration.getMountdPort(), configuration.getMountdProgramNumber()));

		if (configuration.getFlushInterval() > 0) {
			flushTimer = new Timer("NFS database flush", true);
			flushTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (null != pathManager)
						try {
							pathManager.flushPathDatabase();
						} catch (final IOException e) {
							LOG.warn("Could not flush path database", e);
						}
				}
			}, configuration.getFlushInterval() * 1000, configuration.getFlushInterval() * 1000);
		}
	}

	@Override
	public void stopService() throws Exception {
		if (null != flushTimer) {
			flushTimer.cancel();
			flushTimer = null;
		}

		if (mountDaemon != null) {
			LOG.info("Stopping mount daemon");
			mountDaemon.terminate();
			mountDaemon = null;
		}

		if (nfsServer != null) {
			LOG.info("Stopping NFS server");
			nfsServer.terminate();
			nfsServer = null;
		}

		if (myPortmapper != null) {
			LOG.info("Stopping embedded portmapper");
			myPortmapper.terminate();
			myPortmapper = null;
		}

		if (pathManager != null) {
			LOG.info("Stopping path manager");
			pathManager.shutdown();
			pathManager = null;
		}
	}

	/*
	 * public void setExports(Exports exports) { exporter.getExports().clear(); if
	 * (null != exports) for (NFSExport export : exports)
	 * exporter.addExport(export); }
	 */

	@Override
  public void addExport(String exportSpec) throws UnknownHostException {
		LOG.info("Adding export: " + exportSpec);

		final ExportsParser parser = new ExportsParser();

		exporter.addExport(parser.parse(exportSpec));
	}

	@Override
  public void addExport(NFSExport export) {
		LOG.info("Exporting " + export);
		exporter.addExport(export);
	}

	@Override
  public boolean removeExport(String name) {
		boolean result;
		if (result = exporter.removeExport(name))
			LOG.info("Removed NFSExport: " + name);
		else
			LOG.info("NFSExport to be removed not found: " + name);
		return result;
	}

	/**
	 * Wrapper for addExport, creating new TFTPExport-Objects, using the
	 * configured TFTP-Xports
	 */
	public void setExports(org.w3c.dom.Element o) {
		/* We don't care about the "root" */

		final NodeList nl = o.getChildNodes();
		if (nl != null)
			/* Any entries ?? */
			for (int i = 0; i < nl.getLength(); i++) {
				/* get the Actual Node */
				final Node nod = nl.item(i);

				if (nod != null) {
					/* pre-(null)initialized entries */
					String sSpec = null;
					String sName = null;
					String sRoot = null;

					final NamedNodeMap nnm = nod.getAttributes();
					/* There are a few Attributes */
					if (nnm != null) {
						/* get the Named Attributes/Items */
						final Node nSpec = nnm.getNamedItem(ATTR_SPEC);
						final Node nName = nnm.getNamedItem(ATTR_NAME);
						final Node nRoot = nnm.getNamedItem(ATTR_ROOT);

						if (nSpec != null)
							sSpec = nSpec.getNodeValue();

						if (nName != null)
							sName = nName.getNodeValue();

						if (nRoot != null)
							sRoot = nRoot.getNodeValue();

						if (sSpec != null) {
							try {

								final ExportsParser parser = new ExportsParser();

								addExport(parser.parse(sSpec));
							} catch (final UnknownHostException uhe) {
								LOG.warn("INVALID Configuration - unknown Host:", uhe);
							}
							continue;
						}
						if (sName != null && sRoot != null) {
							final NFSExport export = new NFSExport();
							export.setName(sName);
							export.setRoot(new File(sRoot));
							addExport(export);
							continue;
						}
						LOG.warn("INVALID Configuration" + nnm.toString());
					}
				}
			}
	}

	@Override
  public boolean moveLocalFile(File from, File to) {
		if (!from.exists())
			return false;

		pathManager.flushFile(from);
		pathManager.flushFile(to);

		if (!pathManager.handleForFileExists(from))
			return from.renameTo(to);
		else if (from.renameTo(to))
			if (pathManager.createMissigHandles(to)) {
				pathManager.movePath(from, to);
				return true;
			}
		return false;
	}

	@Override
  public boolean moveMoreFiles(HashMap<File, File> fromToMap) {
		final Iterator it = fromToMap.keySet().iterator();
		while (it.hasNext()) {
			final File keyFile = (File) it.next();
			if (!moveLocalFile(keyFile, fromToMap.get(keyFile)))
				return false;
		}
		return true;
	}

	@Override
  public boolean removeFilesFromNFS(List<File> fileList) {
		return pathManager.removeFileFromNFS(fileList);
	}

}
