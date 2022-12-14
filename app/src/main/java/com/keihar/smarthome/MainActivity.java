package com.keihar.smarthome;

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
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView txtCurrentTemp = null;
    TextView subtxtCurrentTemp = null;
    TextView txtTargetTemp = null;
    TextView subtxtTargetTemp = null;
    TextInputEditText inputUpdateTemp = null;
    Button btnUpdateTemp = null;

    int targetTemp = 0;
    int currentTemp = 18;

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
//
//        // Speech Permissions
//        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
//            checkPermission();
//        }
//
//        // Speech Set Up
//        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
//        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }

//    private void checkPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RecordAudioRequestCode);
//        }
//    }

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