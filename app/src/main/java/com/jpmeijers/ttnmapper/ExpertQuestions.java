package com.jpmeijers.ttnmapper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class ExpertQuestions extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_questions);

        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);

        if (myPrefs.getString("backend", "staging").equals("croft")) {
            EditText topic = (EditText) findViewById(R.id.editTextTopic);
            topic.setText(myPrefs.getString("mqtttopiccroft", "nodes/" + myPrefs.getString("nodeaddress", "03FFEEBB") + "/packets"));
        } else {
            EditText topic = (EditText) findViewById(R.id.editTextTopic);
            topic.setText(myPrefs.getString("mqtttopicstaging", "+/devices/#"));
        }

        CheckBox lognegative = (CheckBox) findViewById(R.id.checkBoxNegative);
        lognegative.setChecked(myPrefs.getBoolean("lognegative", false));

        int period = myPrefs.getInt("packetperiod", 10);
        EditText periodText = (EditText) findViewById(R.id.editTextPeriod);
        periodText.setText(period + "");

        int losses = myPrefs.getInt("losses", 6);
        EditText lossesText = (EditText) findViewById(R.id.editTextLosses);
        lossesText.setText(losses + "");


        if (!myPrefs.getBoolean("expertmode", false)) {
            startLogging();
        }
    }

    public void onStartClick(View view) {
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();

        if (myPrefs.getString("backend", "staging").equals("croft")) {
            EditText topic = (EditText) findViewById(R.id.editTextTopic);
            prefsEditor.putString("mqtttopiccroft", topic.getText().toString());
        } else {
            EditText topic = (EditText) findViewById(R.id.editTextTopic);
            prefsEditor.putString("mqtttopicstaging", topic.getText().toString());
        }

        CheckBox lognegative = (CheckBox) findViewById(R.id.checkBoxNegative);
        prefsEditor.putBoolean("lognegative", lognegative.isChecked());

        EditText periodText = (EditText) findViewById(R.id.editTextPeriod);
        try {
            prefsEditor.putInt("packetperiod", Integer.parseInt(periodText.getText().toString()));
        } catch (Exception e) {
            //nothing - use default value
        }

        EditText lossesText = (EditText) findViewById(R.id.editTextLosses);
        try {
            prefsEditor.putInt("losses", Integer.parseInt(lossesText.getText().toString()));
        } catch (Exception e) {
            //nothing - use default value
        }

        prefsEditor.apply();

        startLogging();

    }

    public void startLogging() {
        //remove back button history
        //kill this activity too
        Intent intent = new Intent(this, CheckPermissions.class);
        startActivity(intent);
    }
}
