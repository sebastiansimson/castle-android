package io.castle.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.castle.android.api.model.Event;
import io.castle.android.api.model.IdentifyEvent;
import io.castle.android.api.model.ScreenEvent;
import io.castle.android.queue.EventQueue;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Copyright (c) 2017 Castle
 */

public class Castle {
    private static Castle instance;
    private String identifier;
    private Configuration configuration;
    private EventQueue eventQueue;
    private StorageHelper storageHelper;

    private Castle(Application application, Configuration configuaration) {
        setup(application, configuaration);
    }

    private void setup(Application application, Configuration configuration) {
        Context context = application.getApplicationContext();
        this.storageHelper = new StorageHelper(context);
        this.configuration = configuration;
        this.identifier = storageHelper.getDeviceId();
        this.eventQueue = new EventQueue(context);
    }

    private void test() {

        // OkHttp
        String url = "https://exampleapi.com/v1/awesomeendpoint";
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        for (Map.Entry<String, String> entry : Castle.headers(url).entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }
        Request request = requestBuilder.build();

        Response response = client.newCall(request).execute();

        // HttpURLConnection
        URL url = new URL("https://exampleapi.com/v1/awesomeendpoint");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        for (Map.Entry<String, String> entry : Castle.headers(url.toString()).entrySet()) {
            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        } finally {
            urlConnection.disconnect();
        }
    }

    private void registerLifeCycleCallbacks(Application application) {
        application.registerActivityLifecycleCallbacks(new CastleActivityLifecycleCallbacks());

        // Get the current version.
        PackageInfo packageInfo = Utils.getPackageInfo(application);
        String currentVersion = packageInfo.versionName;
        int currentBuild = packageInfo.versionCode;

        // Get the previous recorded version.
        String previousVersion = storageHelper.getVersion();
        int previousBuild = storageHelper.getBuild();

        if (Castle.configuration().lifecycleTrackingEnabled()) {
            // Check and track Application Installed or Application Updated.
            if (previousBuild == -1) {
                Map<String, String> properties = new HashMap<>();
                properties.put("version", currentVersion);
                properties.put("build", "" + currentBuild);
                track("Application Installed", properties);
            } else if (currentBuild != previousBuild) {
                Map<String, String> properties = new HashMap<>();
                properties.put("version", currentVersion);
                properties.put("build", "" + currentBuild);
                properties.put("previous_version", previousVersion);
                properties.put("previous_build", "" + previousBuild);
                track("Application Updated", properties);
            }

            // Track Application Opened.
            Map<String, String> properties = new HashMap<>();
            properties.put("version", currentVersion);
            properties.put("build", "" + currentBuild);
            track("Application Opened", properties);
        }

        // Update the recorded version.
        storageHelper.setVersion(currentVersion);
        storageHelper.setBuild(currentBuild);
    }

    public static void setupWithConfiguration(Application application, Configuration configuaration) {
        if (instance == null) {
            instance = new Castle(application, configuaration);

            if (publishableKey() == null || !publishableKey().startsWith("pk_")) {
                throw new RuntimeException("You must provide a valid Castle publishable key when initializing the SDK.");
            }

            instance.registerLifeCycleCallbacks(application);
        }
    }

    public static void setupWithDefaultConfiguration(Application application) {
        setupWithConfiguration(application, new Configuration(application));
    }

    public static void track(String event, Map<String, String> properties) {
        track(new Event(event, properties));
    }

    public static void track(String event) {
        track(new Event(event));
    }

    private static void track(Event event) {
        instance.eventQueue.add(event);
        flush();
    }

    public static void identify(String userId) {
        track(new IdentifyEvent(userId));
    }

    public static void identify(String userId, Map<String, String> traits) {
        Castle.userId(userId);
        track(new IdentifyEvent(userId, traits));
    }

    private static void userId(String userId) {
        instance.storageHelper.setIdentity(userId);
    }

    public static String userId() {
        return instance.storageHelper.getIdentity();
    }

    public static void reset() {
        // this should also flush the queue
        Castle.userId(null);
        Castle.flush();
    }

    public static void screen(String name, Map<String, String> properties) {
        track(new ScreenEvent(name, properties));
    }

    public static void screen(String name) {
        track(new ScreenEvent(name));
    }

    public static void screen(Activity activity) {
        track(new ScreenEvent(activity));
    }

    public static String publishableKey() {
        return instance.configuration.publishableKey();
    }

    public static String deviceIdentifier() {
        return instance.identifier;
    }

    public static Configuration configuration() {
        return instance.configuration;
    }

    public static void flush() {
        try {
            instance.eventQueue.flush();
        } catch (IOException exception) {
            CastleLogger.e("Unable to flush queue", exception);
        }
    }

    public static Map<String, String> headers(String url) {
        Map<String, String> headers = new HashMap<>();

        if (isUrlWhitelisted(url)) {
            headers.put("__cid", Castle.deviceIdentifier());
            headers.put("X-Castle-Cookie", Castle.deviceIdentifier());
            headers.put("Castle-Device-Id", Castle.deviceIdentifier());
        }

        return headers;
    }

    public static CastleInterceptor castleInterceptor() {
        return new CastleInterceptor();
    }

    private static boolean isUrlWhitelisted(String urlString) {
        try {
            URL url = new URL(urlString);
            String baseUrl = url.getProtocol() + "://" + url.getHost();

            if (!Castle.configuration().baseURLWhiteList().isEmpty()) {
                if (Castle.configuration().baseURLWhiteList().contains(baseUrl)) {
                    return true;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }
}