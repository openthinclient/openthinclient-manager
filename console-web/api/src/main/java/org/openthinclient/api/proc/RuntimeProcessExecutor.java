package org.openthinclient.api.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Runs external processes
 */
public class RuntimeProcessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeProcessExecutor.class);

    public static void executeManagerUpdateCheck(String relativeCommand) {
        try {
            String workingDir = System.getProperty("user.dir");
            if (workingDir.endsWith("bin")) {
                workingDir = workingDir.substring(0, workingDir.indexOf("bin") - 1);
            }
            String command = Paths.get(workingDir, relativeCommand).toString();
            String[] processCommand = { command };
            LOGGER.info("Trying to run command: " + Arrays.toString(processCommand));
            Runtime.getRuntime().exec(processCommand);
        } catch (IOException e) {
            LOGGER.error("Command file not found.", e);
        } catch (Exception e) {
            LOGGER.error("Faild to run command.", e);
        }
    }
}
