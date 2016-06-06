package com.jpmeijers.ttnmapper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FirstQuestions extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_questions);

        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);

        //set type of logging from settings
        String value = myPrefs.getString("logging_type", "global");
        if (value.equalsIgnoreCase("experiment")) {
            RadioButton button = (RadioButton) findViewById(R.id.radioButtonExperiment);
            button.setChecked(true);
        } else if (value.equalsIgnoreCase("global")) {
            RadioButton button = (RadioButton) findViewById(R.id.radioButtonGlobal);
            button.setChecked(true);
        } else if (value.equalsIgnoreCase("local")) {
            RadioButton button = (RadioButton) findViewById(R.id.radioButtonLocal);
            button.setChecked(true);
        }

        //set backend from settings
        value = myPrefs.getString("backend", "staging");
        if (value.equalsIgnoreCase("croft")) {
            RadioButton button = (RadioButton) findViewById(R.id.radioButtonCroft);
            button.setChecked(true);
        } else if (value.equalsIgnoreCase("staging")) {
            RadioButton button = (RadioButton) findViewById(R.id.radioButtonStaging);
            button.setChecked(true);
        }

        //set experiment default name
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        value = "experiment " + sdf.format(new Date());
        EditText text = (EditText) findViewById(R.id.editTextExperiment);
        text.setText(value);

        RadioButton button = (RadioButton) findViewById(R.id.radioButtonExperiment);
        text.setEnabled(button.isChecked());

        RadioGroup typeGroup = (RadioGroup) findViewById(R.id.radioGroupType);

        typeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton button = (RadioButton) findViewById(R.id.radioButtonExperiment);
                EditText text = (EditText) findViewById(R.id.editTextExperiment);
                text.setEnabled(button.isChecked());
                Log.d("Question1", "Type changed");
            }
        });
    }

    public void onNextClick(View view) {
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();

        RadioButton button;
        button = (RadioButton) findViewById(R.id.radioButtonExperiment);
        if (button.isChecked()) {
            prefsEditor.putString("logging_type", "experiment");
        }
        button = (RadioButton) findViewById(R.id.radioButtonGlobal);
        if (button.isChecked()) {
            prefsEditor.putString("logging_type", "global");
        }
        button = (RadioButton) findViewById(R.id.radioButtonLocal);
        if (button.isChecked()) {
            prefsEditor.putString("logging_type", "local");
        }

        EditText text = (EditText) findViewById(R.id.editTextExperiment);
        prefsEditor.putString("experimentname", text.getText().toString());

        button = (RadioButton) findViewById(R.id.radioButtonCroft);
        if (button.isChecked()) {
            prefsEditor.putString("backend", "croft");
        }
        button = (RadioButton) findViewById(R.id.radioButtonStaging);
        if (button.isChecked()) {
            prefsEditor.putString("backend", "staging");
        }

        prefsEditor.apply();

        Intent intent = new Intent(this, SecondQuestions.class);
        startActivity(intent);
    }
}
