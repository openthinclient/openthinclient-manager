package org.openthinclient.runtime.control.cmd;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.kohsuke.args4j.Option;
import org.openthinclient.wizard.install.InstallableDistribution;
import org.openthinclient.wizard.install.InstallableDistributions;

import java.util.List;

public class ListDistributionsCommand extends AbstractCommand<ListDistributionsCommand.Options> {

  public ListDistributionsCommand() {
    super("ls-distributions");
  }

  @Override
  public Options createOptionsObject() {
    return new Options();
  }

  @Override
  public void execute(Options options) throws Exception {
    final List<InstallableDistribution> dists = InstallableDistributions.getDefaultDistributions()
            .getInstallableDistributions();

    if (options.detail)
      dists.forEach(this::printDistributionDetails);
    else
      dists.forEach(this::printDistribution);
  }

  protected void printDistribution(InstallableDistribution dist) {
    System.out.println(dist.getName() + " [Sources: " + dist.getSourcesList().getSources().size() + "]");
  }

  protected void printDistributionDetails(InstallableDistribution dist) {
    System.out.println("Distribution: " + dist.getName());
    System.out.println("   " + WordUtils.wrap(dist.getDescription(), 60, SystemUtils.LINE_SEPARATOR + "   ", true));
    System.out.println("   Sources:");
    dist.getSourcesList().getSources().forEach(s -> {
              System.out.println("      - " + s.getDescription() + (s.isEnabled() ? "" : " [DISABLED]"));
              System.out.println("        " + s.getUrl());
            }
    );

    System.out.println();
  }

  public class Options {
    /**
     * Whether or not to print details about the distributions.
     */
    @Option(name = "-v", usage = "Print detail information for each distribution")
    public boolean detail;
  }


}
