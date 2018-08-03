# Gigantier Android

> SDK to connect you Android app to Giganter API.

[API reference](https://docs.gigantier.com/?android)

## Installation

Add this in your root build.gradle at the end of repositories:

```java
allprojects {
    repositories {
	    ...
        maven { url 'https://jitpack.io' }
    }
}
```

And this in you app build.gradle:
```java
dependencies {
    implementation 'com.github.gigantier:android-sdk:1.0.2'
}
```

## Usage

To get started, instantiate a new Gigantier client with your credentials.

> **Note:** This requires a [Gigantier](http://gigantier.com) account.

```java
Config config = new Config();
config.clientId = "XXX";
config.clientSecret = "XXX";
config.scope = "XXX";
config.host = "somehost.com";
config.application = "My App Name";

Gigantier gigantier = new Gigantier(context, config);
```

Check out the [API reference](https://docs.gigantier.com/?android) to learn more about authenticating and the available endpoints.

### API Call

Here is an example of api call:

```java
gigantier.call("/Category/list", new ResponseListener<JSONObject>() {
  @Override
  public void onResponse(JSONObject response) {
    // ...
  }
}, new ErrorListener() {
  @Override
  public void onError(int code, String msg) {
    // ...
  }
});
```

### Authentication

Some endpoints need the user to be authenticated, once they are obtained, the ```authenticate()``` method must be called:

```java
gigantier.authenticate("foo@test.com", "1111111", new ResponseListener<Credential>() {
  @Override
  public void onResponse(Credential response) {

  }
}, new ErrorListener() {
  @Override
  public void onError(int code, String msg) {

  }
});
```

### Authenticated API Call

Here is an example of and authenticated api call. Keep in mind that the method ```authenticate()``` must be executed first:

```java
gigantier.authenticatedCall("/User/me", new ResponseListener<JSONObject>() {
  @Override
  public void onResponse(JSONObject response) {
    // ...
  }
}, new ErrorListener() {
  @Override
  public void onError(int code, String msg) {
    // ...
  }
});
```

### Data Post

To perform data post you need to pass a map with that data to ```call()``` method:

```java
Map<String, Object> data = new HashMap<>();
data.put("name", "John");
data.put("surname", "Doe");

gigantier.call("/User/me", data, new ResponseListener<JSONObject>() {
  @Override
  public void onResponse(JSONObject response) {
    // ...
  }
}, new ErrorListener() {
  @Override
  public void onError(int code, String msg) {
    // ...
  }
});
```

## Contributing

Thank you for considering contributing to Gigantier Android SDK.
