package org.openthinclient.runtime.control.cmd;

import org.kohsuke.args4j.Option;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;

import java.nio.file.Path;

public class RemoveServerIdCommand extends AbstractCommand<RemoveServerIdCommand.Options> {

  public RemoveServerIdCommand() {
    super("rm-server-id");
  }

  @Override
  public Options createOptionsObject() {
    return new Options();
  }

  @Override
  public void execute(Options options) throws Exception {

    // FIXME validate that the manager server is not running at the moment

    final DefaultManagerHome managerHome = new DefaultManagerHome(options.homePath.toFile());

    managerHome.getMetadata().setServerID(null);
    managerHome.getMetadata().save();

  }

  public class Options {
    @Option(name = "--home", required = true, metaVar = "DIR", usage = "The target manager home directory")
    public Path homePath;
  }
}
