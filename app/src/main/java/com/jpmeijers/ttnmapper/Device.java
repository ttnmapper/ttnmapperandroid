package com.jpmeijers.ttnmapper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

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
    String nodeAddress = myPrefs.getString("nodeAddress", "03FFEEBB");

    EditText editText = (EditText) findViewById(R.id.editText_address);
    editText.setText(nodeAddress);
  }

  public void onStartClick(View view)
  {

    EditText editText = (EditText) findViewById(R.id.editText_address);
    String nodeAddress = editText.getText().toString();

    SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
    SharedPreferences.Editor prefsEditor = myPrefs.edit();
    prefsEditor.putString("nodeAddress", nodeAddress);
    prefsEditor.apply();

    try
    {
      //test validity of address
      long addressLong = Long.parseLong(nodeAddress, 16);

      Intent intent = new Intent(this, MapsActivity.class);
      intent.putExtra("addressLong", addressLong);
      intent.putExtra("address", nodeAddress);
      intent.putExtra("createFile", createFile);
      intent.putExtra("logToServer", logToServer);
      startActivity(intent);
      //finish();
    } catch (Exception e)
    {
      Toast.makeText(getApplicationContext(), "Invalid device address", Toast.LENGTH_LONG).show();
    }


  }
}
