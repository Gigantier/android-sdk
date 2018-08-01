package gigantier.sdk.listeners;

public interface ResponseListener<T> {

  void onResponse(T response);

}
