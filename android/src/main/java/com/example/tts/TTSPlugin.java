package com.example.tts;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "TTS", permissions = { @Permission(alias = "notifications", strings = { Manifest.permission.POST_NOTIFICATIONS }) })
public class TTSPlugin extends Plugin {

    private AudioFocus audioFocusImplementation;

    private TTS implementation;
    private TTSForegroundService service;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Runnable> pendingCalls = new ArrayList<>();
    private boolean bound;
    private boolean destroyed;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (destroyed) return;
            service = ((TTSForegroundService.LocalBinder) binder).getService();
            implementation = service.getTTS();
            List<Runnable> calls = new ArrayList<>(pendingCalls);
            pendingCalls.clear();
            for (Runnable call : calls) {
                call.run();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            implementation = null;
        }
    };

    private void withService(PluginCall call, Runnable operation) {
        handler.post(() -> {
            if (destroyed) {
                call.reject("TTS plugin has been destroyed.");
                return;
            }
            Runnable guarded = () -> {
                if (service == null) {
                    call.reject("TTS service is unavailable.");
                    return;
                }
                try {
                    operation.run();
                } catch (Exception ex) {
                    call.reject(ex.getLocalizedMessage());
                }
            };
            if (service != null) {
                guarded.run();
            } else if (bound) {
                pendingCalls.add(guarded);
            } else {
                call.reject("Unable to bind to the TTS service.");
            }
        });
    }

    @Override
    public void load() {
        audioFocusImplementation = new AudioFocus(getContext().getApplicationContext());
        handler.post(() -> {
            if (!destroyed) {
                bound = getContext()
                    .getApplicationContext()
                    .bindService(new Intent(getContext(), TTSForegroundService.class), connection, Context.BIND_AUTO_CREATE);
            }
        });
    }

    @PluginMethod
    public void requestForegroundPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || getPermissionState("notifications") == PermissionState.GRANTED) {
            foregroundPermissionsCallback(call);
        } else {
            requestPermissionForAlias("notifications", call, "foregroundPermissionsCallback");
        }
    }

    @PermissionCallback
    private void foregroundPermissionsCallback(PluginCall call) {
        JSObject result = new JSObject();
        result.put(
            "notifications",
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ? "granted" : getPermissionState("notifications").toString()
        );
        call.resolve(result);
    }

    @PluginMethod
    public void speak(PluginCall call) {
        withService(call, () -> {
            String text = call.getString("text", "");
            float rate = call.getFloat("rate", 1.0F);
            float pitch = call.getFloat("pitch", 1.0F);
            float volume = call.getFloat("volume", 1.0F);
            float pan = call.getFloat("pan", 0.0F);
            String voiceURI = call.getString("voiceURI", "");
            String audioStreamType = call.getString("audioStreamType", "");

            TTSResultCallback callback = new TTSResultCallback() {
                @Override
                public void onDone() {
                    call.resolve();
                }

                @Override
                public void onError(String errorMessage) {
                    call.reject(errorMessage);
                }

                @Override
                public void onProgress(JSObject obj) {
                    notifyListeners("progressEvent", obj);
                }

                @Override
                public void onArrayProgress(JSObject progress) {}
            };

            try {
                service.cancelReading();
                implementation.speak(text, rate, pitch, volume, pan, voiceURI, audioStreamType, call.getCallbackId(), callback);
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void stop(PluginCall call) {
        withService(call, () -> {
            boolean isAvailable = implementation.isAvailable();
            if (!isAvailable) {
                call.unavailable("Not yet initialized or not available on this device.");
                return;
            }
            try {
                service.stopReading();
                call.resolve();
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void getSupportedLanguages(PluginCall call) {
        withService(call, () -> {
            try {
                JSArray languages = implementation.getSupportedLanguages();
                JSObject ret = new JSObject();
                ret.put("languages", languages);
                call.resolve(ret);
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void getSupportedVoices(PluginCall call) {
        withService(call, () -> {
            try {
                JSArray voices = implementation.getSupportedVoices();
                JSObject ret = new JSObject();
                ret.put("voices", voices);
                call.resolve(ret);
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void getSupportedEngines(PluginCall call) {
        withService(call, () -> {
            try {
                JSArray engines = implementation.getSupportedEngines();
                JSObject ret = new JSObject();
                ret.put("engines", engines);
                call.resolve(ret);
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void getDefaults(PluginCall call) {
        withService(call, () -> {
            try {
                JSObject ret = implementation.getDefaults();
                call.resolve(ret);
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void switchEngine(PluginCall call) {
        withService(call, () -> {
            String engineName = call.getString("engineName", "");

            try {
                TTSResultCallback resultCallback = new TTSResultCallback() {
                    @Override
                    public void onDone() {
                        call.resolve();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        call.reject(errorMessage);
                    }

                    @Override
                    public void onProgress(JSObject obj) {}

                    @Override
                    public void onArrayProgress(JSObject progress) {}
                };
                service.stopReading();
                implementation.switchEngine(engineName, resultCallback, getContext().getApplicationContext());
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void getMaxSpeechInputLength(PluginCall call) {
        withService(call, () -> {
            int max = implementation.getMaxSpeechInputLength();
            JSObject obj = new JSObject();
            obj.put("maxSpeechInputLength", max);
            call.resolve(obj);
        });
    }

    @PluginMethod
    public void openInstall(PluginCall call) {
        withService(call, () -> {
            try {
                implementation.openInstall(this.getContext());
                call.resolve();
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @Override
    protected void handleOnDestroy() {
        handler.post(() -> {
            destroyed = true;
            service = null;
            implementation = null;
            for (Runnable call : pendingCalls) {
                call.run();
            }
            pendingCalls.clear();
            if (bound) {
                getContext().getApplicationContext().unbindService(connection);
                bound = false;
            }
        });
    }

    @PluginMethod
    public void read(PluginCall call) {
        withService(call, () -> {
            JSArray texts = call.getArray("texts");
            int progress = call.getInt("progress", 0);
            float rate = call.getFloat("rate", 1.0F);
            float pitch = call.getFloat("pitch", 1.0F);
            float volume = call.getFloat("volume", 1.0F);
            float pan = call.getFloat("pan", 0.0F);
            int bookId = call.getInt("bookId", 0);
            String voiceURI = call.getString("voiceURI", "");
            String audioStreamType = call.getString("audioStreamType", "");

            TTSResultCallback callback = new TTSResultCallback() {
                @Override
                public void onDone() {
                    call.resolve();
                }

                @Override
                public void onError(String errorMessage) {
                    call.reject(errorMessage);
                }

                @Override
                public void onProgress(JSObject obj) {
                    notifyListeners("progressEvent", obj);
                }

                @Override
                public void onArrayProgress(JSObject progress) {
                    notifyListeners("progressArrayEvent", progress);
                }
            };

            try {
                if (texts == null || progress < 0 || progress > texts.length()) {
                    call.reject("texts must be an array and progress must be within its bounds.");
                    return;
                }
                if (progress == texts.length()) {
                    service.stopReading();
                    call.resolve();
                    return;
                }
                TTS engine = implementation;
                service.startReading(
                    result ->
                        engine.read(
                            texts,
                            progress,
                            rate,
                            pitch,
                            volume,
                            pan,
                            voiceURI,
                            audioStreamType,
                            call.getCallbackId(),
                            bookId,
                            result
                        ),
                    callback
                );
            } catch (Exception ex) {
                call.reject(ex.getLocalizedMessage());
            }
        });
    }

    @PluginMethod
    public void requestFocus(PluginCall call) {
        withService(call, () -> {
            AudioFocusResultCallback callback = new AudioFocusResultCallback() {
                @Override
                public void onDone() {
                    call.resolve();
                }

                @Override
                public void onError(String errorMessage) {
                    call.reject(errorMessage);
                }

                @Override
                public void onAudioFocusChange(JSObject obj) {
                    notifyListeners("audioFocusChangeEvent", obj);
                }

                @Override
                public void onPauseTTS() {
                    handler.post(() -> {
                        if (service != null) service.stopReading();
                    });
                }
            };

            audioFocusImplementation.requestFocus(callback);
        });
    }

    @PluginMethod
    public void abandonFocus(PluginCall call) {
        withService(call, () -> {
            AudioFocusResultCallback callback = new AudioFocusResultCallback() {
                @Override
                public void onDone() {
                    call.resolve();
                }

                @Override
                public void onError(String errorMessage) {
                    call.reject(errorMessage);
                }

                @Override
                public void onAudioFocusChange(JSObject obj) {
                    notifyListeners("audioFocusChangeEvent", obj);
                }

                @Override
                public void onPauseTTS() {
                    handler.post(() -> {
                        if (service != null) service.stopReading();
                    });
                }
            };

            audioFocusImplementation.abandonFocus(callback);
        });
    }
}
