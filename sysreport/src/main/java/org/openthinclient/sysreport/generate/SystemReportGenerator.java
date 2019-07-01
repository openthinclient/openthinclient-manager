package org.openthinclient.sysreport.generate;

import com.google.common.primitives.Bytes;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.SystemReport;

import java.util.stream.Collectors;

public class SystemReportGenerator extends AbstractReportGenerator<SystemReport> {


  public SystemReportGenerator(ManagerHome managerHome, PackageManager packageManager) {
    super(managerHome);
    contributors.add(new NetworkInterfaceDetailsContributor());
    contributors.add(new PackageManagerReportContributor(packageManager));
  }

  @Override
  public SystemReport generateReport() {

    final SystemReport report = super.generateReport();

    report.getSubmitter().setSubmitterType(SystemReport.SubmitterType.AUTOMATED);
    report.getSubmitter().setName("openthinclient.org Manager, Report Generator");

    return report;
  }

  @Override
  protected SystemReport createReportInstance() {
    return new SystemReport();
  }

  public static String toMacAddressString(byte[] address) {
    return Bytes.asList(address).stream()
            .map(b -> String.format("%02x", b))
            .collect(Collectors.joining(":"));
  }

}
