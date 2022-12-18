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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView txtCurrentTemp = null;
    TextView subtxtCurrentTemp = null;
    TextView txtTargetTemp = null;
    TextView subtxtTargetTemp = null;
    TextInputEditText inputUpdateTemp = null;
    Button btnUpdateTemp = null;
    Switch switchActivator1 = null;
    Switch switchActivator2 = null;
    FloatingActionButton micBtn = null;

    SpeechRecognizer speechRecognizer;
    int targetTemp = 0;
    int currentTemp = 18;
    final int RecordAudioRequestCode = 200;

    private MqttAndroidClient mqttClient;
    private static final String MQTT_BROKER = "tcp://test.mosquitto.org:1883";
    private static final String MQTT_BASE_TOPIC = "su-dsv/iot22/6-5/";
    private static final String[] MQTT_TOPICS_TO_SUBSCRIBE = {"actuators/1/status", "actuators/2/status", "temperature", "temperature-setpoint/status"};

    private CompoundButton.OnCheckedChangeListener switchActivator1Listener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            MqttMessage message = new MqttMessage((isChecked ? "1" : "0").getBytes());
            try {
                mqttClient.publish(MQTT_BASE_TOPIC + "actuators/1", message);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener switchActivator2Listener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            MqttMessage message = new MqttMessage((isChecked ? "1" : "0").getBytes());
            try {
                mqttClient.publish(MQTT_BASE_TOPIC + "actuators/2", message);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    };

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
        switchActivator1 = (Switch) findViewById(R.id.activatorBtn1);
        switchActivator2 = (Switch) findViewById(R.id.activatorBtn2);
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
                updateTempUI(true);
                MqttMessage message = new MqttMessage(Integer.toString(targetTemp).getBytes());
                mqttClient.publish(MQTT_BASE_TOPIC + "temperature-setpoint", message);
                inputUpdateTemp.setText("");
                inputUpdateTemp.clearFocus();
            }
            catch (Exception e){}
        });

        switchActivator1.setOnCheckedChangeListener(switchActivator1Listener);
        switchActivator2.setOnCheckedChangeListener(switchActivator2Listener);

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

        mqttConnect();

        // MQTT setup
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println((reconnect ? "Reconnected" :" Connected") + " to : " + serverURI);
                for (String topic: MQTT_TOPICS_TO_SUBSCRIBE) {
                    mqttSubscribe(MQTT_BASE_TOPIC + topic);
                }
            }
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("The Connection was lost.");
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String newMessage = new String(message.getPayload());
                System.out.println("Incoming message: " + newMessage);

                if (topic.equals(MQTT_BASE_TOPIC + "temperature")) {
                    currentTemp = Math.round(Float.parseFloat(new String(message.getPayload())));
                    updateTempUI(false);
                } else if (topic.equals(MQTT_BASE_TOPIC + "temperature-setpoint/status")) {
                    targetTemp = Math.round(Float.parseFloat(new String(message.getPayload())));
                    updateTempUI(false);
                } else if (topic.equals(MQTT_BASE_TOPIC + "actuators/1/status")) {
                    switchActivator1.setOnCheckedChangeListener(null);
                    switchActivator1.setChecked((new String(message.getPayload())).equals("1"));
                    switchActivator1.setOnCheckedChangeListener(switchActivator1Listener);
                } else if (topic.equals(MQTT_BASE_TOPIC + "actuators/2/status")) {
                    switchActivator2.setOnCheckedChangeListener(null);
                    switchActivator2.setChecked((new String(message.getPayload())).equals("1"));
                    switchActivator2.setOnCheckedChangeListener(switchActivator2Listener);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
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

    private void mqttConnect(){
        String clientId = MqttClient.generateClientId();
        mqttClient = new MqttAndroidClient(this.getApplicationContext(), MQTT_BROKER, clientId);
        try {
            IMqttToken token = mqttClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    System.out.println("Success. Connected to " + MQTT_BROKER);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    System.out.println("Oh no! Failed to connect to " + MQTT_BROKER);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void mqttSubscribe(String topicToSubscribe) {
        final String topic = topicToSubscribe;
        int qos = 1;
        try {
            IMqttToken subToken = mqttClient.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Subscription successful to topic: " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("Failed to subscribe to topic: " + topic);
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    protected void updateTempUI(boolean showSnackbar){
        txtCurrentTemp.setText(currentTemp + "°C");
        txtTargetTemp.setText(targetTemp + "°C");
        int tempLeft = targetTemp - currentTemp;
        subtxtTargetTemp.setText(tempLeft + "°C left");
        int percentageLeft = 100 - (targetTemp * 100) / currentTemp;
        subtxtCurrentTemp.setText(percentageLeft + "% of target");

        if (!showSnackbar) {
            return;
        }

        // Snackbar section
        final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), "Updated Temperature", Snackbar.LENGTH_LONG);
        snackBar.setTextColor(Color.WHITE);
        snackBar.setAction("OK", v -> {
            snackBar.dismiss();
        });
        snackBar.show();
    }
}