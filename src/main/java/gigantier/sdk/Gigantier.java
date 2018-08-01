package gigantier.sdk;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import gigantier.sdk.auth.Credential;
import gigantier.sdk.endpoints.Config;
import gigantier.sdk.endpoints.Gateway;
import gigantier.sdk.listeners.ErrorListener;
import gigantier.sdk.listeners.ResponseListener;
import gigantier.sdk.utils.Constants;
import gigantier.sdk.utils.Preferences;

public class Gigantier {

  private static final String TAG = Gigantier.class.getName();

  private Preferences preferences;
  private Config config;
  private Gateway gateway;

  public Gigantier(Context context, Config config) {
    this.preferences = new Preferences(context);
    this.config = config;
    this.gateway = new Gateway(context, config);
  }

  /**
   * Obtain user token. Needed for user specific api endpoints.
   *
   * @param identifier user identifier
   * @param password user password
   */
  public void authenticate(final String identifier, final String password,
                           final ResponseListener<Credential> responseListener,
                           final ErrorListener errorListener) {

    HashMap<String, Object> body = new HashMap<>();
    body.put("username", identifier);
    body.put("password", password);
    retrieveToken(Constants.GRANT_TYPE_USER, body, (credential) -> {
      onCredential(credential);
      responseListener.onResponse(credential);
    }, errorListener);
  }

  /**
   * Api call
   *
   * @param uri api endpoint uri
   */
  public void call(final String uri, final ResponseListener<JSONObject> responseListener,
                   final ErrorListener errorListener) {
    call(uri, null, responseListener, errorListener);
  }

  /**
   * Api call with body.
   *
   * @param uri api endpoint uri
   */
  public void call(final String uri, final Map<String, Object> body, final ResponseListener<JSONObject> responseListener,
                   final ErrorListener errorListener) {

    getAppToken(false, (token) -> {
      Map<String, Object> requestBody = new HashMap<>();
      if (body != null) requestBody.putAll(body);
      requestBody.put("access_token", token);
      execPost(uri, requestBody, false, config.retries, responseListener, errorListener);
    }, errorListener);
  }

  /**
   * Authenticated Api call
   *
   * @param uri api endpoint uri
   */
  public void authenticatedCall(final String uri, final ResponseListener<JSONObject> responseListener,
                                final ErrorListener errorListener) {
    authenticatedCall(uri, null, responseListener, errorListener);
  }

  /**
   * Authenticated Api call with body.
   *
   * @param uri api endpoint uri
   */
  public void authenticatedCall(final String uri, final Map<String, Object> body,
                                final ResponseListener<JSONObject> responseListener,
                                final ErrorListener errorListener) {

    getUserToken(false, (token) -> {
      Map<String, Object> requestBody = new HashMap<>();
      if (body != null) requestBody.putAll(body);
      requestBody.put("access_token", token);
      execPost(uri, requestBody, true, config.retries, responseListener, errorListener);
    }, errorListener);
  }

  private void execPost(final String uri, final Map<String, Object> body, final boolean isUserApi, final int retries,
                        final ResponseListener<JSONObject> responseListener,
                        final ErrorListener errorListener) {

    Log.d(TAG, "Exec post to: " + uri + " -- retries: " + retries);

    gateway.execMethod(Request.Method.POST, uri, null, body, responseListener, (code, msg) -> {
      ResponseListener<String> onTokenRenewed = token -> {
        Log.d(TAG, "Token renewed, executing again post to " + uri);

        Map<String, Object> newBody = new HashMap<>(body);
        newBody.put("access_token", token);
        execPost(uri, newBody, isUserApi, retries - 1, responseListener, errorListener);
      };

      if (code == 401 && retries > 0 && isUserApi) getUserToken(true, onTokenRenewed, errorListener);
      else if (code == 401 && retries > 0) getAppToken(true, onTokenRenewed, errorListener);
      else errorListener.onError(code, msg);
    });
  }

  private void getAppToken(final boolean renew, final ResponseListener<String> responseListener,
                           final ErrorListener errorListener) {

    String storedAppToken = preferences.getAppToken();
    if (!renew && storedAppToken != null && !"".equals(storedAppToken) && !preferences.isAppTokenExpired()) {
      responseListener.onResponse(storedAppToken);
    } else {
      retrieveToken(Constants.GRANT_TYPE_APP, new HashMap<>(), (credential) -> {
        preferences.resetAppToken();
        preferences.setAppToken(credential.accessToken);
        preferences.setAppRefreshToken(credential.refreshToken);
        preferences.setAppTokenExpiration(credential.expires);
        responseListener.onResponse(credential.accessToken);
      }, errorListener);
    }
  }

  private void getUserToken(final boolean renew, final ResponseListener<String> responseListener,
                            final ErrorListener errorListener) {

    String storedUserToken = preferences.getUserToken();

    if (!renew && storedUserToken != null && !"".equals(storedUserToken) && !preferences.isUserTokenExpired()) {
      responseListener.onResponse(storedUserToken);
    } else {
      Map<String, Object> body = new HashMap<>();
      body.put("refresh_token", preferences.getUserRefreshToken());
      retrieveToken(Constants.GRANT_TYPE_REFRESH, body, (credential) -> {
        onCredential(credential);
        responseListener.onResponse(credential.accessToken);
      }, errorListener);
    }
  }

  private void retrieveToken(final String grantType, final Map<String, Object> body,
                             final ResponseListener<Credential> responseListener,
                             final ErrorListener errorListener) {

    Log.d(TAG, "Token request with grant type: " + grantType);

    Map<String, Object> requestBody = new HashMap<>(body);
    requestBody.put("grant_type", grantType);
    requestBody.put("client_id", config.clientId);
    requestBody.put("client_secret", config.clientSecret);
    requestBody.put("scope", config.scope);

    gateway.execMethod(Request.Method.POST, config.authUri, null, requestBody, (response) -> {
      Credential credential = new Credential();

      try {
        if (!response.getBoolean("ok")) {
          errorListener.onError(-1, response.toString());
        } else {
          credential.accessToken = response.getString("access_token");
          credential.expires = response.getLong("expires_in");
          credential.refreshToken = response.optString("refresh_token", "");
          responseListener.onResponse(credential);
        }
      } catch (JSONException e) {
        Log.e(TAG, "Cannot parse json response.", e);
        errorListener.onError(-1, "Cannot parse json response. " + e.getMessage());
      }

    }, errorListener);
  }

  private void onCredential(final Credential credential) {
    preferences.resetUserToken();
    preferences.setUserToken(credential.accessToken);
    preferences.setUserRefreshToken(credential.refreshToken);
    preferences.setUserTokenExpiration(credential.expires);
  }

}
