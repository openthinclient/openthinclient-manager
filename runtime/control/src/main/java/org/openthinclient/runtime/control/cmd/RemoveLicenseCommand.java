package org.openthinclient.runtime.control.cmd;

import org.kohsuke.args4j.Option;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public class RemoveLicenseCommand extends AbstractCommand<RemoveLicenseCommand.Options> {

  public RemoveLicenseCommand() {
    super("rm-license");
  }

  @Override
  public Options createOptionsObject() {
    return new Options();
  }

  @Override
  public void execute(Options options) throws Exception {

    // FIXME validate that the manager server is not running at the moment

    final ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();
    managerHomeFactory.setManagerHomeDirectory(options.homePath.toFile());

    if (!managerHomeFactory.isManagerHomeValidAndInstalled()) {
      System.err.println("Not a valid manager home directory: " + options.homePath.toAbsolutePath());
      System.exit(1);
      return;
    }

    String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    String dbUrl = "jdbc:derby:" +
            options.homePath.resolve("db").resolve("manager").toString() +
            ";create=true";

    Class.forName(driver).newInstance();

    Connection conn = DriverManager.getConnection(dbUrl);

    conn.createStatement().execute("DELETE FROM SA.otc_license");
    conn.close();

    System.out.println("License removed.");
  }

  public class Options {
    @Option(name = "--home", required = true, metaVar = "DIR", usage = "The target manager home directory")
    public Path homePath;
  }
}
