package com.example.tts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TTS {
    public static final String LOG_TAG = "myTTS";
    private int status = TextToSpeech.ERROR;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int readGeneration;
    private TTSResultCallback callback;
    private android.speech.tts.TextToSpeech tts = null;

    private int progress = 0;
    private String speechParamsTxt = "";
    private DB dbImplementation;

    public TTS(Context context) {
        dbImplementation = new DB(context);
        try {
            tts = new android.speech.tts.TextToSpeech(context, status -> {
                this.status = status;
                if (status == TextToSpeech.SUCCESS) {
                    setProgressListener(this.callback);
                } else {
                    if (this.callback != null) {
                        this.callback.onError("Initialization failed! " + this.getMessageFromStatusCode(status));
                    }
                }
            });
        } catch (Exception ex) {
            Log.d(LOG_TAG, ex.getLocalizedMessage());
        }
    }

    private void setProgressListener(TTSResultCallback callback) {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.i(LOG_TAG, "onStart: " + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                Log.i(LOG_TAG, "onDone: " + utteranceId);

                if (callback != null) {
                    callback.onDone();
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.i(LOG_TAG, "onError: " + utteranceId);

                if (callback != null) {
                    callback.onError("Error: " + utteranceId);
                }
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                Log.i(LOG_TAG, "onStop: " + utteranceId);

                if (callback != null) {
                    callback.onError("Canceled: " + utteranceId + " " + interrupted);
                }
            }

            @Override
            public void onRangeStart(String utteranceId, int start, int end, int frame) {
                Log.i(LOG_TAG, "onRange: " + utteranceId + " start " + start + " end " + end + " frame " + frame);

                if (callback != null) {
                    JSObject obj = new JSObject();
                    obj.put("utteranceId", utteranceId);
                    obj.put("start", start);
                    obj.put("end", end);
                    obj.put("frame", frame);
                    callback.onProgress(obj);
                }
            }
        });
    }

    public void speak(
        String text,
        float rate,
        float pitch,
        float volume,
        float pan,
        String voiceURI,
        String audioStreamType,
        String callbackId,
        TTSResultCallback callback
    ) {
        this.callback = callback;
        this.setProgressListener(callback);
        String speakParams = rate + pitch + volume + pan + voiceURI + audioStreamType + callbackId;
        Log.i(LOG_TAG, "speakParams: " + speakParams);
        Bundle params = null;

        if (!this.speechParamsTxt.equals(speakParams)) {
            this.speechParamsTxt = speakParams;
            if (!voiceURI.isEmpty()) {
                ArrayList<Voice> supportedVoices = getSupportedVoicesOrdered();
                supportedVoices.stream()
                        .filter(item -> voiceURI.equals(item.getName()))
                        .findAny().ifPresent(voice -> tts.setVoice(voice));
            }

            tts.setSpeechRate(rate);
            tts.setPitch(pitch);
            int streamType = this.getAudioStreamType(audioStreamType);

            params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType);
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, pan);
        }

        this.tts.speak(text, TextToSpeech.QUEUE_ADD, params, callbackId);
    }

    public void speak(
        JSArray texts,
        float rate,
        float pitch,
        float volume,
        float pan,
        String voiceURI,
        String audioStreamType,
        String callbackId,
        int bookId,
        int prgrs,
        TTSResultCallback callback
    ) {
        final int generation = readGeneration;
        final int index = prgrs;
        final String utteranceId = callbackId + ":" + index;
        this.callback = callback;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            private boolean isCurrent(String id) {
                return generation == readGeneration && utteranceId.equals(id);
            }

            @Override
            public void onStart(String id) {}

            @Override
            public void onDone(String id) {
                handler.post(() -> {
                    if (!isCurrent(id)) return;
                    try {
                        dbImplementation.updateProgressOfBook(bookId, (index + 1) + "/" + (texts.length() - 1));
                        JSObject obj = new JSObject();
                        obj.put("progress", index);
                        callback.onArrayProgress(obj);
                        if (generation != readGeneration) return;
                        if (progress < texts.length()) {
                            speak(texts, rate, pitch, volume, pan, voiceURI, audioStreamType,
                                callbackId, bookId, progress, callback);
                        } else {
                            callback.onDone();
                        }
                    } catch (Exception ex) {
                        callback.onError(ex.getLocalizedMessage());
                    }
                });
            }

            @Override
            public void onError(String id) {
                handler.post(() -> {
                    if (isCurrent(id)) callback.onError("Error: " + id);
                });
            }

            @Override
            public void onStop(String id, boolean interrupted) {
                handler.post(() -> {
                    if (isCurrent(id)) callback.onError("Canceled: " + id + " " + interrupted);
                });
            }

            @Override
            public void onRangeStart(String id, int start, int end, int frame) {
                handler.post(() -> {
                    if (!isCurrent(id)) return;
                    JSObject obj = new JSObject();
                    obj.put("utteranceId", callbackId);
                    obj.put("start", start);
                    obj.put("end", end);
                    obj.put("frame", frame);
                    callback.onProgress(obj);
                });
            }
        });

        // Every segment needs its own parameters, including volume and stream type.
        if (!voiceURI.isEmpty()) {
            getSupportedVoicesOrdered().stream()
                .filter(voice -> voiceURI.equals(voice.getName()))
                .findAny().ifPresent(voice -> tts.setVoice(voice));
        }
        tts.setSpeechRate(rate);
        tts.setPitch(pitch);
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, getAudioStreamType(audioStreamType));
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, pan);

        try {
            String text = texts.getString(index);
            progress = index + 1;
            if (tts.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId) == TextToSpeech.ERROR) {
                callback.onError("Unable to queue text at index " + index);
            }
        } catch (JSONException ex) {
            callback.onError("Invalid text at index " + index);
        }
    }

    public void read(
        JSArray texts,
        int progress,
        float rate,
        float pitch,
        float volume,
        float pan,
        String voiceURI,
        String audioStreamType,
        String callbackId,
        int bookId,
        TTSResultCallback callback
    ) {
        if (!isAvailable()) {
            callback.onError("Not yet initialized or not available on this device.");
            return;
        }
        if (texts == null || progress < 0 || progress > texts.length()) {
            callback.onError("texts must be an array and progress must be within its bounds.");
            return;
        }
        this.progress = progress;
        readGeneration++;
        this.speechParamsTxt = "";
        if (progress == texts.length()) {
            callback.onDone();
            return;
        }
        speak(texts, rate, pitch, volume, pan, voiceURI, audioStreamType,
            callbackId, bookId, progress, callback);
    }

    private int getAudioStreamType(String audioStreamTypeString) {
        return switch (audioStreamTypeString) {
            case "STREAM_ALARM" -> AudioManager.STREAM_ALARM;
            case "STREAM_DTMF" -> AudioManager.STREAM_DTMF;
            case "STREAM_MUSIC" -> AudioManager.STREAM_MUSIC;
            case "STREAM_NOTIFICATION" -> AudioManager.STREAM_NOTIFICATION;
            case "STREAM_RING" -> AudioManager.STREAM_RING;
            case "STREAM_SYSTEM" -> AudioManager.STREAM_SYSTEM;
            case "STREAM_VOICE_CALL" -> AudioManager.STREAM_VOICE_CALL;
            default -> AudioManager.USE_DEFAULT_STREAM_TYPE;
        };
    }

    private String getMessageFromStatusCode(int statusCode) {
        return switch (statusCode) {
            case TextToSpeech.SUCCESS -> "SUCCESS";
            case TextToSpeech.LANG_COUNTRY_AVAILABLE -> "LANG_COUNTRY_AVAILABLE";
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "LANG_COUNTRY_VAR_AVAILABLE";
            case TextToSpeech.ERROR_INVALID_REQUEST -> "ERROR_INVALID_REQUEST";
            case TextToSpeech.ERROR_NETWORK -> "ERROR_NETWORK";
            case TextToSpeech.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT";
            case TextToSpeech.ERROR_NOT_INSTALLED_YET -> "ERROR_NOT_INSTALLED_YET";
            case TextToSpeech.ERROR_OUTPUT -> "ERROR_OUTPUT";
            case TextToSpeech.ERROR_SERVICE -> "ERROR_SERVICE";
            case TextToSpeech.ERROR_SYNTHESIS -> "ERROR_SYNTHESIS";
            case TextToSpeech.LANG_MISSING_DATA -> "LANG_MISSING_DATA";
            case TextToSpeech.LANG_NOT_SUPPORTED -> "LANG_NOT_SUPPORTED";
            default -> "UNKNOWN_ERROR_CODE: " + statusCode;
        };
    }

    public void switchEngine(String engineName, TTSResultCallback callback, Context context) {
        try {
            status = TextToSpeech.ERROR;
            stop();
            if (tts != null) {
                tts.shutdown();
            }
            tts = new android.speech.tts.TextToSpeech(
                context,
                status -> {
                    this.status = status;
                    if (status == TextToSpeech.SUCCESS) {
                        callback.onDone();
                    } else {
                        callback.onError("Failed to switch to engine: " + engineName + " " + this.getMessageFromStatusCode(status));
                    }
                },
                engineName
            );
        } catch (Exception ex) {
            callback.onError(ex.getLocalizedMessage());
        }
    }

    public int getMaxSpeechInputLength() {
        return TextToSpeech.getMaxSpeechInputLength();
    }

    public boolean isAvailable() {
        return tts != null && this.status == TextToSpeech.SUCCESS;
    }

    public JSObject getDefaults() {
        Voice voice = tts.getDefaultVoice();
        JSObject obj = new JSObject();
        obj.put("rate", 1.0F);
        obj.put("pitch", 1.0F);
        obj.put("volume", 1.0F);
        obj.put("pan", 0.0F);
        obj.put("audioStreamType", "DEFAULT_STREAM_TYPE");
        obj.put("engine", tts.getDefaultEngine());
        obj.put("voiceURI", voice.getName());
        obj.put("language", voice.getLocale().toLanguageTag());
        return obj;
    }

    public ArrayList<Voice> getSupportedVoicesOrdered() {
        Set<Voice> supportedVoices = tts.getVoices();
        ArrayList<Voice> orderedVoices = new ArrayList<>(supportedVoices);

        orderedVoices.sort(Comparator.comparingInt(Voice::hashCode));

        return orderedVoices;
    }
    
    public JSArray getSupportedLanguages() {
        ArrayList<String> languages = new ArrayList<>();
        Set<Locale> supportedLocales = tts.getAvailableLanguages();
        for (Locale supportedLocale : supportedLocales) {
            String tag = supportedLocale.toLanguageTag();
            languages.add(tag);
        }
        return JSArray.from(languages.toArray());
    }

    public JSArray getSupportedVoices() {
        ArrayList<JSObject> voices = new ArrayList<>();
        ArrayList<Voice> supportedVoices = getSupportedVoicesOrdered();
        for (Voice supportedVoice : supportedVoices) {
            JSObject obj = this.convertVoiceToJSObject(supportedVoice);
            voices.add(obj);
        }
        return JSArray.from(voices.toArray());
    }

    public JSArray getSupportedEngines() {
        ArrayList<JSObject> engines = new ArrayList<>();
        List<TextToSpeech.EngineInfo> supportedEngines = tts.getEngines();
        for (android.speech.tts.TextToSpeech.EngineInfo supportedEngine : supportedEngines) {
            JSObject obj = this.convertEngineToJSObject(supportedEngine);
            engines.add(obj);
        }
        return JSArray.from(engines.toArray());
    }

    public void stop() {
        readGeneration++;
        if (tts != null) tts.stop();
    }

    private JSObject convertVoiceToJSObject(Voice voice) {
        Locale locale = voice.getLocale();
        JSObject obj = new JSObject();
        obj.put("voiceURI", voice.getName());
        obj.put("name", locale.getDisplayLanguage() + " " + locale.getDisplayCountry());
        obj.put("lang", locale.toLanguageTag());
        obj.put("localService", !voice.isNetworkConnectionRequired());
        obj.put("default", false);

        return obj;
    }

    private JSObject convertEngineToJSObject(android.speech.tts.TextToSpeech.EngineInfo engine) {
        JSObject obj = new JSObject();
        obj.put("icon", engine.icon);
        obj.put("label", engine.label);
        obj.put("name", engine.name);

        return obj;
    }
    
    public void openInstall(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent installIntent = new Intent();
        installIntent.setAction(android.speech.tts.TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

        ResolveInfo resolveInfo = packageManager.resolveActivity(installIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo != null) {
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(installIntent);
        }
    }

    public void onDestroy() {
        dbImplementation.close();
        if (tts == null) {
            return;
        }
        stop();
        tts.shutdown();
    }

}
