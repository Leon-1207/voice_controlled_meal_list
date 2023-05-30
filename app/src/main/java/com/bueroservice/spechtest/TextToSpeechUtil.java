package com.bueroservice.spechtest;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechUtil {

    private TextToSpeech tts;

    public void speakText(Context context, String textToSpeak) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Diese Sprache wird nicht unterstützt.");
                    } else {
                        tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
                    }
                } else {
                    Log.e("TTS", "Initialisierung fehlgeschlagen.");
                }
            }
        });
    }

    public void shutdownTextToSpeech() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
