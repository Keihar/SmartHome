package com.keihar.smarthome;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView txtCurrentTemp = null;
    TextView subtxtCurrentTemp = null;
    TextView txtTargetTemp = null;
    TextView subtxtTargetTemp = null;
    TextInputEditText inputUpdateTemp = null;
    Button btnUpdateTemp = null;
    FloatingActionButton micBtn = null;

    SpeechRecognizer speechRecognizer;
    int targetTemp = 0;
    int currentTemp = 18;
    final int RecordAudioRequestCode = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtCurrentTemp = (TextView) findViewById(R.id.ctemp);
        subtxtCurrentTemp = (TextView) findViewById(R.id.ctemp_bottom);
        txtTargetTemp = (TextView) findViewById(R.id.ttemp);
        subtxtTargetTemp = (TextView) findViewById(R.id.ttemp_bottom);
        inputUpdateTemp = (TextInputEditText) findViewById(R.id.txtUpdateTemp);
        btnUpdateTemp = (Button) findViewById(R.id.btnUpdateTemp);
        micBtn = (FloatingActionButton) findViewById(R.id.micFab);

        // Presses submit btn when clicking "done" on keyboard
        inputUpdateTemp.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE))
                btnUpdateTemp.performClick();
            return false;
        });

        btnUpdateTemp.setOnClickListener(v -> {
            String text = String.valueOf(inputUpdateTemp.getText());
            try{
                targetTemp = Integer.parseInt(text);
                // Hides Keyboard
                View view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                // Updates UI Elements
                updateTempUI();
            }
            catch (Exception e){}
        });

        // Speech Permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        // Speech Set Up
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                System.out.println("ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                System.out.println("beggining of speech");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                System.out.println("end of speech");
            }

            @Override
            public void onError(int i) {
                System.out.println("error " + i);
            }

            @Override
            public void onResults(Bundle bundle) {
                System.out.println("results ready");
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                subtxtCurrentTemp.setText(data.get(0));
                System.out.println(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                System.out.println("partial results");
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        micBtn.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                speechRecognizer.cancel();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                speechRecognizer.startListening(speechRecognizerIntent);
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }


    protected void updateTempUI(){
        txtTargetTemp.setText(targetTemp + "°C");
        int tempLeft = targetTemp - currentTemp;
        subtxtTargetTemp.setText(tempLeft + "°C left");
        int percentageLeft = 100 - (targetTemp * 100) / currentTemp;
        subtxtCurrentTemp.setText(percentageLeft + "% of target");

        // Snackbar section
        final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), "Updated Temperature", Snackbar.LENGTH_LONG);
        snackBar.setTextColor(Color.WHITE);
        snackBar.setAction("OK", v -> {
            snackBar.dismiss();
        });
        snackBar.show();
    }
}