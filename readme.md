![Castle: The easiest way to protect your users](http://fprimecapital.com/assets/2016/11/Castle-Logo-HiRez.png)

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
	- [Configuration](#configuration)
	- [Identify](#identify)
	- [Track Events](#track-events)
	- [Track Screen Views](#track-screen-views)

## Requirements

- Android 4.0

## Installation

Castle is available through Gradle.

### Gradle

Add the following line to your projects ```build.gradle```

```ruby
compile 'io.castle.android:castle:0.0.1
```

### Usage

#### Configuration

Configurating Castle is easy. Add the following snippet to your applications's ```onCreate``` method.

```java
// Create configuration
Castle.Configuration configuration = new Castle.Configuration(this);

// Enable the desired functionality
configuration.appId("566492732398746");
configuration.lifecycleTrackingEnabled(true); // Default: true
configuration.screenTrackingEnabled(true); // Default: true
configuration.debugLoggingEnabled(true); // Default: false
```

For Castle to perform well it is important to include the Castles’ device_id in all calls to our API from your backend. This lets us tie a specific transaction in your backend to it’s origin.

```java
List<String> whitelist = Arrays.asList(new String[] { "https://api.castle.io/" });
configuration.baseURLWhiteList(whitelist);
```

Then setup Castle with the by providing the configuration

```java
// Setup Castle SDK with provided configuration
Castle.setupWithConfiguration(this, configuration);
```

The Castle app id for your application can also be provided as meda-data in your applications Android Manifest

```xml
<meta-data android:name="castle_app_id"
            android:value="@string/castle_app_id"/>
```

Then simply setup Castle with the default configuration like so

```java
// Setup Castle SDK with default configuration
Castle.setupWithDefaultConfiguration(this); // Reads appId from manifest meta tag
```

To enable Castle to include our device_id in your requests to your backend we need to be be able to include it as an HTTP header. The simplest way of doing this is using OkHttp and adding our interceptor to your client

```java
OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(Castle.interceptor())
                    .build();
```

For other Http clients you can add the headers manually by getting a list based on the whitelist by using

```java
Map<String, String> castleHeaders = Castle.headers();

// Add headers to your http request
...
```

#### Identify

The identify call lets you tie a user to their action and record traits about them. We recommend calling it once after the user successfully logged in. The user_id will be persisted locally so subsequent calls to track and screen will automatically be tied to that user.

```java
// Identify user with a unique identifier
Castle.identify("johan@castle.io");

// OR

// Identify user with a unique identifier including user traits
Map<String, String> traits = new HashMap<>();
traits.put("username", "brissmyr");
traits.put("zip", "12331");

Castle.identify("johan@castle.io", traits);
```

#### Track Events

Track lets you record any actions your users perform, along with properties that describe the action. It could look something like this:

```java
// Track an event
Castle.track("loginFormSubmitted");

// OR

// Track an event and include some properties
Map<String, String> properties = new HashMap<>();
properties.put("username", "brissmyr");

Castle.track("loginFormSubmitted", properties);
```

#### Track Screen Views

The screen call lets you record whenever a user sees a screen. It could look something like this:

```java
// Track a screen view
Castle.screen("Menu");

// OR

// Track screen view and include some properties
Map<String, String> properties = new HashMap<>();
properties.put("locale", "en_US");

Castle.screen("Menu", properties);
```