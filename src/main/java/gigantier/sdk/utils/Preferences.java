package gigantier.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Date;

public class Preferences {

  private static final String USER_TOKEN = "usertoken";
  private static final String USER_TOKEN_EXPIRES = "usertokenexpires";
  private static final String USER_REFRESH_TOKEN = "userrefreshtoken";
  private static final String APP_TOKEN = "apptoken";
  private static final String APP_TOKEN_EXPIRES = "apptokenexpires";
  private static final String APP_REFRESH_TOKEN = "apprefreshtoken";

  private SharedPreferences.Editor editor;
  private SharedPreferences sharedPreferences;

  public Preferences(Context context) {
    this.sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
    this.editor = this.sharedPreferences.edit();
  }

  public void resetAppToken() {
    setAppToken("");
    setAppRefreshToken("");
    setAppTokenExpiration(0L);
  }

  public String getAppToken() {
    return getString(APP_TOKEN);
  }

  public void setAppToken(String token) {
    setString(APP_TOKEN, token);
  }

  public void setAppTokenExpiration(long expires) {
    setTokenExpiration(APP_TOKEN_EXPIRES, expires);
  }

  public boolean isAppTokenExpired() {
    return isTokenExpired(APP_TOKEN_EXPIRES);
  }

  public long getAppTokenExpiration() {
    return getLong(APP_TOKEN_EXPIRES);
  }

  public void setAppRefreshToken(String refreshToken) {
    setString(APP_REFRESH_TOKEN, refreshToken);
  }

  public String getAppRefreshToken() {
    return getString(APP_REFRESH_TOKEN);
  }

  public void resetUserToken() {
    setUserToken("");
    setUserRefreshToken("");
    setUserTokenExpiration(0L);
  }

  public String getUserToken() {
    return getString(USER_TOKEN);
  }

  public void setUserRefreshToken(String refreshToken) {
    setString(USER_REFRESH_TOKEN, refreshToken);
  }

  public String getUserRefreshToken() {
    return getString(USER_REFRESH_TOKEN);
  }

  public void setUserToken(String token) {
    setString(USER_TOKEN, token);
  }

  public void setUserTokenExpiration(long expires) {
    setTokenExpiration(USER_TOKEN_EXPIRES, expires);
  }

  public boolean isUserTokenExpired() {
    return isTokenExpired(USER_TOKEN_EXPIRES);
  }

  public long getUserTokenExpiration() {
    return getLong(USER_TOKEN_EXPIRES);
  }

  private String getString(String name) {
    return sharedPreferences.getString(name, "");
  }

  private int getInt(String name) {
    return sharedPreferences.getInt(name, 0);
  }

  private long getLong(String name) {
    return sharedPreferences.getLong(name, 0);
  }

  private boolean getBoolean(String name) {
    return sharedPreferences.getBoolean(name, false);
  }

  private void setString(String name, String value) {
    editor.putString(name, value);
    editor.commit();
  }

  private void setInt(String name, int value) {
    editor.putInt(name, value);
    editor.commit();
  }

  private void setLong(String name, long value) {
    editor.putLong(name, value);
    editor.commit();
  }

  private void setBoolean(String name, boolean value) {
    editor.putBoolean(name, value);
    editor.commit();
  }

  private void setTokenExpiration(String key, long expires) {
    setLong(key, new Date().getTime() + (expires * 1000));
  }

  private boolean isTokenExpired(String key) {
    long dateLong = getLong(key);
    Date expireDate = new Date(dateLong);
    Date currentDate = new Date();

    Log.d("gigantier", "dateLong " + dateLong);
    Log.d("gigantier", "expireDate " + expireDate);
    Log.d("gigantier", "currentDate " + currentDate);
    Log.d("gigantier", "currentDate.after(expireDate) " + (currentDate.after(expireDate)));
    Log.d("gigantier", "!\"\".equals(getAppToken() " + (!"".equals(getAppToken())));

    return currentDate.after(expireDate) && !"".equals(getAppToken());
  }
}
