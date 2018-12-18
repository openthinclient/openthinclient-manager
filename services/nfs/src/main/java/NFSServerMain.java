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
 *******************************************************************************/
/*
 * This code is based on: 
 * JNFSD - Free NFSD. Mark Mitchell 2001 markmitche11@aol.com
 * http://hometown.aol.com/markmitche11
 */

import org.acplt.oncrpc.apps.jportmap.OncRpcEmbeddedPortmap;
import org.acplt.oncrpc.apps.jportmap.jportmap;
import org.openthinclient.mountd.Exporter;
import org.openthinclient.mountd.ListExporter;
import org.openthinclient.mountd.MountDaemon;
import org.openthinclient.nfsd.NFSServer;
import org.openthinclient.nfsd.PathManager;
import org.openthinclient.service.nfs.NFSExport;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.io.File;


class _Portmap extends jportmap {
	public _Portmap() throws
		org.acplt.oncrpc.OncRpcException,
		java.io.IOException {
	}
} 


public class NFSServerMain {
	public static void main(String[] args) throws Exception {
		_Portmap portmap_server = null;

		// Launches Portmap-Server (if not running)
		System.err.println("Checking if Portmap-Server already launched...");
		if (OncRpcEmbeddedPortmap.isPortmapRunning()) {
			System.err.println("Portmap-Server is already running");
		} else {
			System.err.println("Portmap-Server not found");
			try {
				System.err.println("Creating Portmap-Server...");

				portmap_server = new _Portmap();
				final _Portmap _portmap_server = portmap_server;

				// Launches Portmap-Server
				new Thread("Portmap-Server") {
					@Override
					public void run() {
						try {
							System.err.println("Launching portmapper on port " + _Portmap.PMAP_PORT + " (in a separate thread)");
							_portmap_server.run(_portmap_server.transports);
							System.err.println("Portmap-Server terminated");
						} catch (Throwable e) {
							e.printStackTrace();
							System.err.println("Failure in Portmap-Server");
							System.exit(1);
						}
					}
				}.start();
			} catch (Throwable e) {
				e.printStackTrace();
				System.err.println("Failed to launch Portmap-Server");
				System.exit(1);
			}
		}

		// Creates list of exported_resources
		NFSExport resources[] = new NFSExport[1];
		resources[0] = new NFSExport();
		resources[0].setName("/share");
		resources[0].setRoot(new File("./share").getAbsoluteFile());
		final Exporter exporter = new ListExporter(resources);

		final PathManager path_manager = new PathManager(new File("nfs-handles.db"), exporter);

		// Launches NFS-Server
		final NFSServer nfs_server = new NFSServer(path_manager, 0, 0);
		new Thread("NFS-Server") {
			@Override
			public void run() {
				try {
					System.err.println("Launching NFS-Server (in a separate thread)");
					nfs_server.run();
					System.err.println("NFS-Server terminated");
				} catch (Throwable e) {
					e.printStackTrace();
					System.err.println("Failure in NFS-Server");
					System.exit(1);
				}
			}
		}.start();

		// Launches Mountd-Server
		final MountDaemon mountd_server = new MountDaemon(path_manager, exporter, 0, 0);
		new Thread("Mountd-Server") {
			@Override
			public void run() {
				// setName("Mountd-Server");
				try {
					System.err.println("Launching Mountd-Server (in a separate thread)");
					mountd_server.run();
					System.err.println("Mountd-Server terminated");
				} catch (Throwable e) {
					e.printStackTrace();
					System.err.println("Failure in Mountd-Server");
					System.exit(1);
				}
			}
		}.start();

		System.out.println("Waiting...");
		try {
			Thread.sleep(999999999);  // 31 year
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// If we are the portmap server, then wait for it to die
		// If it does, kill proc
		//    if (portmap_server != null) {
		//      try {
		//        portmap_server.getEmbeddedPortmapServiceThread().join();
		//        System.err.println("Portmap-Server terminated");
		//      } catch (Throwable e) {
		//        e.printStackTrace();
		//        System.err.println("Portmap-Server was interrupted");
		//      }
		//    }
		System.out.println("Done.");

		nfs_server.stopRpcProcessing();
		mountd_server.stopRpcProcessing();
		portmap_server.stopRpcProcessing();

		System.out.println("Main process was terminated");
	}
}
