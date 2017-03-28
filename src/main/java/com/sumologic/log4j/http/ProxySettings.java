package com.sumologic.log4j.http;


public class ProxySettings
{

  public static final String NTLM_AUTH = "ntlm";
  public static final String BASIC_AUTH = "basic";

  private String hostname = null;
  private Integer port = null;
  private String authType = null;
  private String username = null;
  private String password = null;
  private String domain = null;


  public ProxySettings(String hostname, Integer port, String authType, String username, String password, String domain)
  {
    this.hostname = hostname;
    this.port = port;
    this.authType = authType;
    this.username = username;
    this.password = password;
    this.domain = domain;

    normalize();
  }

  public String getDomain()
  {
    return domain;
  }

  public void setDomain(String domain)
  {
    this.domain = domain;
    normalize();
  }

  public String getHostname()
  {
    return hostname;
  }

  public void setHostname(String hostname)
  {
    this.hostname = hostname;
    normalize();
  }

  public Integer getPort()
  {
    return port;
  }

  public void setPort(Integer port)
  {
    this.port = port;
    normalize();
  }

  public String getAuthType()
  {
    return authType;
  }

  public void setAuthType(String authType)
  {
    this.authType = authType;
    normalize();
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String username)
  {
    this.username = username;
    normalize();
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(String password)
  {
    this.password = password;
    normalize();
  }

  private void normalize()
  {
    // Default to Basic Auth when credentials are specified without an auth type
    if (username != null && authType == null) {
      this.authType = BASIC_AUTH;
    }
  }

  public void validate()
  {
    if (hostname != null) {
      if (port == null) {
        throw new IllegalArgumentException("port property must be set");
      }

      if (authType != null && (username == null || password == null)) {
        throw new IllegalArgumentException("username and password properties must be set if authType property is set");
      }

      if (NTLM_AUTH.equals(authType) && domain == null) {
        throw new IllegalArgumentException("domain property must be set if authType property is ntlm");
      }

      if (authType != null && !(NTLM_AUTH.equals(authType) || BASIC_AUTH.equals(authType))) {
        throw new IllegalArgumentException("authType type not supported: " + authType);
      }
    }
  }
}
