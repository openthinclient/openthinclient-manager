package org.openthinclient.runtime.web.comptest.ui.tests;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationResolver.ResolveState;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.util.dpkg.LocalPackageRepository;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.vaadin.viritin.button.MButton;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class InstallationPlanSummaryDialogTest extends VerticalLayout implements ComponentTest {

    /** serialVersionUID */
    private static final long serialVersionUID = 5697534476324346265L;
    
    private final InstallationPlanSummaryDialog dialog;

    public InstallationPlanSummaryDialogTest() {
        setSpacing(true);

        final PackageManager packageManager = createDummyPackageManager();
        final PackageManagerOperation op = packageManager.createOperation();
        final InstallPlan ip = op.getInstallPlan();
        
        dialog = new InstallationPlanSummaryDialog(op, packageManager);

        addComponent(new MButton("Open").withListener((Button.ClickListener) e -> dialog.open(false)));
        addComponent(new MButton("Close").withListener((Button.ClickListener) e -> dialog.close()));

        addComponent(new MButton("Add Install") //
                .withListener((Button.ClickListener) e -> addInstallStep(ip)));
        addComponent(new MButton("Add Uninstall") //
                .withListener((Button.ClickListener) e -> addUninstallStep(ip)));
        addComponent(new MButton("Add Update") //
                .withListener((Button.ClickListener) e -> addUpdateStep(ip)));

    }

    private void addInstallStep(InstallPlan ip) {
        ip.getSteps().add(new InstallPlanStep.PackageInstallStep(createPackage("test-pkg", "1.2-3")));
        dialog.update();
    }

    private void addUninstallStep(InstallPlan ip) {
        ip.getSteps().add(new InstallPlanStep.PackageUninstallStep(createPackage("dumb-pkg", "0.1-21")));
        dialog.update();
    }

    private void addUpdateStep(InstallPlan ip) {
        ip.getSteps().add(new InstallPlanStep.PackageVersionChangeStep(
                createPackage("test-pkg", "1.2-3"),
                createPackage("old-pkg", "1.5-3")
        ));
        dialog.update();
    }

    private Package createPackage(String name, String version) {
        final Package pkg = new Package();
        pkg.setName(name);
        pkg.setVersion(version);
        return pkg;
    }

    @Override
    public String getTitle() {
        return "Installation Summary Dialog";
    }

    @Override
    public String getDetails() {
        return "Summary of Steps to be executed based on an InstallPlan.";
    }

    @Override
    public Component get() {
        return this;
    }
    
    /**
     * Returns an NOT implemented PackageMananger
     * @return non working PackageMananger
     */
    private PackageManager createDummyPackageManager() {
      PackageManager packageManager = new PackageManager() {
        
        @Override
        public ListenableProgressFuture<PackageListUpdateReport> updateCacheDB() {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public boolean isInstalled(Package pkg) {
          // TODO Auto-generated method stub
          return false;
        }
        
        @Override
        public boolean isInstallable(Package pkg) {
          // TODO Auto-generated method stub
          return false;
        }
        
        @Override
        public Collection<Package> getUpdateablePackages() {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public SourcesList getSourcesList() {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public LocalPackageRepository getLocalPackageRepository() {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public Collection<Package> getInstalledPackages() {
          // TODO Auto-generated method stub
          return null;
        }

          @Override
          public Collection<Package> getInstallablePackagesWithoutInstalledOfSameVersion() {
              return null;
          }

          @Override
        public Collection<Package> getInstallablePackages() throws PackageManagerException {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public long getFreeDiskSpace() throws PackageManagerException {
          // TODO Auto-generated method stub
          return 0;
        }
        
        @Override
        public PackageManagerConfiguration getConfiguration() {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public Collection<String> getChangelogFile(Package package1) throws IOException {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public PackageManagerTaskSummary fetchTaskSummary() {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public ListenableProgressFuture<PackageManagerOperationReport> execute(
            PackageManagerOperation operation) {
          // TODO Auto-generated method stub
          return null;
        }
        
        @Override
        public PackageManagerOperation createOperation() {
          // TODO Auto-generated method stub
          return new PackageManagerOperation() {
            
            @Override
            public void uninstall(Package pkg) {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public void resolve() {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public boolean isResolved() {
              // TODO Auto-generated method stub
              return false;
            }
            
            @Override
            public void install(Package pkg) {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public boolean hasPackagesToUninstall() {
              // TODO Auto-generated method stub
              return false;
            }
            
            @Override
            public Collection<UnresolvedDependency> getUnresolved() {
              // TODO Auto-generated method stub
              return null;
            }
            
            @Override
            public Collection<Package> getSuggested() {
              // TODO Auto-generated method stub
              return null;
            }
            
            @Override
            public ResolveState getResolveState() {
              // TODO Auto-generated method stub
              return null;
            }
            
            @Override
            public InstallPlan getInstallPlan() {
              // TODO Auto-generated method stub
              return null;
            }
            
            @Override
            public Collection<PackageConflict> getConflicts() {
              // TODO Auto-generated method stub
              return null;
            }
          };
        }
        
        @Override
        public void close() throws PackageManagerException {
          // TODO Auto-generated method stub
          
        }
        
        @Override
        public boolean addWarning(String warning) {
          // TODO Auto-generated method stub
          return false;
        }

        @Override
        public void deleteSource(Source source) throws SourceIntegrityViolationException {
          // TODO Auto-generated method stub
          
        }

        @Override
        public Source saveSource(Source source) {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Collection<Source> findAllSources() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public void saveSources(List<Source> sources) {
          // TODO Auto-generated method stub
        }

      @Override
      public ListenableProgressFuture<PackageListUpdateReport> deleteSourcePackagesFromCacheDB(Source source) {
         // TODO Auto-generated method stub
         return null;
      }

        @Override
        public List<PackageInstalledContent> getInstalledPackageContents(Package pkg) {
          return null;
        }

      };
      return packageManager;
    }    
}
