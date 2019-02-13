package org.openthinclient.service.nfs;

import java.io.File;

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

public class NFSService {
	public NFSService() {
		try {
	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			VirtualFileSystem vfs = new LocalVFS(new File(""));

	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			OncRpcSvc nfsSvc = new OncRpcSvcBuilder()
				.withPort(2049)
				.withTCP()
				.withAutoPublish()
				.withWorkerThreadIoStrategy()
				.build();

			File exports_file_descriptor = new File("/tmp/github/openthinclient-manager/services/nfs/exports");
			System.err.println("Exports-file exists? " + exports_file_descriptor.exists());
	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			ExportFile exports_file = new ExportFile(exports_file_descriptor.toURI());

	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			NfsServerV3 nfs3 = new NfsServerV3(exports_file, vfs);
	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			MountServer mountd = new MountServer(exports_file, vfs);

	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			NFSServerV41 nfs4 = new NFSServerV41.Builder()
				.withExportFile(exports_file)
				.withVfs(vfs)
				.withOperationFactory(new MDSOperationFactory())
				.build();

			// Registers servers at RPC service
	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			nfsSvc.register(new OncRpcProgram(100003, 4), nfs4);
	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			nfsSvc.register(new OncRpcProgram(100003, 3), nfs3);
	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			nfsSvc.register(new OncRpcProgram(100005, 3), mountd);

			// Launches RPC service
	// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
			nfsSvc.start();
		} catch (java.io.IOException e) {
		}
	}

	public static void main(String[] args) throws Exception {
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		VirtualFileSystem vfs = new LocalVFS(new File(""));

// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		OncRpcSvc nfsSvc = new OncRpcSvcBuilder()
			.withPort(2049)
			.withTCP()
			.withAutoPublish()
			.withWorkerThreadIoStrategy()
			.build();

		File exports_file_descriptor = new File("exports");
		System.err.println("Exports-file exists? " + exports_file_descriptor.exists());
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		ExportFile exports_file = new ExportFile(exports_file_descriptor.toURI());

// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		NfsServerV3 nfs3 = new NfsServerV3(exports_file, vfs);
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		MountServer mountd = new MountServer(exports_file, vfs);

// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		NFSServerV41 nfs4 = new NFSServerV41.Builder()
			.withExportFile(exports_file)
			.withVfs(vfs)
			.withOperationFactory(new MDSOperationFactory())
			.build();

		// Registers servers at RPC service
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		nfsSvc.register(new OncRpcProgram(100003, 4), nfs4);
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		nfsSvc.register(new OncRpcProgram(100003, 3), nfs3);
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		nfsSvc.register(new OncRpcProgram(100005, 3), mountd);

		// Launches RPC service
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");
		nfsSvc.start();
// System.err.println(new java.text.SimpleDateFormat("HH:mm:ss.SSS ").format(new java.util.Date()) + "<<< PROFILE >>> " + "[ExportFile.java] " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + "():" + Thread.currentThread().getStackTrace()[1].getLineNumber() + ": ");

		System.err.println("NFS is ready. Press Ctrl+C to terminate.");
		System.in.read();
	}
}
