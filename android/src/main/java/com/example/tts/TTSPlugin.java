package com.example.tts;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "TTS")
public class TTSPlugin extends Plugin {

    private TTS implementation;

    @Override
    public void load() {
        implementation = new TTS(getContext());
    }

    @PluginMethod
    public void speak(PluginCall call) {
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
        };

        try {
            implementation.speak(text, rate, pitch, volume, pan, voiceURI, call.getCallbackId(), audioStreamType, callback);
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        boolean isAvailable = implementation.isAvailable();
        if (!isAvailable) {
            call.unavailable("Not yet initialized or not available on this device.");
            return;
        }
        try {
            implementation.stop();
            call.resolve();
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @PluginMethod
    public void getSupportedLanguages(PluginCall call) {
        try {
            JSArray languages = implementation.getSupportedLanguages();
            JSObject ret = new JSObject();
            ret.put("languages", languages);
            call.resolve(ret);
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @PluginMethod
    public void getSupportedVoices(PluginCall call) {
        try {
            JSArray voices = implementation.getSupportedVoices();
            JSObject ret = new JSObject();
            ret.put("voices", voices);
            call.resolve(ret);
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @PluginMethod
    public void getSupportedEngines(PluginCall call) {
        try {
            JSArray engines = implementation.getSupportedEngines();
            JSObject ret = new JSObject();
            ret.put("engines", engines);
            call.resolve(ret);
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @PluginMethod
    public void getDefaults(PluginCall call) {
        try {
            JSObject ret = implementation.getDefaults();
            call.resolve(ret);
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @PluginMethod
    public void switchEngine(PluginCall call) {
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

            };
            implementation.switchEngine(engineName, resultCallback, getContext());
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @PluginMethod
    public void getMaxSpeechInputLength(PluginCall call) {
        int max = implementation.getMaxSpeechInputLength();
        JSObject obj = new JSObject();
        obj.put("maxSpeechInputLength", max);
        call.resolve(obj);
    }

    @PluginMethod
    public void openInstall(PluginCall call) {
        try {
            implementation.openInstall(this.getContext());
            call.resolve();
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
        }
    }

    @Override
    protected void handleOnDestroy() {
        implementation.onDestroy();
    }

}
