package org.openthinclient.runtime.control.cmd;

import static org.openthinclient.common.ApplicationVersionUtil.readBuildDateFromPomProperties;
import static org.openthinclient.common.ApplicationVersionUtil.readVersionFromPomProperties;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows the version of application
 */
public class ShowVersionCommand extends AbstractCommand<ShowVersionCommand.Options> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShowVersionCommand.class);

  public ShowVersionCommand() {
    super("version");
  }

  @Override
  public Options createOptionsObject() {
    return new Options();
  }

  @Override
  public void execute(Options options) throws Exception {

    String version = readVersionFromPomProperties();
    LOGGER.debug("Application version is {}", version);
    System.out.println("Application version is " + version==null ? "unknown" : version);

    if (options.detail) {
      String buildDate = readBuildDateFromPomProperties();
      System.out.println("Creation date " + buildDate==null ? "unknown" : buildDate);
    }
  }


  public class Options {

    /**
     * Whether or not to print details about this distributions.
     */
    @Option(name = "-v", usage = "Print detail information of version")
    public boolean detail;
  }

}
