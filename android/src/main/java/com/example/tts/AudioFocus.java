package com.example.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import com.getcapacitor.JSObject;

public class AudioFocus {

    AudioManager am;
    Boolean delayedFocus;
    AudioFocusRequest focusRequest;

    public AudioFocus(Context context) {
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void requestFocus(AudioFocusResultCallback callback) {
        AudioManager.OnAudioFocusChangeListener listener = focusChange -> {
            JSObject obj = new JSObject();
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN -> {
                    obj.put("type", "AUDIOFOCUS_GAIN");
                    callback.onAudioFocusChange(obj);
                    if (delayedFocus) {
                        delayedFocus = false;
                        callback.onDone();
                    }
                }
                case AudioManager.AUDIOFOCUS_LOSS -> {
                    delayedFocus = false;
                    obj.put("type", "AUDIOFOCUS_LOSS");
                    callback.onAudioFocusChange(obj);
                    callback.onPauseTTS();
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    delayedFocus = true;
                    obj.put("type", "AUDIOFOCUS_LOSS_TRANSIENT");
                    callback.onAudioFocusChange(obj);
                    callback.onPauseTTS();
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    delayedFocus = true;
                    obj.put("type", "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    callback.onAudioFocusChange(obj);
                    callback.onPauseTTS();
                }
            }
        };

        AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();

        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(listener)
                .build();

        final int audioFocusRequest = am.requestAudioFocus(focusRequest);

        if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            callback.onError("AUDIOFOCUS_REQUEST_FAILED");
        } else if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            callback.onDone();
        } else if (audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            delayedFocus = true;
        }
    }

    public void abandonFocus(AudioFocusResultCallback callback) {
        if (focusRequest != null) {
            int respond = am.abandonAudioFocusRequest(focusRequest);
            if (respond == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                callback.onDone();
            } else if (respond == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                callback.onError("AUDIOFOCUS_REQUEST_FAILED");
            }
            focusRequest = null;
        } else {
            callback.onDone();
        }
    }
}
