package org.openthinclient.runtime.control;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;
import org.openthinclient.runtime.control.cmd.AbstractCommand;
import org.openthinclient.runtime.control.cmd.ListDistributionsCommand;
import org.openthinclient.runtime.control.cmd.PrepareHomeCommand;
import org.openthinclient.runtime.control.cmd.RemoveServerIdCommand;

import java.io.PrintStream;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManagerControlApplication {

  public static final AbstractCommand<?>[] COMMANDS = new AbstractCommand<?>[]{ //
          new PrepareHomeCommand(), //
          new ListDistributionsCommand(), //
          new RemoveServerIdCommand() //
  };

  @SuppressWarnings(
          // suppressing unchecked, as it will not be possible to work with the AbstractCommand in a generic way
          "unchecked")
  public static void main(String[] args) {

    if (args.length == 0) {
      printHelp();
      System.exit(1);
    }

    final Optional<AbstractCommand<?>> commandOptional = determineCommand(args[0]);

    final AbstractCommand command;
    if (!commandOptional.isPresent()) {
      printErrorHelp(args);
      System.exit(1);
      return;
    } else {
      command = commandOptional.get();
    }

    // removing the first entry as this will be the name of the command to be used.
    String[] commandArgs = new String[args.length - 1];
    if (commandArgs.length > 0)
      System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

    try {

      final Object optionsObject = command.createOptionsObject();
      final CmdLineParser parser = new CmdLineParser(optionsObject);

      parser.parseArgument(commandArgs);

      command.execute(optionsObject);

      // application ended successfully
      System.exit(0);
    } catch (CmdLineException e) {

      System.err.println(e.getLocalizedMessage());
      System.err.println("");
      printErrorHelp();
      System.exit(1);

    } catch (Exception e) {
      System.err.println("#####");
      System.err.println("## Execution Failed");
      System.err.println("## " + e.getMessage() + " (" + e.getClass().getName() + ")");
      System.err.println("#####");
    }
    System.exit(1);

  }

  private static Optional<AbstractCommand<?>> determineCommand(String arg) {
    return Stream.of(COMMANDS).filter(cmd -> cmd.getName().equals(arg)).findFirst();
  }

  private static void printErrorHelp() {
    printHelp(System.err);
  }

  private static void printErrorHelp(String[] args) {
    System.err.println("Unsupported command line: " + Stream.of(args).collect(Collectors.joining(" ")));
    printErrorHelp();
  }

  private static void printHelp() {
    printHelp(System.out);
  }

  private static void printHelp(PrintStream ps) {
    ps.println("Usage:");

    final Integer maxLength = Stream.of(COMMANDS).map(cmd -> cmd.getName().length()).max(Integer::compareTo).get();
    final ParserProperties properties = ParserProperties.defaults().withUsageWidth(80 - 2 - maxLength);

    for (AbstractCommand<?> cmd : COMMANDS) {

      ps.println();
      ps.println(cmd.getName() + ":");
      final CmdLineParser parser = new CmdLineParser(cmd.createOptionsObject(), properties);

      parser.printUsage(ps);

    }

    ps.flush();

  }
}
