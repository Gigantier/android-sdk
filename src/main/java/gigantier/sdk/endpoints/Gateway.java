package gigantier.sdk.endpoints;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import gigantier.sdk.listeners.ErrorListener;
import gigantier.sdk.listeners.ResponseListener;
import gigantier.sdk.utils.Constants;


public class Gateway {

  private static final String TAG = Gateway.class.getName();

  private Context context;
  private RequestQueue requestQueue;

  private Config config;

  public Gateway(Context context, Config config) {
    this.context = context;
    this.config = config;
  }

  public void execMethod(final int method, final String uri, final Map<String, String> headers, final Map<String, Object> body,
                         final ResponseListener<JSONObject> responseListener,
                         final ErrorListener errorListener) {

    try {
      JsonObjectRequest request = new JsonObjectRequest(method, config.buildUrl(uri), buildBody(body),
          responseListener::onResponse,
          onError(errorListener)) {

        public Map<String, String> getHeaders() {
          return headers != null ? headers : new HashMap<>();
        }

        @Override
        public String getBodyContentType() {
          return Constants.CONTENT_TYPE;
        }

      };

      request.setRetryPolicy(new DefaultRetryPolicy(0, 0,0));
      getRequestQueue().add(request);
    } catch (JSONException e) {
      Log.e(TAG, "Cannot build request json body.", e);
      errorListener.onError(-1, e.getMessage());
    }

  }

  private JSONObject buildBody(Map<String, Object> body) throws JSONException {
    JSONObject requestBody = new JSONObject();
    for (String key: body.keySet()) {
      requestBody.put(key, body.get(key));
    }

    return requestBody;
  }

  private RequestQueue getRequestQueue() {
    if (requestQueue == null) {
      requestQueue = Volley.newRequestQueue(this.context);
    }
    return requestQueue;
  }

  private Response.ErrorListener onError(final ErrorListener listener) {
    return error -> {
      Log.e(TAG, error.getMessage(), error);

      String msg = "no-network-response";
      int statusCode = -1;

      if (error.networkResponse != null) {
        msg = new String(error.networkResponse.data, Charset.forName("UTF-8"));
        statusCode = error.networkResponse.statusCode;
      }

      listener.onError(statusCode, msg);
    };
  }

}
