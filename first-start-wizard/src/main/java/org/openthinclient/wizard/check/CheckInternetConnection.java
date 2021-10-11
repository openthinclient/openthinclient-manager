package org.openthinclient.wizard.check;

import static org.openthinclient.wizard.AdvisorMessages.*;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.HttpConnectionTester;

import ch.qos.cal10n.MessageConveyor;

/**
 * Die Klasse cInetConnection prüft, ob eine funktionierende Internetverbindung vorhanden ist.
 *
 * @author Benedikt Diehl
 */
public class CheckInternetConnection extends AbstractCheck<Boolean> {

  public CheckInternetConnection(Locale locale) {
    super(new MessageConveyor(locale).getMessage(ADVISOR_CHECKINTERNETCONNECTION_TITLE), "");
  }

  private NetworkConfiguration.ProxyConfiguration proxyConfiguration;

  public NetworkConfiguration.ProxyConfiguration getProxyConfiguration() {
    return proxyConfiguration;
  }

  public void setProxyConfiguration(NetworkConfiguration.ProxyConfiguration proxyConfiguration) {
    this.proxyConfiguration = proxyConfiguration;
  }

  /**
   * Verify the connectivity by contacting some well known sites.
   */
  @Override
  protected CheckExecutionResult<Boolean> perform() {
    final HttpConnectionTester connectionTester = new HttpConnectionTester(proxyConfiguration);

    final List<HttpConnectionTester.Result> results = Stream.of(
            "http://archive.openthinclient.org"
    )
            .map(URI::create)
            .map(HttpConnectionTester.Request::new)
            .map(connectionTester::verifyConnectivity)
            .collect(Collectors.toList());

    if (results.stream().allMatch(HttpConnectionTester.Result::isSuccess)) {
      return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.SUCCESS);
    }
    return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED);
  }

}
