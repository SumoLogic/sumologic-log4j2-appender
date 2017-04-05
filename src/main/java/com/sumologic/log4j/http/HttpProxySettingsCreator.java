package com.sumologic.log4j.http;


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class HttpProxySettingsCreator
{
  private static final Logger logger = StatusLogger.getLogger();
  private ProxySettings proxySettings;

  public HttpProxySettingsCreator(ProxySettings proxySettings)
  {
    this.proxySettings = proxySettings;
  }

  private String hostname()
  {
    String host = "localhost";
    try {
      host = java.net.InetAddress.getLocalHost().getHostName();
    }
    catch (Exception e) {
      logger.error("Unable to obtain local hostname. Defaulting to localhost", e);
    }
    return host;
  }

  private CredentialsProvider createCredentialsProvider()
  {
    String username = proxySettings.getUsername();
    String password = proxySettings.getPassword();
    String domain = proxySettings.getDomain();

    if (ProxySettings.BASIC_AUTH.equals(proxySettings.getAuthType())) {
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
          new AuthScope(proxySettings.getHostname(), proxySettings.getPort()),
          new UsernamePasswordCredentials(username, password)
      );
      return credsProvider;
    } else if (ProxySettings.NTLM_AUTH.equals(proxySettings.getAuthType())) {
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
          new AuthScope(proxySettings.getHostname(), proxySettings.getPort()),
          new NTCredentials(username, password, hostname(), domain)
      );
      return credsProvider;
    } else {
      throw new IllegalStateException(
          "proxyAuth " + proxySettings.getAuthType() + " not supported!");
    }
  }

  public void configureProxySettings(HttpClientBuilder builder)
  {
    proxySettings.validate();
    String proxyHost = proxySettings.getHostname();
    int proxyPort = proxySettings.getPort();
    String proxyAuth = proxySettings.getAuthType();

    if (proxyHost != null) {
      HttpHost host = new HttpHost(proxyHost, proxyPort);
      builder.setProxy(host);

      if (proxyAuth != null) {
        CredentialsProvider credsProvider = createCredentialsProvider();
        builder.setDefaultCredentialsProvider(credsProvider);
      }
    }
  }
}
