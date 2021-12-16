package org.openthinclient.service.update;

import com.install4j.api.launcher.ApplicationLauncher;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs external processes
 */
public class RuntimeProcessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeProcessExecutor.class);

    public static void executeManagerUpdateCheck(String install4jID, String updatesUrl, NetworkConfiguration.ProxyConfiguration proxyConfiguration, Consumer<Integer> callback) {

        List<String> args = new ArrayList<String>();

        args.add("-q"); // run quite
        args.add("-v"); // verbose
        args.add("-console"); // try to find a console to add logging
        args.add("-Dinstall4j.noProxyAutoDetect=true,sys.confirmedUpdateInstallation=true"); // switch off proxy-auto detection
        args.add("-VupdatesUrl="+updatesUrl);

        if (proxyConfiguration != null && proxyConfiguration.isEnabled()) {
            args.add("-DproxySet=true");
            args.add("-DproxyHost="+proxyConfiguration.getHost());
            args.add("-DproxyPort="+proxyConfiguration.getPort());
            if (proxyConfiguration.getUser() != null && proxyConfiguration.getPassword() != null) {
                args.add("-DproxyAuth=true");
                args.add("-DproxyAuthUser="+proxyConfiguration.getUser());
                args.add("-DproxyAuthPassword="+proxyConfiguration.getPassword());
            }
        }

        try {
            LOGGER.info("Launch Application with id={} and params={}", install4jID, args.toArray(new String[args.size()]));
            ApplicationLauncher.launchApplication(install4jID, args.toArray(new String[args.size()]), false, new ApplicationLauncher.Callback() {
                public void exited(int exitValue) {
                  LOGGER.info("Installer exited with code {}", exitValue);
                  callback.accept(exitValue);
                }

                public void prepareShutdown() {
                  LOGGER.info("Application shutdown");
                  callback.accept(0);
                }
            }
            );
        } catch (Exception e) {
            LOGGER.error("Exception " , e);
            callback.accept(-1);
        }

    }
}
