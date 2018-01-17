package org.openthinclient.pkgmgr.it;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.progress.ListenableProgressFuture;

import java.util.List;
import java.util.function.Consumer;

public class PackageManagerTestUtils {

    public static void doInstallPackages(PackageManager packageManager, List<Package> packages) throws Exception {

        final PackageManagerOperation operation = packageManager.createOperation();
        final Consumer<Package> consumer = operation::install;

        forAllExecute(packageManager, operation, packages, consumer);
    }

    private static void forAllExecute(PackageManager packageManager, PackageManagerOperation operation, List<Package> packages, Consumer<Package> consumer) throws InterruptedException, java.util.concurrent.ExecutionException {

       packages.forEach(consumer);
       operation.resolve();

       final ListenableProgressFuture<PackageManagerOperationReport> future = packageManager.execute(operation);

       // block until the operation has been executed
       future.get();
    }

    public static void doUninstallPackages(PackageManager packageManager, List<Package> packages) throws Exception {

        final PackageManagerOperation operation = packageManager.createOperation();
        final Consumer<Package> consumer = operation::uninstall;

        forAllExecute(packageManager, operation, packages, consumer);
    }
    
}
