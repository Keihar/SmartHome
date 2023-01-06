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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView txtCurrentTemp = null;
    TextView subtxtCurrentTemp = null;
    TextView txtTargetTemp = null;
    TextView subtxtTargetTemp = null;
    TextInputEditText inputUpdateTemp = null;
    Button btnUpdateTemp = null;
    Switch activatorSwitch = null;
    Switch lightSwitch = null;
    FloatingActionButton micBtn = null;

    SpeechRecognizer speechRecognizer;
    boolean isActuatorSwitchChecked = false;
    boolean isLightSwitchChecked = false;
    int targetTemp = 20;
    int currentTemp = 18;
    final int RecordAudioRequestCode = 200;

    PopupWindow popupWindow;

    private MqttAndroidClient mqttClient;
    private static final String MQTT_BROKER = "tcp://test.mosquitto.org:1883";
    private static final String MQTT_BASE_TOPIC = "su-dsv/iot22/6-5/";
    private static final String[] MQTT_TOPICS_TO_SUBSCRIBE = {"actuators/1/status", "actuators/2/status", "temperature", "temperature-setpoint/status"};

    private CompoundButton.OnCheckedChangeListener activatorSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            MqttMessage message = new MqttMessage((isChecked ? "1" : "0").getBytes());
            mqttClient.publish(MQTT_BASE_TOPIC + "actuators/1", message);
        }
    };

    private CompoundButton.OnCheckedChangeListener lightSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            MqttMessage message = new MqttMessage((isChecked ? "1" : "0").getBytes());
            mqttClient.publish(MQTT_BASE_TOPIC + "actuators/2", message);
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
        activatorSwitch = (Switch) findViewById(R.id.activatorBtn1);
        lightSwitch = (Switch) findViewById(R.id.activatorBtn2);
        micBtn = (FloatingActionButton) findViewById(R.id.micFab);

        // Presses submit btn when clicking "done" on keyboard
        inputUpdateTemp.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                    (actionId == EditorInfo.IME_ACTION_DONE))
                btnUpdateTemp.performClick();
            return false;
        });

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.micpopup, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        popupWindow = new PopupWindow(popupView, width, height, false);

        btnUpdateTemp.setOnClickListener(v -> {
            String text = String.valueOf(inputUpdateTemp.getText());
            try {
                targetTemp = Integer.parseInt(text);
                // Hides Keyboard
                View view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                // Updates UI Elements
                updateTempUI(true);
                MqttMessage message = new MqttMessage(Integer.toString(targetTemp).getBytes());
                mqttClient.publish(MQTT_BASE_TOPIC + "temperature-setpoint", message);
                inputUpdateTemp.setText("");
                inputUpdateTemp.clearFocus();
            } catch (Exception e) {
            }
        });

        activatorSwitch.setOnCheckedChangeListener(activatorSwitchListener);
        lightSwitch.setOnCheckedChangeListener(lightSwitchListener);
        //activatorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> isActuatorSwitchChecked = isChecked);
        //lightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> isLightSwitchChecked = isChecked);

        // Speech Permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
        }

        // Speech Set Up
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());

        // Speech Functions Events
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                System.out.println("ready for speech");
                popupWindow.showAtLocation(findViewById(R.id.micFab), Gravity.CENTER, 0, 0);
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
                popupWindow.dismiss();
            }

            @Override
            public void onResults(Bundle bundle) {
                System.out.println("results ready");
                ArrayList<String> data = bundle.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                processSpeech(data.get(0));
                System.out.println(data.get(0));
                popupWindow.dismiss();
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
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
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
                    activatorSwitch.setOnCheckedChangeListener(null);
                    activatorSwitch.setChecked((new String(message.getPayload())).equals("1"));
                    activatorSwitch.setOnCheckedChangeListener(activatorSwitchListener);
                } else if (topic.equals(MQTT_BASE_TOPIC + "actuators/2/status")) {
                    lightSwitch.setOnCheckedChangeListener(null);
                    lightSwitch.setChecked((new String(message.getPayload())).equals("1"));
                    lightSwitch.setOnCheckedChangeListener(lightSwitchListener);
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

    // Mic Permissions section
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void mqttConnect(){
        String clientId = MqttClient.generateClientId();
        mqttClient = new MqttAndroidClient(this.getApplicationContext(), MQTT_BROKER, clientId, Ack.AUTO_ACK);
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
    }

    private void mqttSubscribe(String topicToSubscribe) {
        final String topic = topicToSubscribe;
        int qos = 1;
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
    }

    protected void updateTempUI(boolean showSnackbar) {
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
        final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content),
                "Updated Temperature to " + targetTemp + "°C", Snackbar.LENGTH_LONG);
        snackBar.setTextColor(Color.WHITE);
        snackBar.setAction("OK", v -> {
            snackBar.dismiss();
        });
        snackBar.show();
    }

    private void processSpeech(String out) {
        String[] wordsArr = out.split(" ");
        List<String> wordsList = Arrays.asList(wordsArr);

        //Turn plugs on/off
        int index = wordsList.indexOf("turn");
        if (index != -1) {
            if (wordsArr.length > index + 1 && (wordsArr[index + 1].equals("on") ||
                    wordsArr[wordsArr.length - 1].equals("on"))) {
                if (wordsList.contains("actuator") || wordsList.contains("plug")) {
                    if (!isActuatorSwitchChecked) {
                        activatorSwitch.performClick();
                        Toast.makeText(this, "Turned the actuator on",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                if (wordsList.contains("light") || wordsList.contains("lights")) {
                    if (!isLightSwitchChecked) {
                        lightSwitch.performClick();
                        Toast.makeText(this, "Turned the lights on",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            if (wordsArr.length > index + 1 && (wordsArr[index + 1].equals("off") || wordsArr[wordsArr.length - 1].equals("off"))) {
                if (wordsList.contains("actuator") || wordsList.contains("plug")) {
                    if (isActuatorSwitchChecked) {
                        activatorSwitch.performClick();
                        Toast.makeText(this, "Turned the actuator off",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                if (wordsList.contains("light") || wordsList.contains("lights")) {
                    if (isLightSwitchChecked) {
                        lightSwitch.performClick();
                        Toast.makeText(this, "Turned the lights off",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        //Temperature
        if (wordsList.contains("temperature")) {
            for (String str : wordsArr) {
                int tempNum = wordToNum(str);
                int strToInt = -1;
                try {
                    strToInt = Integer.parseInt(str);
                    tempNum = strToInt;
                } catch(Exception e){}
                if (str.indexOf('°') != -1) {
                    inputUpdateTemp.setText(str.substring(0, str.length() - 1));
                    btnUpdateTemp.performClick();
                    break;
                } else if (tempNum != -1) {
                    inputUpdateTemp.setText("" + tempNum);
                    btnUpdateTemp.performClick();
                    break;
                }
            }
        }
    }

    private int wordToNum(String input) {
        int result = 0;
        int finalResult = 0;
        List<String> allowedStrings = Arrays.asList
                (
                        "zero", "one", "two", "three", "four", "five", "six", "seven",
                        "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen",
                        "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty",
                        "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
                        "hundred"
                );

        if (input != null && input.length() > 0) {
            input = input.replaceAll("-", " ");
            input = input.toLowerCase().replaceAll(" and", " ");
            String[] splittedParts = input.trim().split("\\s+");

            for (String str : splittedParts) {
                if (!allowedStrings.contains(str)) {
                    return -1;
                }
            }

            for (String str : splittedParts) {
                if (str.equalsIgnoreCase("zero")) {
                    result += 0;
                } else if (str.equalsIgnoreCase("one")) {
                    result += 1;
                } else if (str.equalsIgnoreCase("two")) {
                    result += 2;
                } else if (str.equalsIgnoreCase("three")) {
                    result += 3;
                } else if (str.equalsIgnoreCase("four")) {
                    result += 4;
                } else if (str.equalsIgnoreCase("five")) {
                    result += 5;
                } else if (str.equalsIgnoreCase("six")) {
                    result += 6;
                } else if (str.equalsIgnoreCase("seven")) {
                    result += 7;
                } else if (str.equalsIgnoreCase("eight")) {
                    result += 8;
                } else if (str.equalsIgnoreCase("nine")) {
                    result += 9;
                } else if (str.equalsIgnoreCase("ten")) {
                    result += 10;
                } else if (str.equalsIgnoreCase("eleven")) {
                    result += 11;
                } else if (str.equalsIgnoreCase("twelve")) {
                    result += 12;
                } else if (str.equalsIgnoreCase("thirteen")) {
                    result += 13;
                } else if (str.equalsIgnoreCase("fourteen")) {
                    result += 14;
                } else if (str.equalsIgnoreCase("fifteen")) {
                    result += 15;
                } else if (str.equalsIgnoreCase("sixteen")) {
                    result += 16;
                } else if (str.equalsIgnoreCase("seventeen")) {
                    result += 17;
                } else if (str.equalsIgnoreCase("eighteen")) {
                    result += 18;
                } else if (str.equalsIgnoreCase("nineteen")) {
                    result += 19;
                } else if (str.equalsIgnoreCase("twenty")) {
                    result += 20;
                } else if (str.equalsIgnoreCase("thirty")) {
                    result += 30;
                } else if (str.equalsIgnoreCase("forty")) {
                    result += 40;
                } else if (str.equalsIgnoreCase("fifty")) {
                    result += 50;
                } else if (str.equalsIgnoreCase("sixty")) {
                    result += 60;
                } else if (str.equalsIgnoreCase("seventy")) {
                    result += 70;
                } else if (str.equalsIgnoreCase("eighty")) {
                    result += 80;
                } else if (str.equalsIgnoreCase("ninety")) {
                    result += 90;
                } else if (str.equalsIgnoreCase("hundred")) {
                    result *= 100;
                }
            }

            finalResult += result;
        }
        return finalResult;
    }
}