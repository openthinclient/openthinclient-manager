package org.openthinclient.runtime.control.util;

import com.google.common.base.Strings;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.wizard.model.InstallModel;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

/**
 * Util class for handle InstallableDistributions depending on given Options
 */
public class DistributionsUtil {

    /**
     * Return a InstallableDistribution as source for installation, based on distribution.xml
     * @param distributionSource Command-line options 'distributionSource'
     * @param distribution Command-line options 'distribution'
     * @param proxyHost Command-line options 'proxyHost'
     * @param proxyPort Command-line options 'proxyPort'
     * @return a InstallableDistribution or null
     * @throws Exception
     */
    public static InstallableDistribution getInstallableDistribution(String distributionSource, String distribution, String proxyHost, Integer proxyPort) throws Exception {

        final InstallableDistribution installableDistribution;

        if (Strings.isNullOrEmpty(distributionSource)) {

            // using default
            if (Strings.isNullOrEmpty(distribution)) {
                installableDistribution = InstallModel.DEFAULT_DISTRIBUTION;

                // select a distribution from default
            } else {
                installableDistribution = InstallableDistributions.getDefaultDistributions()
                        .getInstallableDistributions().stream()
                        .filter(dist -> distribution.equals(dist.getName()))
                        .findFirst().orElse(null);
            }

        } else {

            // load a distribution.xml from somewhere (with or without proxy)
            InstallableDistributions installableDistributions = loadInstallableDistributions(distributionSource, proxyHost, proxyPort);

            // using default distribution from somewhere
            if (Strings.isNullOrEmpty(distribution)) {
                installableDistribution = installableDistributions.getPreferred();

            // select a special distribution from somewhere
            } else {
                installableDistribution = installableDistributions.getInstallableDistributions().stream()
                        .filter(dist -> distribution.equals(dist.getName()))
                        .findFirst().orElse(null);
            }

        }

        return installableDistribution;
    }

    /**
     * Returns a list with available installable distributions
     * @param distributionSource Command-line options 'distributionSource'
     * @param proxyHost Command-line options 'proxyHost'
     * @param proxyPort Command-line options 'proxyPort'
     * @return a list with installable distributions
     * @throws Exception
     */
    public static List<InstallableDistribution> getInstallableDistributions(String distributionSource, String proxyHost, Integer proxyPort) throws Exception {

        if (Strings.isNullOrEmpty(distributionSource)) {
           // return default distributions
           return InstallableDistributions.getDefaultDistributions().getInstallableDistributions();
        } else {
            // return available distributions
            return loadInstallableDistributions(distributionSource, proxyHost, proxyPort).getInstallableDistributions();
        }
    }

    /**
     * Load a distribution.xml from somewhere (with or without proxy)
     * @param distributionSource Command-line options 'distributionSource'
     * @param proxyHost Command-line options 'proxyHost'
     * @param proxyPort Command-line options 'proxyPort'
     * @return InstallableDistributions
     * @throws Exception
     */
    private static InstallableDistributions loadInstallableDistributions(String distributionSource, String proxyHost, Integer proxyPort) throws Exception {

        InstallableDistributions installableDistributions;
        if (proxyHost != null && proxyPort != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            installableDistributions = InstallableDistributions.load(new URL(distributionSource), proxy);
        } else {
            installableDistributions = InstallableDistributions.load(new URL(distributionSource));
        }
        return installableDistributions;
    }
}
