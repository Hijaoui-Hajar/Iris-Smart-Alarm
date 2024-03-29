package com.example.smartalarme2;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener, TextToSpeech.OnInitListener {

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;

    private boolean isAlarmOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound);

        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];
        if (lux > 50 && !isAlarmOn) { // Si la luminosité est supérieure à 50 lux
            startAlarm();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ne pas nécessairement besoin d'implémenter cette méthode
    }

    private void startAlarm() {
        isAlarmOn = true;
        mediaPlayer.start();
        Toast.makeText(this, "It's morning. The time now is "
                + java.time.LocalTime.now().toString(), Toast.LENGTH_LONG).show();

        // Dire un message "Good morning" à l'utilisateur
        textToSpeech.speak("Good morning", TextToSpeech.QUEUE_FLUSH, null, null);

        // Obtenir l'heure actuelle et la dire à l'utilisateur
        String currentTime = java.time.LocalTime.now().toString();
        textToSpeech.speak("The time now is " + currentTime, TextToSpeech.QUEUE_ADD, null, null);

        // Démarre la reconnaissance vocale pour écouter la commande de l'utilisateur
        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say stop to turn off the alarm");

        try {
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            Toast.makeText(this, "Your device doesn't support speech recognition", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0).toLowerCase();
                if (spokenText.contains("stop")) {
                    stopAlarm();
                } else {
                    startVoiceRecognition(); // Redémarrer la reconnaissance vocale si la commande n'est pas "stop"
                }
            }
        }
    }

    private void stopAlarm() {
        isAlarmOn = false;
        mediaPlayer.stop();
        mediaPlayer.reset();
        textToSpeech.speak("Alarm stopped", TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
