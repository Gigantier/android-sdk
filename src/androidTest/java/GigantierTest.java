import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;

import gigantier.sdk.BuildConfig;
import gigantier.sdk.Gigantier;
import gigantier.sdk.endpoints.Config;
import gigantier.sdk.utils.Constants;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GigantierTest {

  public static final String API_VERSION = "vtest";
  public static final String SCOPE = "Category User";
  public static final String ACCESS_TOKEN = "SOME_ACCESS_TOKEN";
  public static final String ANOTHER_ACCESS_TOKEN = "ANOTHER_ACCESS_TOKEN";
  public static final String REFRESH_TOKEN = "SOME_REFRESH_TOKEN";
  public static final String ERROR_INVALID_GRANT = "invalid_grant";
  public static final long EXPIRES_IN = 3600;
  public static final int UNAUTHORIZED_STATUS_CODE = 401;
  public static final String CATEGORY_URI = "/Category/list";
  public static final String USER_URI = "/User/me";
  public static final String USER_EMAIL = "test@foo.com";
  public static final String USER_PWD = "pasword";
  public static final String USER_NAME = "Test";
  public static final String USER_SURNAME = "Foo";
  public static final String TEST_APP = "TEST_APP";

  private Gigantier gigantier;
  private MockWebServer server;
  private Config config;

  @Before
  public void initTest() throws Exception {
    clearSharedPrefs(InstrumentationRegistry.getContext());

    if (server != null) server.shutdown();

    server = new MockWebServer();
    server.start();

    config = new Config();
    config.clientId = "SOME_CLIENT_ID";
    config.clientSecret = "SOME_CLIENT_SECRET";
    config.application = TEST_APP;
    config.scope = SCOPE;
    config.version = API_VERSION;
    config.host = server.getHostName() + ":" + server.getPort();
    config.protocol = "http";

    gigantier = new Gigantier(InstrumentationRegistry.getContext(), config);
  }

  @Test
  public void authenticate_ok() throws Exception {
    testTemplate(callback -> {
      JSONObject responseJson = ApiResponseBuilder.buildTokenResponseJson();
      server.enqueue(new MockResponse().setBody(responseJson.toString()));

      gigantier.authenticate(USER_EMAIL, USER_PWD, response -> responseListenerTemplate(callback, () -> {

        RecordedRequest request = getRecordedRequest();

        basicRequestValidation(request, Constants.AUTH_URI);

        JSONObject tokenRequestBody = new JSONObject(request.getBody().readUtf8());
        assertEquals(Constants.GRANT_TYPE_USER, tokenRequestBody.getString("grant_type"));

        assertThat(response.accessToken, is(ACCESS_TOKEN));
        assertThat(response.expires, is(EXPIRES_IN + 20));
        assertThat(response.refreshToken, is(REFRESH_TOKEN));

      }), (statusCode, msg) -> errorListenerTemplate(callback, () -> fail(statusCode + " - " + msg)));
    });
  }

  @Test
  public void authenticate_unauthorized() throws Exception {
    testTemplate(callback -> {
      JSONObject responseJson = ApiResponseBuilder.buildUnauthorizedTokenResponseJson();
      server.enqueue(new MockResponse().setBody(responseJson.toString()).setResponseCode(UNAUTHORIZED_STATUS_CODE));

      gigantier.authenticate(USER_EMAIL, USER_PWD, response -> {
        responseListenerTemplate(callback, () -> fail("Response ok, but must be unauthorized error"));
      }, (statusCode, msg) -> errorListenerTemplate(callback, () -> {
        RecordedRequest request = getRecordedRequest();
        basicRequestValidation(request, Constants.AUTH_URI);
        assertThat(statusCode, is(UNAUTHORIZED_STATUS_CODE));

        JSONObject tokenRequestBody = new JSONObject(request.getBody().readUtf8());
        assertEquals(Constants.GRANT_TYPE_USER, tokenRequestBody.getString("grant_type"));

      }));
    });
  }

  @Test
  public void call_ok() throws Exception {
    testTemplate(callback -> {
      JSONObject tokenResponseJson = ApiResponseBuilder.buildTokenResponseJson();
      JSONObject categoriesResponseJson = ApiResponseBuilder.buildCategoryResponseJson();
      server.enqueue(new MockResponse().setBody(tokenResponseJson.toString()));
      server.enqueue(new MockResponse().setBody(categoriesResponseJson.toString()));

      gigantier.call(CATEGORY_URI, response -> responseListenerTemplate(callback, () -> {

        // access token request
        RecordedRequest tokenRequest = getRecordedRequest();
        basicRequestValidation(tokenRequest, Constants.AUTH_URI);

        JSONObject tokenRequestBody = new JSONObject(tokenRequest.getBody().readUtf8());
        assertEquals(Constants.GRANT_TYPE_APP, tokenRequestBody.getString("grant_type"));

        // category request
        RecordedRequest categoryRequest = getRecordedRequest();
        basicRequestValidation(categoryRequest, CATEGORY_URI);

        JSONObject categoriesRequestBody = new JSONObject(categoryRequest.getBody().readUtf8());
        assertEquals(ACCESS_TOKEN, categoriesRequestBody.getString("access_token"));

        JSONObject categoryJson = null;
        categoryJson = response.getJSONArray("categories").getJSONObject(0);
        assertThat(response.get("ok"), is(true));
        assertNotEquals(categoryJson.get("id"), "");

      }), (statusCode, msg) -> errorListenerTemplate(callback, () -> fail(statusCode + " - " + msg)));
    });
  }

  @Test
  public void call_renew_token() throws Exception {
    testTemplate(callback -> {
      JSONObject tokenResponseJson = ApiResponseBuilder.buildTokenResponseJson();
      JSONObject token401ResponseJson = ApiResponseBuilder.buildUnauthorizedTokenResponseJson();
      JSONObject renewTokenResponseJson = ApiResponseBuilder.buildAnotherTokenResponseJson();
      JSONObject categoriesResponseJson = ApiResponseBuilder.buildCategoryResponseJson();
      server.enqueue(new MockResponse().setBody(tokenResponseJson.toString()));
      server.enqueue(new MockResponse().setBody(token401ResponseJson.toString()).setResponseCode(UNAUTHORIZED_STATUS_CODE));
      server.enqueue(new MockResponse().setBody(renewTokenResponseJson.toString()));
      server.enqueue(new MockResponse().setBody(categoriesResponseJson.toString()));

      gigantier.call(CATEGORY_URI, response -> responseListenerTemplate(callback, () -> {

        // first access token request
        RecordedRequest tokenRequest = getRecordedRequest();
        basicRequestValidation(tokenRequest, Constants.AUTH_URI);

        JSONObject tokenRequestBody = new JSONObject(tokenRequest.getBody().readUtf8());
        assertEquals(Constants.GRANT_TYPE_APP, tokenRequestBody.getString("grant_type"));

        // first category request (401 response code)
        RecordedRequest firstCategoryRequest = getRecordedRequest();
        basicRequestValidation(firstCategoryRequest, CATEGORY_URI);

        JSONObject categoriesRequestBody = new JSONObject(firstCategoryRequest.getBody().readUtf8());
        assertEquals(ACCESS_TOKEN, categoriesRequestBody.getString("access_token"));

        // second access token request
        RecordedRequest secondTokenRequest = getRecordedRequest();
        basicRequestValidation(secondTokenRequest, Constants.AUTH_URI);

        JSONObject secondTokenRequestBody = new JSONObject(secondTokenRequest.getBody().readUtf8());
        assertEquals(Constants.GRANT_TYPE_APP, secondTokenRequestBody.getString("grant_type"));

        // second category request
        RecordedRequest secondCategoryRequest = getRecordedRequest();
        basicRequestValidation(secondCategoryRequest, CATEGORY_URI);

        JSONObject secondCategoriesRequestBody = new JSONObject(secondCategoryRequest.getBody().readUtf8());
        assertEquals(ANOTHER_ACCESS_TOKEN, secondCategoriesRequestBody.getString("access_token"));

        JSONObject categoryJson = null;
        categoryJson = response.getJSONArray("categories").getJSONObject(0);
        assertThat(response.get("ok"), is(true));
        assertNotEquals(categoryJson.get("id"), "");

      }), (statusCode, msg) -> errorListenerTemplate(callback, () -> fail(statusCode + " - " + msg)));
    });
  }

  @Test
  public void authenticated_call_ok() throws Exception {
    testTemplate(callback -> {
      JSONObject tokenResponseJson = ApiResponseBuilder.buildTokenResponseJson();
      JSONObject userResponseJson = ApiResponseBuilder.buildUserResponseJson();
      server.enqueue(new MockResponse().setBody(tokenResponseJson.toString()));
      server.enqueue(new MockResponse().setBody(userResponseJson.toString()));

      gigantier.authenticate(USER_EMAIL, USER_PWD, authenticateResponse -> {
        gigantier.authenticatedCall(USER_URI, response -> responseListenerTemplate(callback, () -> {

          // access token request
          RecordedRequest tokenRequest = getRecordedRequest();
          basicRequestValidation(tokenRequest, Constants.AUTH_URI);

          JSONObject tokenRequestBody = new JSONObject(tokenRequest.getBody().readUtf8());
          assertEquals(Constants.GRANT_TYPE_USER, tokenRequestBody.getString("grant_type"));

          // user request
          RecordedRequest userRequest = getRecordedRequest();
          basicRequestValidation(userRequest, USER_URI);

          JSONObject userRequestBody = new JSONObject(userRequest.getBody().readUtf8());
          assertEquals(ACCESS_TOKEN, userRequestBody.getString("access_token"));

          assertThat(response.get("ok"), is(true));
          assertThat(response.get("name"), is(USER_NAME));
          assertThat(response.get("surname"), is(USER_SURNAME));
          assertThat(response.get("email"), is(USER_EMAIL));

        }), (statusCode, msg) -> errorListenerTemplate(callback, () -> fail(statusCode + " - " + msg)));
      }, (statusCode, msg) -> errorListenerTemplate(callback, () -> fail(statusCode + " - " + msg)));
    });
  }

  @Test
  public void authenticated_call_renew_token() throws Exception {
    testTemplate(callback -> {
      JSONObject tokenResponseJson = ApiResponseBuilder.buildTokenResponseJson();
      JSONObject token401ResponseJson = ApiResponseBuilder.buildUnauthorizedTokenResponseJson();
      JSONObject renewTokenResponseJson = ApiResponseBuilder.buildAnotherTokenResponseJson();
      JSONObject userResponseJson = ApiResponseBuilder.buildUserResponseJson();
      server.enqueue(new MockResponse().setBody(tokenResponseJson.toString()));
      server.enqueue(new MockResponse().setBody(token401ResponseJson.toString()).setResponseCode(UNAUTHORIZED_STATUS_CODE));
      server.enqueue(new MockResponse().setBody(renewTokenResponseJson.toString()));
      server.enqueue(new MockResponse().setBody(userResponseJson.toString()));

      gigantier.authenticate(USER_EMAIL, USER_PWD, authenticateResponse -> {
        gigantier.authenticatedCall(USER_URI, response -> responseListenerTemplate(callback, () -> {

          // first access token request
          RecordedRequest tokenRequest = getRecordedRequest();
          basicRequestValidation(tokenRequest, Constants.AUTH_URI);

          JSONObject tokenRequestBody = new JSONObject(tokenRequest.getBody().readUtf8());
          assertEquals(Constants.GRANT_TYPE_USER, tokenRequestBody.getString("grant_type"));

          // first user request (401 response code)
          RecordedRequest firstUserRequest = getRecordedRequest();
          basicRequestValidation(firstUserRequest, USER_URI);

          JSONObject categoriesRequestBody = new JSONObject(firstUserRequest.getBody().readUtf8());
          assertEquals(ACCESS_TOKEN, categoriesRequestBody.getString("access_token"));

          // second access token request
          RecordedRequest secondTokenRequest = getRecordedRequest();
          basicRequestValidation(secondTokenRequest, Constants.AUTH_URI);

          JSONObject secondTokenRequestBody = new JSONObject(secondTokenRequest.getBody().readUtf8());
          assertEquals(Constants.GRANT_TYPE_REFRESH, secondTokenRequestBody.getString("grant_type"));

          // second category request
          RecordedRequest secondCategoryRequest = getRecordedRequest();
          basicRequestValidation(secondCategoryRequest, USER_URI);

          JSONObject secondUserRequestBody = new JSONObject(secondCategoryRequest.getBody().readUtf8());
          assertEquals(ANOTHER_ACCESS_TOKEN, secondUserRequestBody.getString("access_token"));

          assertThat(response.get("ok"), is(true));
          assertThat(response.get("name"), is(USER_NAME));
          assertThat(response.get("surname"), is(USER_SURNAME));
          assertThat(response.get("email"), is(USER_EMAIL));

        }), (statusCode, msg) -> errorListenerTemplate(callback, () -> fail(statusCode + " - " + msg)));
      }, (statusCode, msg) -> errorListenerTemplate(callback, () -> fail(statusCode + " - " + msg)));
    });
  }

  @NonNull
  private RecordedRequest getRecordedRequest() throws Exception {
    return server.takeRequest(1, TimeUnit.SECONDS);
  }

  private void clearSharedPrefs(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear();
    editor.commit();
  }

  private void basicRequestValidation(RecordedRequest request, String uri) {
    assertEquals("POST " + config.buildPath(uri) + " HTTP/1.1", request.getRequestLine());
    assertEquals(Constants.CONTENT_TYPE, request.getHeader("Content-Type"));
    assertEquals(Constants.SDK_LANG, request.getHeader(Constants.SDK_LANG_HEADER));
    assertEquals(BuildConfig.VERSION_NAME, request.getHeader(Constants.SDK_VERSION_HEADER));
    assertEquals(TEST_APP, request.getHeader(Constants.SDK_APP_HEADER));
  }

  private void testTemplate(TestExec exec) throws Exception {
    final CountDownLatch signal = new CountDownLatch(1);
    try {
      exec.run(signal::countDown);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    signal.await();
  }

  private void responseListenerTemplate(TestFinishedCallback callback, TestResponseExec exec) {
    try {
      exec.run();
      callback.finished();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  private void errorListenerTemplate(TestFinishedCallback callback, TestResponseExec exec) {
    try {
      exec.run();
      callback.finished();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  interface TestFinishedCallback {
    void finished() throws Exception;
  }

  interface TestExec {
    void run(TestFinishedCallback callback) throws Exception;
  }

  interface TestResponseExec {
    void run() throws Exception;
  }

}
