package org.openthinclient.pkgmgr.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
public class PackageInstallTest {

  private static DebianTestRepositoryServer testRepositoryServer;
  
  @Autowired
  PackageManagerConfiguration configuration;

  @BeforeClass
  public static void startRepoServer() {
    testRepositoryServer = new DebianTestRepositoryServer();
    testRepositoryServer.start();
  }
  
  @AfterClass
  public static void shutdownRepoServer() {
    testRepositoryServer.stop();
    testRepositoryServer = null;
  }

  @Test
  public void testInstallSinglePackage() throws Exception {
    String[] packages = {"foo", "zonk"};
    
    for ( String pkgName : packages) {
      final DPKGPackageManager packageManager = preparePackageManager();

      final Optional<org.openthinclient.util.dpkg.Package> testPackage = packageManager.getInstallablePackages()
              .stream()
              .filter(pkg -> pkg.getName().equals(pkgName))
              .findFirst();

      assertTrue(pkgName + "-Package wasn't avaible",testPackage.isPresent());
      assertTrue("couldn't install " + pkgName + "-package",packageManager.install(Collections.singletonList(testPackage.get())));

      final Path installDirectory = configuration.getInstallDir().toPath();
      final Path testInstallDirectory = configuration.getTestinstallDir().toPath();
      
      Path[] pkgPath = getFilePathsInPackage(pkgName, installDirectory);
      for (Path file : pkgPath)
        assertFileExists(file);
      assertEquals("test-install-directory isn't empty",0,testInstallDirectory.toFile().listFiles().length);
      
      resetTestData("single", pkgName, packageManager);
  }
}
 
  @Test
  public void testInstallSinglePackageDependingOther() throws Exception {
    final DPKGPackageManager packageManager = preparePackageManager();

    final Optional<org.openthinclient.util.dpkg.Package> bar2Package = packageManager.getInstallablePackages()
            .stream()
            .filter(pkg -> pkg.getName().equals("bar2"))
            .findFirst();

    assertTrue(bar2Package.isPresent());
    assertTrue(packageManager.install(Collections.singletonList(bar2Package.get())));

    final Path installDirectory = configuration.getInstallDir().toPath();
    final Path testInstallDirectory = configuration.getTestinstallDir().toPath();    
    
    Path[] fooPath = getFilePathsInPackage("foo", installDirectory);
    Path[] barPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : fooPath)
    	assertFileExists(file);
    for (Path file: barPath)
    	assertFileExists(file);
    
    assertEquals(0,testInstallDirectory.toFile().listFiles().length);
    
    resetTestData("depending","foo-bar2", packageManager);
  }

  private DPKGPackageManager preparePackageManager() throws Exception {
    final DPKGPackageManager packageManager = PackageManagerFactory.createPackageManager(configuration);
  
    
    writeSourcesList();

    assertNotNull("sources-list could not be loaded",packageManager.getSourcesList());
    assertEquals("number of entries in sources list is not correct",1, packageManager.getSourcesList().getSources().size());
    assertEquals("wrong URL of repository",testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

    //assertEquals(0, packageManager.getInstallablePackages().size());
    assertTrue("couldn't update cache-DB",packageManager.updateCacheDB());
    assertEquals("wrong number of installables packages",4, packageManager.getInstallablePackages().size());

    saveDB();
    
    return packageManager;
  }

private void writeSourcesList() throws Exception {

    try (final FileOutputStream out = new FileOutputStream(configuration.getSourcesList())) {
      out.write(("deb " + testRepositoryServer.getServerUrl().toExternalForm() + " ./").getBytes());
    }
  }

  private void saveDB() throws Exception {
	  File dbDir = configuration.getPackageDB().getParentFile();
	  for (File dbFile : dbDir.listFiles()) {
		  File outFile = new File(dbFile.getPath()+"-save");
		  FileInputStream is = new FileInputStream(dbFile);
		  FileOutputStream os = new FileOutputStream(outFile);
		  while(is.available() != 0) {
			  os.write(is.read());
		  }
		  is.close();
		  os.close();
	  }
  }
  
  private void restoreDB() throws Exception {
	  File dbDir = configuration.getPackageDB().getParentFile();
	  for (File dbFile : dbDir.listFiles()) {
		  if (dbFile.getPath().contains("-save")) {
			  File outFile = new File(dbFile.getPath().substring(0, dbFile.getPath().length() - "-save".length() ));
			  FileInputStream is = new FileInputStream(dbFile);
			  FileOutputStream os = new FileOutputStream(outFile);
			  while(is.available() != 0) {
				  os.write(is.read());
			  }
			  is.close();
			  os.close();
			  dbFile.delete();
		  }
	  }
  }

  private void resetTestData(String testcase, String pkgNames, DPKGPackageManager manager) throws Exception {
	File root = configuration.getInstallDir().getParentFile();
	
	File oldInstall = configuration.getInstallDir();
	File newInstall = new File(root, "install");
	File oldTestInstall = configuration.getTestinstallDir();
	File newTestInstall = new File(root, "install-test");
	oldInstall.renameTo(new File(oldInstall.toString() + "-" + testcase + "-" + pkgNames));
	oldTestInstall.renameTo(new File(oldTestInstall.toString() + "-" + testcase + "-" + pkgNames));	
	newInstall.mkdir();
	newTestInstall.mkdir();
	
	restoreDB();
  }
  
  @Configuration()
  @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
  public static class PackageManagerConfig {

  }
  
  private void assertFileExists(Path path) {
	    assertTrue(path.getFileName() + " does not exist", Files.exists(path));
	    assertTrue(path.getFileName() + " is not a regular file", Files.isRegularFile(path));
  }
  
  private Path[] getFilePathsInPackage(String pkg, Path directory) {
	  Path[] filePaths = new Path[3];
	  filePaths[0] = directory.resolve("schema").resolve("application").resolve(pkg + ".xml");
	  filePaths[1] = directory.resolve("schema").resolve("application").resolve(pkg + "-tiny.xml.sample");
	  filePaths[2] = directory.resolve("sfs").resolve("package").resolve(pkg + ".sfs");
	  return filePaths;
  }
}
