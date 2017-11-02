package org.openthinclient.util.dpkg;

import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.progress.ProgressReceiver;
import org.openthinclient.progress.ProgressTask;

import java.util.List;
import java.util.Locale;

/**
 * Removes packages from database for a corresponding source
 */
public class RemoveFromDatabase implements ProgressTask<PackageListUpdateReport> {

   PackageManagerConfiguration configuration;
   Source source;
   PackageManagerDatabase packageManagerDatabase;
   
   public RemoveFromDatabase(PackageManagerConfiguration configuration, Source source, PackageManagerDatabase packageManagerDatabase) {
      this.configuration = configuration;
      this.source = source;
      this.packageManagerDatabase = packageManagerDatabase;
   }

   @Override
   public ProgressTaskDescription getDescription(Locale locale) {
      return null;
   }

   @Override
   public PackageListUpdateReport execute(ProgressReceiver progressReceiver) throws Exception {
      
      final PackageListUpdateReport report = new PackageListUpdateReport();
      
      List<Package> existingPackages = packageManagerDatabase.getPackageRepository().findBySource(source);
      for (int i = 0; i < existingPackages.size(); i++) {
         Package p = existingPackages.get(i);
         packageManagerDatabase.getPackageRepository().delete(p);
         report.incRemoved();
         progressReceiver.progress("Delete package " + p.getDisplayVersion(), i / existingPackages.size());
      };
      
      return report;
   }

}
