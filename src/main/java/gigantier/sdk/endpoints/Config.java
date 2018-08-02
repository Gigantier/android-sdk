package gigantier.sdk.endpoints;

import gigantier.sdk.utils.Constants;

public class Config {
  public String clientId;
  public String clientSecret;
  public String scope;
  public String host = Constants.HOST;
  public String protocol = Constants.PROTOCOL;
  public String version = Constants.VERSION;
  public int retries = Constants.RETRIES;
  public String authUri = Constants.AUTH_URI;
  public String contentType = Constants.CONTENT_TYPE;
  public String application;

  public String buildUrl(String uri) {
    return this.protocol + "://" + this.host + buildPath(uri);
  }

  public String buildPath(String uri) {
    String versionPath = (this.version != null) ? "/" + this.version : "";
    return "/api" + versionPath + uri;
  }
}
