package org.openthinclient.web.thinclient.util;

import org.openthinclient.web.thinclient.ClientView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientIPAddressFinder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientIPAddressFinder.class);

	/*
	 * This pattern is used to find a matching IP-Address in the Client-log.
	 */
	private static final Pattern IPADDRESS_PATTERN = Pattern
			.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
	/*
	 * This is the pathname on the server, where the client-log-file is located.
	 */
	private static String fileName = "/logs/syslog.log";

	/*
	 * Originally taken from org.openthinclient.console.OpenVNCConnectionAction
	 *
	 * Load and find mac-address in syslog.log file.
	 * After that, we use our pattern IPADDRESS_PATTERN to find the IP address.
	 * To be sure that this the newest IP address for the client, we take the last one out of the list.
	 * If there is no IP address, we return Optional.empty()
	 */
	public static Optional<String> findIPAddress(String macAddress, File managerHome) {

    try {
      final BufferedReader br = Files.newBufferedReader(Paths.get(managerHome.toPath().toAbsolutePath().toString(), fileName));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains(macAddress)) {
          Matcher matcher = IPADDRESS_PATTERN.matcher(line);
          while (matcher.find()) {
            String group = matcher.group();
            LOGGER.debug("Found IP {} for mac {}", group, macAddress);
            return Optional.of(group);
          }
        }
      }
    } catch (final IOException e) {
      LOGGER.error("Cannot read " + fileName + " in " + managerHome.toPath(), e);
    }

    LOGGER.debug("No IP found for mac {}", macAddress);
    return Optional.empty();
	}

}
