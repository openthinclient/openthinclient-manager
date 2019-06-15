package org.openthinclient.sysreport.generate;

import org.openthinclient.sysreport.AbstractReport;

import java.util.*;
import java.io.*;

public class ManagerReportContributor implements ReportContributor<AbstractReport> {

  private static final Set<String> PROPERTIES_WHITELIST;

  static {
    final Set<String> whitelist = new HashSet<>();

    whitelist.add("awt.toolkit".toLowerCase());
    whitelist.add("java.specification.version".toLowerCase());
    whitelist.add("file.encoding.pkg".toLowerCase());
    whitelist.add("java.vm.vendor".toLowerCase());
    whitelist.add("java.vendor.url".toLowerCase());
    whitelist.add("user.timezone".toLowerCase());
    whitelist.add("os.name".toLowerCase());
    whitelist.add("java.vm.specification.version".toLowerCase());
    whitelist.add("user.country".toLowerCase());
    whitelist.add("user.language".toLowerCase());
    whitelist.add("java.specification.vendor".toLowerCase());
    whitelist.add("file.separator".toLowerCase());
    whitelist.add("line.separator".toLowerCase());
    whitelist.add("java.specification.name".toLowerCase());
    whitelist.add("java.vm.specification.vendor".toLowerCase());
    whitelist.add("java.awt.graphicsenv".toLowerCase());
    whitelist.add("java.awt.headless".toLowerCase());
    whitelist.add("java.runtime.version".toLowerCase());
    whitelist.add("os.version".toLowerCase());
    whitelist.add("java.runtime.name".toLowerCase());
    whitelist.add("file.encoding".toLowerCase());
    whitelist.add("java.vm.name".toLowerCase());
    whitelist.add("java.vendor.url.bug".toLowerCase());
    whitelist.add("java.version".toLowerCase());
    whitelist.add("os.arch".toLowerCase());
    whitelist.add("java.vm.specification.name".toLowerCase());
    whitelist.add("java.vm.info".toLowerCase());
    whitelist.add("java.vendor".toLowerCase());
    whitelist.add("java.vm.version".toLowerCase());

    PROPERTIES_WHITELIST = Collections.unmodifiableSet(whitelist);
  }

  @Override
  public void contribute(AbstractReport report) {
    final Map<String, String> properties = new HashMap<>();
    final List<String> propertyKeys = new ArrayList<>();

    System.getProperties().forEach((key, value) -> {
      if (PROPERTIES_WHITELIST.contains(("" + key).toLowerCase())) {
        properties.put("" + key, "" + value);
      }
      propertyKeys.add("" + key);
    });

    report.getManager().getJava().setProperties(properties);
    report.getManager().getJava().setPropertyKeys(propertyKeys);
    report.getManager().setVersion(readVersion());
  }

  private String readVersion() {
    final Properties props = new Properties();
    try {
      final InputStream in = ClassLoader.getSystemResourceAsStream("application.properties");
      props.load(in);
      in.close();
    } catch(IOException ex) {
    }
    return props.getProperty("application.version", null);
  }
}
