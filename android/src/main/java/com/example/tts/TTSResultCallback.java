package com.example.tts;

import com.getcapacitor.JSObject;

public interface TTSResultCallback {
    void onDone();
    void onError(String errorMessage);
    void onProgress(JSObject obj);
}
