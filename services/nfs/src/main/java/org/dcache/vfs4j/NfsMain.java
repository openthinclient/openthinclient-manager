package org.dcache.vfs4j;

import org.dcache.nfs.ExportFile;
import org.dcache.nfs.v3.MountServer;
import org.dcache.nfs.v3.NfsServerV3;
import org.dcache.nfs.v4.MDSOperationFactory;
import org.dcache.nfs.v4.NFSServerV41;
import org.dcache.nfs.v4.xdr.nfs4_prot;
import org.dcache.nfs.vfs.VirtualFileSystem;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;

import java.io.File;

public class NfsMain {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.println("Usage: NfsMain <path> <export file>");
            System.exit(1);
        }

        VirtualFileSystem vfs = new LocalVFS( new File(args[0]));
        OncRpcSvc nfsSvc = new OncRpcSvcBuilder()
                .withPort(2049)
                .withTCP()
                .withAutoPublish()
                .withWorkerThreadIoStrategy()
                .build();

        ExportFile exportFile = new ExportFile( new File(args[1]));

        NFSServerV41 nfs4 = new NFSServerV41.Builder()
                .withExportFile(exportFile)
                .withVfs(vfs)
                .withOperationFactory(new MDSOperationFactory())
                .build();

        NfsServerV3 nfs3 = new NfsServerV3(exportFile, vfs);
        MountServer mountd = new MountServer(exportFile, vfs);

        nfsSvc.register(new OncRpcProgram(100003, 3), nfs3);
        nfsSvc.register(new OncRpcProgram(100005, 3), mountd);

        nfsSvc.register(new OncRpcProgram(nfs4_prot.NFS4_PROGRAM, nfs4_prot.NFS_V4), nfs4);
        nfsSvc.start();

        Thread.currentThread().join();
    }
}

