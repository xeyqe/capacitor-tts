package com.example.tts;

import static org.junit.Assert.*;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.PluginCall;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class TTSForegroundPermissionsTest {

    @Test
    @Config(sdk = 32)
    public void olderAndroidResolvesWithoutPermissionRequest() {
        RecordingPlugin plugin = new RecordingPlugin();
        RecordingCall call = new RecordingCall();
        plugin.requestForegroundPermissions(call);
        assertEquals("granted", call.result.getString("notifications"));
        assertNull(plugin.requestedCall);
    }

    @Test
    public void grantedPermissionDoesNotPromptAgain() {
        RecordingPlugin plugin = new RecordingPlugin();
        plugin.state = PermissionState.GRANTED;
        RecordingCall call = new RecordingCall();
        plugin.requestForegroundPermissions(call);
        assertEquals("granted", call.result.getString("notifications"));
        assertNull(plugin.requestedCall);
    }

    @Test
    public void requestWaitsForUserAndReturnsTheirDecision() throws Exception {
        for (PermissionState decision : new PermissionState[] { PermissionState.GRANTED, PermissionState.DENIED, PermissionState.PROMPT }) {
            RecordingPlugin plugin = new RecordingPlugin();
            RecordingCall call = new RecordingCall();
            plugin.requestForegroundPermissions(call);
            assertEquals("notifications", plugin.requestedAlias);
            assertSame(call, plugin.requestedCall);
            assertNull(call.result);
            plugin.state = decision;
            Method callback = TTSPlugin.class.getDeclaredMethod(plugin.callbackName, PluginCall.class);
            callback.setAccessible(true);
            callback.invoke(plugin, call);
            assertEquals(decision.toString(), call.result.getString("notifications"));
        }
    }

    private static class RecordingPlugin extends TTSPlugin {

        PermissionState state = PermissionState.PROMPT;
        String requestedAlias;
        PluginCall requestedCall;
        String callbackName;

        @Override
        public PermissionState getPermissionState(String alias) {
            assertEquals("notifications", alias);
            return state;
        }

        @Override
        protected void requestPermissionForAlias(String alias, PluginCall call, String callback) {
            requestedAlias = alias;
            requestedCall = call;
            callbackName = callback;
        }
    }

    private static class RecordingCall extends PluginCall {

        JSObject result;

        RecordingCall() {
            super(null, "TTS", "permission-test", "requestForegroundPermissions", new JSObject());
        }

        @Override
        public void resolve(JSObject data) {
            result = data;
        }
    }
}
