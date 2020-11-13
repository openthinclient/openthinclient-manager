package org.openthinclient.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * SocketFactory for nominal LDAPS support.
 * Rubber-stamps all certificates.
 */
public class NoSSLSocketFactory extends SSLSocketFactory {
  private static Logger logger =
      LoggerFactory.getLogger(NoSSLSocketFactory.class);

  private SSLSocketFactory socketFactory;

  public class TrustEverybodyManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] xcs, String type)
    throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] xcs, String type)
    throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
      return new java.security.cert.X509Certificate[0];
    }
  }

  public NoSSLSocketFactory() {
    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(	null,
                new TrustManager[] { new TrustEverybodyManager() },
                new SecureRandom());
      socketFactory = ctx.getSocketFactory();
    } catch (Exception ex) {
      logger.error("Could not create SSL socket.", ex);
    }
  }

  public static SocketFactory getDefault() {
    return new NoSSLSocketFactory();
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return socketFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return socketFactory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket( Socket socket, String host, int port,
                              boolean autoClose) throws IOException {
    return socketFactory.createSocket(socket, host, port, autoClose);
  }

  @Override
  public Socket createSocket(String host, int port)
  throws IOException, UnknownHostException {
    return socketFactory.createSocket(host, port);
  }

  @Override
  public Socket createSocket( String host, int port,
                              InetAddress localHost, int localPort)
  throws IOException, UnknownHostException {
    return socketFactory.createSocket(host, port, localHost, localPort);
  }

  @Override
  public Socket createSocket(InetAddress address, int port)
  throws IOException {
    return socketFactory.createSocket(address, port);
  }

  @Override
  public Socket createSocket( InetAddress address, int port,
                              InetAddress localAddress, int localPort)
  throws IOException {
    return socketFactory.createSocket(address, port,
                                      localAddress, localPort);
  }

  @Override
  public Socket createSocket() throws IOException {
    return socketFactory.createSocket();
  }
}
