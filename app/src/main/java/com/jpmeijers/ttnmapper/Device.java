package com.jpmeijers.ttnmapper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class Device extends AppCompatActivity
{
  private boolean createFile = false;
  private boolean logToServer = false;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_device);

    CheckBox checkBoxFile = (CheckBox) findViewById(R.id.checkBoxFile);
    checkBoxFile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                                            {
                                              @Override
                                              public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                                              {
                                                createFile = isChecked;
                                              }
                                            }
    );
    createFile = checkBoxFile.isChecked();

    CheckBox checkBoxServer = (CheckBox) findViewById(R.id.checkBoxServer);
    checkBoxServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                                              {
                                                @Override
                                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                                                {
                                                  logToServer = isChecked;
                                                }
                                              }
    );
    logToServer = checkBoxServer.isChecked();

    SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);

    String value = myPrefs.getString("server", "tcp://staging.thethingsnetwork.org:1883");
    EditText editText = (EditText) findViewById(R.id.editTextServer);
    editText.setText(value);

    value = myPrefs.getString("user", "");
    editText = (EditText) findViewById(R.id.editTextUser);
    editText.setText(value);

    value = myPrefs.getString("password", "");
    editText = (EditText) findViewById(R.id.editTextPassword);
    editText.setText(value);

    value = myPrefs.getString("topic", "+/devices/#");
    editText = (EditText) findViewById(R.id.editTextTopic);
    editText.setText(value);
  }

  public void onStartClick(View view)
  {

    EditText editText = (EditText) findViewById(R.id.editTextServer);
    String server = editText.getText().toString();

    editText = (EditText) findViewById(R.id.editTextUser);
    String user = editText.getText().toString().toUpperCase();

    editText = (EditText) findViewById(R.id.editTextPassword);
    String password = editText.getText().toString();

    editText = (EditText) findViewById(R.id.editTextTopic);
    String topic = editText.getText().toString(); //user+"/devices/+/up";//


    SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
    SharedPreferences.Editor prefsEditor = myPrefs.edit();
    prefsEditor.putString("server", server);
    prefsEditor.putString("user", user);
    prefsEditor.putString("password", password);
    prefsEditor.putString("topic", topic);
    prefsEditor.apply();

    Intent intent = new Intent(this, MapsActivity.class);
    intent.putExtra("server", server);
    intent.putExtra("user", user);
    intent.putExtra("password", password);
    intent.putExtra("topic", topic);
    intent.putExtra("createFile", createFile);
    intent.putExtra("logToServer", logToServer);
    startActivity(intent);

  }
}
