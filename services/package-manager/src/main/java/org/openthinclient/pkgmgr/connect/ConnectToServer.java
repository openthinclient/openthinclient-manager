package org.openthinclient.pkgmgr.connect;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;

import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Every connection which is made from the PackageManager to the Internet to
 * download some things is made through this class.
 * 
 * @author tauschfn
 * 
 */
public class ConnectToServer {
	private static final Logger logger = LoggerFactory.getLogger(ConnectToServer.class);
	private PackageManagerTaskSummary taskSummaryManager;

	public ConnectToServer(PackageManagerTaskSummary taskSummary) {
		this.taskSummaryManager = taskSummary;
	}

	public InputStream getInputStream(String adress) throws IOException, PackageManagerException {
		if (isProxyInUse())
			return getInputProxyStream(adress);
		else
			return new URL(adress).openStream();

	}

	public InputStream getInputStream(URL url) throws  PackageManagerException, IOException {
		if (isProxyInUse())
			return getInputProxyStream(url.toString());
		else
			return url.openStream();

	}

	/**
	 * Checks the SystemProperty
	 * 
	 * @return
	 */
	private static boolean isProxyInUse() {
		if (System.getProperty("proxySet") != null
				&& System.getProperty("proxySet") == "true") {
			return true;
		}
		else {
			return false;
		}
	}
	private InputStream getInputProxyStream(String URLName) throws PackageManagerException{
	  try {
	    Properties systemSettings = System.getProperties();
	    systemSettings.put("http.proxyHost",ProxyProperties.getProxyHost()) ;
	    systemSettings.put("http.proxyPort", ProxyProperties.getProxyPort()) ;
	    System.setProperties(systemSettings);
	 
	    Authenticator.setDefault(new Authenticator() {
	      protected PasswordAuthentication getPasswordAuthentication() {
	        return new
	           PasswordAuthentication(ProxyProperties.getProxyAccount(),ProxyProperties.getProxyPassword().toCharArray());
	    }});
	 
	    URL u = new URL(URLName);
	    HttpURLConnection con = (HttpURLConnection) u.openConnection();
	    con.connect();
	    return con.getInputStream();
	  } catch (Exception e) {
      final String message = I18N.getMessage("ProxyManager.getInputStreamByProxy.IOException.connect");
      if (null != taskSummaryManager) {
        taskSummaryManager.addWarning(message);
      }

      logger.error(message, e);
      throw new PackageManagerException(message,e);

    }

	}
}
