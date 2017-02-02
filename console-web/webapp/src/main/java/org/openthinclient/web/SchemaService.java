package org.openthinclient.web;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchemaService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaService.class);

  private final PackageManager packageManager;
  private final ApplicationService applicationService;
  private final SchemaProvider schemaProvider;

  public SchemaService(PackageManager packageManager, ApplicationService applicationService, SchemaProvider schemaProvider) {
    this.packageManager = packageManager;
    this.applicationService = applicationService;
    this.schemaProvider = schemaProvider;
  }

  public Stream<? extends Schema<?>> findAffectedSchemas(InstallPlan installPlan) {

    return installPlan.getPackageUninstallSteps() //
            .flatMap(this::getInstalledContents) //
            .filter(this::isSchemaFilePath) //
            .map(this::loadSchema) //
            .filter(Objects::nonNull) //
            ;
  }

  public Collection<Application> findAffectedApplications(InstallPlan installPlan) {
    return findAffectedSchemas(installPlan) //
            .flatMap((schema) -> applicationService.findAllUsingSchema(schema).stream()) //
            .collect(Collectors.toList());
  }

  private Stream<PackageInstalledContent> getInstalledContents(InstallPlanStep.PackageUninstallStep packageUninstallStep) {
    final Package pkg = packageUninstallStep.getInstalledPackage();
    final List<PackageInstalledContent> contents = packageManager.getInstalledPackageContents(pkg);

    if (contents == null) {
      return Stream.empty();
    }
    return contents.stream();
  }

  private Schema<?> loadSchema(PackageInstalledContent packageInstalledContent) {

    final Path path = packageInstalledContent.getPath();

    // this will be the profile type, like Application, Client, etc.
    final String profileTypeName = path.getName(1).toString();
    // this is the actual schema
    final String schemaFileName = path.getName(2).toString();
    // remove the .xml extension
    final String schemaName = schemaFileName.substring(0, schemaFileName.length() - 4);

    final ProfileType profileType = ProfileType.get(profileTypeName);

    if (profileType == null) {
      LOGGER.error("failed to map profile type name {}", profileTypeName);
      return null;
    }

    // to ensure that the schema provider will include the most recent list of schemas, we're forcing a reload at this point
    schemaProvider.reload();

    return schemaProvider.getSchema(profileType.getProfileClass(), schemaName);
  }

  protected boolean isSchemaFilePath(PackageInstalledContent packageInstalledContent) {

    // schema files are not directories
    if (packageInstalledContent.getType() == PackageInstalledContent.Type.DIR)
      return false;

    return isSchemaFilePath(packageInstalledContent.getPath());

  }

  protected boolean isSchemaFilePath(Path path) {
    // only xml schemas are possible
    if (!path.getFileName().toString().endsWith(".xml"))
      return false;

    // possible path values are:
    // schema/application.xml
    // schema/application/my-application.xml

    // this logic will only handle specific schemas (like my-application in the example above) and ignore "top-level" schemas.
    // The top-level schemas represent the core data model and may never be touched, unless death and destruction is desired.


    return path.getNameCount() == 3 && //
            path.startsWith(Paths.get("schema", "application"));
  }

  public enum ProfileType {
    APPLICATION("application", Application.class), //
    HARDWARE_TYPE("hardwaretype", HardwareType.class), //
    DEVICE("device", Device.class), //
    LOCATION("location", Location.class), //
    CLIENT("client", Client.class),
    PRINTER("printer", Printer.class);
    private final String profileName;
    private final Class<?> profileClass;

    ProfileType(String profileName, Class<?> profileClass) {
      this.profileName = profileName;
      this.profileClass = profileClass;
    }

    public static ProfileType get(String name) {
      for (ProfileType profileType : ProfileType.values()) {
        if (profileType.getProfileName().equals(name))
          return profileType;
      }
      return null;
    }

    public Class<?> getProfileClass() {
      return profileClass;
    }

    public String getProfileName() {
      return profileName;
    }
  }
}
