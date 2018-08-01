import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static org.junit.Assert.fail;

public class ApiResponseBuilder {

  @NonNull
  public static  JSONObject buildTokenResponseJson() throws JSONException {
    JSONObject responseJson = new JSONObject();
    responseJson.put("ok", true);
    responseJson.put("access_token", GigantierTest.ACCESS_TOKEN);
    responseJson.put("expires_in", GigantierTest.EXPIRES_IN);
    responseJson.put("scope", GigantierTest.SCOPE);
    responseJson.put("refresh_token", GigantierTest.REFRESH_TOKEN);
    return responseJson;
  }

  @NonNull
  public static  JSONObject buildAnotherTokenResponseJson() throws JSONException {
    JSONObject responseJson = new JSONObject();
    responseJson.put("ok", true);
    responseJson.put("access_token", GigantierTest.ANOTHER_ACCESS_TOKEN);
    responseJson.put("expires_in", GigantierTest.EXPIRES_IN);
    responseJson.put("scope", GigantierTest.SCOPE);
    responseJson.put("refresh_token", GigantierTest.REFRESH_TOKEN);
    return responseJson;
  }

  @NonNull
  public static  JSONObject buildUnauthorizedTokenResponseJson() throws JSONException {
    JSONObject responseJson = new JSONObject();
    responseJson.put("ok", false);
    responseJson.put("error", GigantierTest.ERROR_INVALID_GRANT);
    responseJson.put("error_description", "Invalid username and password combination");
    return responseJson;
  }

  @NonNull
  public static JSONObject buildCategoryResponseJson() throws JSONException {
    JSONObject responseJson = new JSONObject();
    JSONObject categoryJson = new JSONObject();

    categoryJson.put("id", "1");
    categoryJson.put("name", "First Category");
    categoryJson.put("code", "CAT_1");
    categoryJson.put("description", "This is the first category");
    categoryJson.put("active", true);
    categoryJson.put("visible", "1");

    ArrayList<JSONObject> categories = new ArrayList<>();
    categories.add(categoryJson);

    responseJson.put("ok", true);
    responseJson.put("categories", new JSONArray(categories));
    return responseJson;
  }

  @NonNull
  public static JSONObject buildUserResponseJson() throws JSONException {
    JSONObject responseJson = new JSONObject();

    responseJson.put("id", "1");
    responseJson.put("name", GigantierTest.USER_NAME);
    responseJson.put("surname", GigantierTest.USER_SURNAME);
    responseJson.put("email", GigantierTest.USER_EMAIL);

    responseJson.put("ok", true);
    return responseJson;
  }

}
