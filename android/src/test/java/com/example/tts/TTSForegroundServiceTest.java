package com.example.tts;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowTextToSpeech;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class TTSForegroundServiceTest {

    private TTSForegroundService service;

    @Before
    public void setUp() {
        service = Robolectric.buildService(TTSForegroundService.class).create().get();
    }

    @After
    public void tearDown() {
        service.onDestroy();
    }

    private TTSResultCallback start(RecordingCallback result) {
        AtomicReference<TTSResultCallback> callback = new AtomicReference<>();
        service.startReading(callback::set, result);
        // Deliver the start intent as Android would after startForegroundService().
        service.onStartCommand(new Intent(), 0, 1);
        assertNotNull(callback.get());
        assertNotNull(shadowOf(service).getLastForegroundNotification());
        assertTrue(wakeLock().isHeld());
        return callback.get();
    }

    private PowerManager.WakeLock wakeLock() {
        return ReflectionHelpers.getField(service, "wakeLock");
    }

    @Test
    public void completionRemovesNotificationAndSettlesOnce() {
        RecordingCallback result = new RecordingCallback();
        TTSResultCallback callback = start(result);
        callback.onDone();
        callback.onDone();
        callback.onError("late error");
        assertEquals(1, result.completed);
        assertTrue(result.errors.isEmpty());
        assertNull(shadowOf(service).getLastForegroundNotification());
        assertTrue(shadowOf(service).isStoppedBySelf());
        assertFalse(wakeLock().isHeld());
    }

    @Test
    public void replacementIgnoresOldCompletion() {
        RecordingCallback first = new RecordingCallback();
        TTSResultCallback old = start(first);
        shadowOf(service).getNextStartedService();
        RecordingCallback second = new RecordingCallback();
        AtomicReference<TTSResultCallback> next = new AtomicReference<>();
        service.startReading(next::set, second);
        TTSResultCallback current = next.get();
        assertNotNull(current);
        assertNull(shadowOf(service).getNextStartedService());
        assertEquals(1, first.errors.size());
        old.onDone();
        assertEquals(0, second.completed);
        assertNotNull(shadowOf(service).getLastForegroundNotification());
        current.onDone();
        assertEquals(1, second.completed);
        assertNull(shadowOf(service).getLastForegroundNotification());
        assertFalse(wakeLock().isHeld());
    }

    @Test
    public void stopBeforeStartDoesNotRunPendingRead() {
        RecordingCallback result = new RecordingCallback();
        AtomicReference<TTSResultCallback> task = new AtomicReference<>();
        service.startReading(task::set, result);
        service.stopReading();
        service.onStartCommand(new Intent(), 0, 1);
        assertNull(task.get());
        assertEquals(1, result.errors.size());
        assertNull(shadowOf(service).getLastForegroundNotification());
    }

    @Test
    public void notificationStopActionCancelsRead() {
        RecordingCallback result = new RecordingCallback();
        TTSResultCallback callback = start(result);
        Intent stop = shadowOf(shadowOf(service).getLastForegroundNotification().actions[0].actionIntent).getSavedIntent();
        service.onStartCommand(stop, 0, 2);
        callback.onDone();
        assertEquals(0, result.completed);
        assertEquals(1, result.errors.size());
        assertNull(shadowOf(service).getLastForegroundNotification());
    }

    @Test
    public void readFailureRemovesNotification() {
        RecordingCallback result = new RecordingCallback();
        start(result).onError("synthesis failed");
        assertEquals(Arrays.asList("synthesis failed"), result.errors);
        assertNull(shadowOf(service).getLastForegroundNotification());
        assertFalse(wakeLock().isHeld());
    }

    @Test
    public void allSegmentsFinishBeforeReadResolves() {
        TTS engine = service.getTTS();
        // Initialization is asynchronous on a device; mark the shadow engine ready.
        ReflectionHelpers.setField(engine, "status", TextToSpeech.SUCCESS);
        RecordingCallback result = new RecordingCallback();
        JSArray texts = JSArray.from(new String[] { "first", "second", "third" });
        service.startReading(callback -> engine.read(texts, 0, 1, 1, 1, 0, "", "STREAM_MUSIC", "read", 0, callback), result);
        service.onStartCommand(new Intent(), 0, 1);
        assertEquals(0, result.completed);
        assertNotNull(shadowOf(service).getLastForegroundNotification());
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(
            Arrays.asList("first", "second", "third"),
            shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()).getSpokenTextList()
        );
        assertEquals(Arrays.asList(0, 1, 2), result.progress);
        assertEquals(1, result.completed);
        assertTrue(result.errors.isEmpty());
        assertNull(shadowOf(service).getLastForegroundNotification());
    }

    @Test
    public void stoppedReadCannotQueueMoreSegments() {
        TTS engine = service.getTTS();
        ReflectionHelpers.setField(engine, "status", TextToSpeech.SUCCESS);
        RecordingCallback result = new RecordingCallback();
        service.startReading(
            callback ->
                engine.read(JSArray.from(new String[] { "first", "second" }), 0, 1, 1, 1, 0, "", "STREAM_MUSIC", "read", 0, callback),
            result
        );
        service.onStartCommand(new Intent(), 0, 1);
        service.stopReading();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(Arrays.asList("first"), shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()).getSpokenTextList());
        assertEquals(0, result.completed);
        assertEquals(1, result.errors.size());
    }

    private static class RecordingCallback implements TTSResultCallback {

        int completed;
        final List<String> errors = new ArrayList<>();
        final List<Integer> progress = new ArrayList<>();

        @Override
        public void onDone() {
            completed++;
        }

        @Override
        public void onError(String message) {
            errors.add(message);
        }

        @Override
        public void onProgress(JSObject progress) {}

        @Override
        public void onArrayProgress(JSObject value) {
            progress.add(value.getInteger("progress"));
        }
    }
}
