package io.castle.android;

import android.app.Application;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.castle.android.api.model.Event;
import io.castle.android.api.model.ScreenEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Copyright (c) 2017 Castle
 */
@RunWith(AndroidJUnit4.class)
public class CastleTest {
    @Rule
    public ActivityTestRule<TestActivity> rule  = new ActivityTestRule<>(TestActivity.class);

    private Application application;
    private OkHttpClient client;

    @Before
    public void setup() {
        application = (Application) InstrumentationRegistry.getTargetContext().getApplicationContext();

        ArrayList<String> baseUrlWhiteList = new ArrayList<>();
        baseUrlWhiteList.add("https://google.com/");

        Configuration configuration = new Configuration(application);
        configuration.publishableKey("pk_SE5aTeotKZpDEn8kurzBYquRZy");
        configuration.screenTrackingEnabled(true);
        configuration.baseURLWhiteList(baseUrlWhiteList);

        Castle.setupWithConfiguration(application, configuration);

        client = new OkHttpClient.Builder()
                .addInterceptor(Castle.castleInterceptor())
                .build();
    }

    @Test
    public void testDeviceIdentifier() {
        // Check device ID
        Assert.assertNotNull(Castle.deviceIdentifier());
    }

    @Test
    public void testUserIdPersistance() {
        // Make sure the user id is persisted correctly after identify
        Castle.identify("thisisatestuser");

        // Check that the stored identity is the same as the identity we tracked
        Assert.assertEquals(Castle.userId(), "thisisatestuser");
    }

    @Test
    public void testReset() {
        Castle.reset();

        // Check to see if the user identity was cleared on reset
        Assert.assertNull(Castle.userId());
    }

    @Test
    public void testTracking() {
        // This should lead to no event being tracked since empty string isn't a valid name
        int count = Castle.queueSize();
        Castle.track("");
        int newCount = Castle.queueSize();
        Assert.assertEquals(count, newCount);

        count = Castle.queueSize();
        Castle.track("Event");
        newCount = Castle.queueSize();
        Assert.assertEquals(count + 1, newCount);

        count = Castle.queueSize();
        Castle.track("Event", new HashMap<String, String>());
        newCount = Castle.queueSize();
        Assert.assertEquals(count + 1, newCount);

        count = Castle.queueSize();
        Castle.track("Event", null);
        newCount = Castle.queueSize();
        Assert.assertEquals(count, newCount);

        // This should lead to no event being tracked since empty string isn't a valid name
        count = Castle.queueSize();
        Castle.screen("");
        newCount = Castle.queueSize();
        Assert.assertEquals(count, newCount);

        // This should lead to no event being tracked properties can't be nil
        count = Castle.queueSize();
        Castle.screen("Screen", null);
        newCount = Castle.queueSize();
        Assert.assertEquals(count, newCount);

        // This should lead to no event being tracked since identity can't be an empty string
        count = Castle.queueSize();
        Castle.identify("");
        newCount = Castle.queueSize();
        Assert.assertEquals(count, newCount);

        // This should lead to no event being tracked properties can't be nil
        count = Castle.queueSize();
        Castle.identify("testuser1", null);
        newCount = Castle.queueSize();
        Assert.assertEquals(count, newCount);

        ScreenEvent screenEvent = new ScreenEvent("Main");
        Assert.assertEquals(screenEvent.getEvent(), "Main");
        Assert.assertEquals(screenEvent.getType(), Event.EVENT_TYPE_SCREEN);

        count = Castle.queueSize();
        Castle.screen("Main");
        newCount = Castle.queueSize();
        Assert.assertEquals(count + 1, newCount);

        count = Castle.queueSize();
        Castle.screen("Main", new HashMap<String, String>());
        newCount = Castle.queueSize();
        Assert.assertEquals(count + 1, newCount);

        count = Castle.queueSize();
        Castle.screen(rule.getActivity());
        newCount = Castle.queueSize();
        Assert.assertEquals(count + 1, newCount);

        count = Castle.queueSize();
        Castle.identify("testuser1");
        newCount = Castle.queueSize();
        Assert.assertEquals(count + 1, newCount);

        count = Castle.queueSize();
        Castle.identify("testuser1", new HashMap<String, String>());
        newCount = Castle.queueSize();
        Assert.assertEquals(count + 1, newCount);

        Castle.flush();

        while (Castle.isFlushingQueue()) {
            // wait until flush is finished
        }
    }

    @Test
    public void testErrorParsing() {
        io.castle.android.api.model.Error error = Utils.getGsonInstance().fromJson("{ \"type\": \"type\", \"message\": \"message\" }", io.castle.android.api.model.Error.class);
        Assert.assertEquals("type", error.getType());
        Assert.assertEquals("message", error.getMessage());
        Assert.assertEquals("type message", error.toString());
    }

    @Test
    public void testDefaultHeaders() {
        Map<String, String> headers = Castle.headers("https://google.com/test");
        Assert.assertNotNull(headers);
        Assert.assertTrue(!headers.isEmpty());
        Assert.assertTrue(headers.containsKey("X-Castle-Client-Id"));
        Assert.assertEquals(headers.get("X-Castle-Client-Id"), Castle.deviceIdentifier());
    }

    @Test
    public void testRequestInterceptor() throws IOException {
        Request request = new Request.Builder()
                .url("https://google.com/test")
                .build();

        Response response = client.newCall(request).execute();
        Assert.assertEquals(Castle.deviceIdentifier(), response.request().header("X-Castle-Client-Id"));

        request = new Request.Builder()
                .url("https://example.com/test")
                .build();

        response = client.newCall(request).execute();
        Assert.assertEquals(null, response.request().header("X-Castle-Client-Id"));
    }
}
