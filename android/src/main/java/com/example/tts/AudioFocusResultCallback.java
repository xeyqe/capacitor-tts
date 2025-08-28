package com.example.tts;

import com.getcapacitor.JSObject;

public interface AudioFocusResultCallback {
    void onDone();
    void onError(String errorMessage);
    void onAudioFocusChange(JSObject obj);
    void onPauseTTS();
}

