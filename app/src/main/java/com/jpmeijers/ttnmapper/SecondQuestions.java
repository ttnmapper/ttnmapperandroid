package com.jpmeijers.ttnmapper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SecondQuestions extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_questions);

        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);

        //set type of logging from settings
        boolean value = myPrefs.getBoolean("savefile", true);
        CheckBox checkboxFileSave = (CheckBox) findViewById(R.id.checkBoxSaveFile);
        EditText editTextFilename = (EditText) findViewById(R.id.editTextFilename);
        checkboxFileSave.setChecked(value);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        String filename = "ttnmapper_" + sdf.format(new Date()) + ".csv";
        editTextFilename.setText(filename);
        editTextFilename.setEnabled(value);

        // Pre-fill region input
        final EditText regionEditText = (EditText) findViewById(R.id.editTextMQTTregion);
        String mqttregion = myPrefs.getString("mqttregion", "");
        regionEditText.setText(mqttregion);

        // Pre-fill username field
        EditText editText1 = (EditText) findViewById(R.id.editTextMQTTuser);
        String mqttusername = myPrefs.getString("mqttusername", "");
        editText1.setText(mqttusername);

        // Pre-fill password field
        editText1 = (EditText) findViewById(R.id.editTextMQTTpassword);
        String mqttpassword = myPrefs.getString("mqttpassword", "");
        editText1.setText(mqttpassword);

        // Set expert mode settings from preferences
        value = myPrefs.getBoolean("expertmode", false);
        CheckBox checkboxExpert = (CheckBox) findViewById(R.id.checkBoxExpert);
        checkboxExpert.setChecked(value);
        Button startButton = (Button) findViewById(R.id.buttonStart);
        if (value) {
            startButton.setText("Next");
        } else {
            startButton.setText("Start");
        }

        //on click listener for save file
        checkboxFileSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                                        @Override
                                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                            EditText editText = (EditText) findViewById(R.id.editTextFilename);
                                                            editText.setEnabled(isChecked);
                                                        }
                                                    }
        );

        checkboxExpert.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                                      @Override
                                                      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                          Button startButton = (Button) findViewById(R.id.buttonStart);
                                                          if (isChecked) {
                                                              startButton.setText("Next");
                                                          } else {
                                                              startButton.setText("Start");
                                                          }
                                                      }
                                                  }
        );
    }

    public void onStartClick(View view) {

        CheckBox checkboxFileSave = (CheckBox) findViewById(R.id.checkBoxSaveFile);
        boolean saveFile = checkboxFileSave.isChecked();

        EditText editTextFilename = (EditText) findViewById(R.id.editTextFilename);
        String fileName = editTextFilename.getText().toString();

        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean("savefile", saveFile);
        prefsEditor.putString("filename", fileName);

        final EditText editTextRegion = (EditText) findViewById(R.id.editTextMQTTregion);
        final String mqttregion = editTextRegion.getText().toString();
        prefsEditor.putString("mqttregion", mqttregion);

        EditText editText1 = (EditText) findViewById(R.id.editTextMQTTuser);
        String mqttusername = editText1.getText().toString();
        prefsEditor.putString("mqttusername", mqttusername);

        editText1 = (EditText) findViewById(R.id.editTextMQTTpassword);
        String mqttpassword = editText1.getText().toString();
        prefsEditor.putString("mqttpassword", mqttpassword);

        CheckBox checkboxExpert = (CheckBox) findViewById(R.id.checkBoxExpert);
        boolean expertMode = checkboxExpert.isChecked();
        prefsEditor.putBoolean("expertmode", expertMode);

        prefsEditor.apply();

        Intent intent = new Intent(this, ExpertQuestions.class);
        startActivity(intent);
        finish();

    }
}
