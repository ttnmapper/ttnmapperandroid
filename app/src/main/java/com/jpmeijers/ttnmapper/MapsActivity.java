package com.jpmeijers.ttnmapper;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends AppCompatActivity /*extends FragmentActivity*/
    implements OnMapReadyCallback, MqttCallback, ActivityCompat.OnRequestPermissionsResultCallback
{
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  public static final String TAG = "TTNMapsActivity";
  /**
   * Id to identify a camera permission request.
   */
  private static final int REQUEST_LOCATION_PERMISION = 0;
  private static final int REQUEST_FILE_PERMISION = 1;
  //  static TTNMqttClient mTTNMqttClient;
  MqttAndroidClient myMQTTclient;
  OkHttpClient client = new OkHttpClient();
  private GoogleMap mMap;
  private String nodeAddress;
  private boolean createFile;
  private boolean logToServer;
  private String loggingFilename;
  private String mqttTopic = "nodes/02031701/packets";

  Call postToServer(String url, String json, Callback callback) throws IOException
  {
    RequestBody body = RequestBody.create(JSON, json);
    Request request = new Request.Builder()
        .url(url)
        .post(body)
        .build();
    Call call = client.newCall(request);
    call.enqueue(callback);
    return call;
  }

  @Override
  public void onBackPressed()
  {
    //warn and ask if we should stop
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

    // set title
    alertDialogBuilder.setTitle("Exit");

    // set dialog message
    alertDialogBuilder
        .setMessage("You touched the back button. Do you want to stop logging?")
        .setCancelable(false)
        .setPositiveButton("Stop logging", new DialogInterface.OnClickListener()
        {
          public void onClick(DialogInterface dialog, int id)
          {
            // if this button is clicked, close
            // current activity
            //            mTTNMqttClient.disconnect();
            MapsActivity.this.finish();
          }
        })
        .setNegativeButton("Continue logging", new DialogInterface.OnClickListener()
        {
          public void onClick(DialogInterface dialog, int id)
          {
            // if this button is clicked, just close
            // the dialog box and do nothing
            dialog.cancel();
          }
        });

    // create alert dialog
    AlertDialog alertDialog = alertDialogBuilder.create();

    // show it
    alertDialog.show();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);


    setContentView(R.layout.activity_maps);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
    mapFragment.setRetainInstance(true);

    Intent intent = getIntent();
    nodeAddress = intent.getStringExtra("address");
    createFile = intent.getBooleanExtra("createFile", false);
    logToServer = intent.getBooleanExtra("logToServer", false);

    mqttTopic = "nodes/" + nodeAddress + "/packets";

    Toast.makeText(getApplicationContext(), "Address: " + nodeAddress + ", " + createFile + ", " + logToServer, Toast.LENGTH_SHORT).show();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    loggingFilename = Environment.getExternalStorageDirectory() + "/" + sdf.format(new Date()) + ".csv";
    System.out.println("Writing to file " + loggingFilename);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    mqtt_connect();

    // Check permissions
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    {
      requestPositionPermission();
    }
    if (createFile)
    {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
      {
        requestFilePermission();
      }
    }
  }

  private void requestFilePermission()
  {
    Log.i(TAG, "FILE permission has NOT been granted. Requesting permission.");

    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
    {
      Log.i(TAG, "Displaying file permission rationale to provide additional context.");
      Snackbar.make(findViewById(R.id.map), R.string.permission_file_rationale,
          Snackbar.LENGTH_INDEFINITE)
          .setAction(R.string.ok, new View.OnClickListener()
          {
            @Override
            public void onClick(View view)
            {
              ActivityCompat.requestPermissions(MapsActivity.this,
                  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                  REQUEST_FILE_PERMISION);
            }
          })
          .show();
    } else
    {
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
          REQUEST_FILE_PERMISION);
    }
  }

  @Override
  public void onResume()
  {
    super.onResume();  // Always call the superclass method first

    //    mqtt_unsubscribe();
    //    mqtt_connect();
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
    mqtt_unsubscribe();
  }


  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  @Override
  public void onMapReady(GoogleMap googleMap)
  {
    mMap = googleMap;

    // Zoom in the Google Map
    mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

    // set map type
    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

    // Enable MyLocation Layer of Google Map
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    {
      requestPositionPermission();
      return;
    }
    mMap.setMyLocationEnabled(true);

    // Show the current location in Google Map
    mMap.moveCamera(CameraUpdateFactory.newLatLng(getCurrentLatLng()));

    // Zoom in the Google Map
    mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
  }

  private void requestPositionPermission()
  {
    Log.i(TAG, "LOCATION permission has NOT been granted. Requesting permission.");

    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
        || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
    {
      Log.i(TAG, "Displaying location permission rationale to provide additional context.");
      Snackbar.make(findViewById(R.id.map), R.string.permission_location_rationale,
          Snackbar.LENGTH_INDEFINITE)
          .setAction(R.string.ok, new View.OnClickListener()
          {
            @Override
            public void onClick(View view)
            {
              ActivityCompat.requestPermissions(MapsActivity.this,
                  new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                  REQUEST_LOCATION_PERMISION);
            }
          })
          .show();
    } else
    {
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
          REQUEST_LOCATION_PERMISION);
    }
  }

  /**
   * Callback received when a permissions request has been completed.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
  {

    if (requestCode == REQUEST_LOCATION_PERMISION)
    {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)
      {
        Log.i(TAG, "LOCATION permission has now been granted.");
        Snackbar.make(findViewById(R.id.map), R.string.permission_available_location,
            Snackbar.LENGTH_SHORT).show();
      } else
      {
        Log.i(TAG, "LOCATION permission was NOT granted.");
        Snackbar.make(findViewById(R.id.map), R.string.permissions_not_location,
            Snackbar.LENGTH_SHORT).show();
      }
    }
    if (requestCode == REQUEST_FILE_PERMISION)
    {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)
      {
        Log.i(TAG, "FILE permission has now been granted.");
        Snackbar.make(findViewById(R.id.map), R.string.permission_available_file,
            Snackbar.LENGTH_SHORT).show();
      } else
      {
        Log.i(TAG, "FILE permission was NOT granted.");
        Snackbar.make(findViewById(R.id.map), R.string.permissions_not_file,
            Snackbar.LENGTH_SHORT).show();
      }
    } else
    {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private LatLng getCurrentLatLng()
  {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    {
      return new LatLng(0, 0);
    } else
    {
      // Get Current Location
      Location myLocation = getCurrentLocation();

      double latitude = 0;
      double longitude = 0;

      try
      {
        // Get latitude of the current location
        latitude = myLocation.getLatitude();

        // Get longitude of the current location
        longitude = myLocation.getLongitude();
      } catch (Exception e)
      {
      }

      // Create a LatLng object for the current location
      return new LatLng(latitude, longitude);
    }
  }

  private Location getCurrentLocation()
  {
    // Enable MyLocation Layer of Google Map
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    {
      return null;
    }
    // Get LocationManager object from System Service LOCATION_SERVICE
    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    // Create a criteria object to retrieve provider
    Criteria criteria = new Criteria();

    // Get the name of the best provider
    String provider = locationManager.getBestProvider(criteria, true);

    // Get Current Location
    return locationManager.getLastKnownLocation(provider);
  }

  void mqtt_connect()
  {
    String clientId = MqttClient.generateClientId();
    myMQTTclient =
        new MqttAndroidClient(this.getApplicationContext(), "tcp://croft.thethings.girovito.nl:1883",
            clientId);

    try
    {
      IMqttToken token = myMQTTclient.connect();
      token.setActionCallback(new IMqttActionListener()
      {
        @Override
        public void onSuccess(IMqttToken asyncActionToken)
        {
          // We are connected
          Log.d("MQTT", "connect onSuccess");

          mqtt_subscribe();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception)
        {
          // Something went wrong e.g. connection timeout or firewall problems
          Log.d("MQTT", "connect onFailure");

        }
      });
    } catch (MqttException e)
    {
      e.printStackTrace();
    }

  }

  void mqtt_subscribe()
  {
    try
    {
      myMQTTclient.setCallback(this);

      IMqttToken subToken = myMQTTclient.subscribe(mqttTopic, 1);
      subToken.setActionCallback(new IMqttActionListener()
      {
        @Override
        public void onSuccess(IMqttToken asyncActionToken)
        {
          // The message was published
          Log.d("MQTT", "subscribe onSuccess");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken,
                              Throwable exception)
        {
          // The subscription could not be performed, maybe the user was not
          // authorized to subscribe on the specified topic e.g. using wildcards

          Log.d("MQTT", "subscribe onFailure");
        }
      });
    } catch (MqttException e)
    {
      e.printStackTrace();
    }
  }


  void mqtt_unsubscribe()
  {
    //unsubscribe
    try
    {
      IMqttToken unsubToken = myMQTTclient.unsubscribe(mqttTopic);
      unsubToken.setActionCallback(new IMqttActionListener()
      {
        @Override
        public void onSuccess(IMqttToken asyncActionToken)
        {
          // The subscription could successfully be removed from the client
          Log.d("MQTT", "unsub onSuccess");
          mqtt_disconnect();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken,
                              Throwable exception)
        {
          // some error occurred, this is very unlikely as even if the client
          // did not had a subscription to the topic the unsubscribe action
          // will be successfully
          Log.d("MQTT", "unsub onFailure");
        }
      });
    } catch (MqttException e)
    {
      e.printStackTrace();
    }
  }

  void mqtt_disconnect()
  {
    //disconnect
    try
    {
      IMqttToken disconToken = myMQTTclient.disconnect();
      disconToken.setActionCallback(new IMqttActionListener()
      {
        @Override
        public void onSuccess(IMqttToken asyncActionToken)
        {
          // we are now successfully disconnected
          Log.d("MQTT", "disconnect onSuccess");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken,
                              Throwable exception)
        {
          // something went wrong, but probably we are disconnected anyway
          Log.d("MQTT", "unsub onFailure");
        }
      });
    } catch (MqttException e)
    {
      e.printStackTrace();
    }
  }

  @Override
  public void connectionLost(Throwable cause)
  {
    Log.d("MQTT", "connection lost");
  }

  @Override
  public void messageArrived(String topic, MqttMessage message)
  {
    Log.d("MQTT", "message arrived");
    System.out.println("Message arrived: " + message.toString());
    process_packet(message.toString());
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token)
  {
    Log.d("MQTT", "delivery complete");
  }


  void process_packet(String jsonData)
  {

    class AddMarker implements Runnable
    {

      private String jsonData;

      public AddMarker(String jsonData)
      {
        this.jsonData = jsonData;
      }

      @Override
      public void run()
      {
        String gatewayAddr = "null";
        String nodeAddr = "null";
        String time = "0";
        double freq = 0;
        String dataRate = "null";
        double rssi = 0;
        double snr = 0;

        try
        {
          JSONObject received_data = new JSONObject(jsonData);

          gatewayAddr = received_data.getString("gatewayEui");
          nodeAddr = received_data.getString("nodeEui");
          time = received_data.getString("time");
          freq = received_data.getDouble("frequency");
          dataRate = received_data.getString("dataRate");
          rssi = received_data.getDouble("rssi");
          snr = received_data.getDouble("snr");
        } catch (JSONException e)
        {
          e.printStackTrace();
        }

        Location location = getCurrentLocation();

        if (mMap != null)
        {
          CircleOptions options = new CircleOptions()
              .center(new LatLng(location.getLatitude(), location.getLongitude()))
              .radius(15)
              .strokeColor(0x00000000);

          if (rssi == 0)
          {
            options.fillColor(0x7f000000);
          } else if (rssi < -120)
          {
            options.fillColor(0x7f0000ff);
          } else if (rssi < -110)
          {
            options.fillColor(0x7f00ffff);
          } else if (rssi < -100)
          {
            options.fillColor(0x7f00ff00);
          } else if (rssi < -90)
          {
            options.fillColor(0x7fffff00);
          } else if (rssi < -80)
          {
            options.fillColor(0x7fff7f00);
          } else
          {
            options.fillColor(0x7fff0000);
          }

          mMap.addCircle(options);
          mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }

        if (createFile)
        {
          try
          {

          /*
            02031701

            time, nodeaddr, gwaddr, datarate, snr, rssi, freq, lat, lon
           */
            String data =
                time + "," + nodeAddr + "," + gatewayAddr + "," +
                    dataRate + "," + snr + "," + rssi + "," +
                    freq + "," + location.getLatitude() + "," + location.getLongitude() + "\n";

            FileWriter writer = new FileWriter(loggingFilename, true);
            // Writes the content to the file
            writer.write(data);
            writer.flush();
            writer.close();
            System.out.println("File written");
          } catch (IOException e)
          {
            Log.e("Exception", "File write failed: " + e.toString());
          }
        }

        if (logToServer)
        {
          HttpURLConnection connection = null;
          try
          {
            System.out.println("Datarate: \"" + dataRate + "\"");
            //object for storing Json
            JSONObject data = new JSONObject();
            data.put("time", time);
            data.put("nodeaddr", nodeAddr);
            data.put("gwaddr", gatewayAddr);
            data.put("datarate", dataRate);
            data.put("snr", snr);
            data.put("rssi", rssi);
            data.put("freq", freq);
            data.put("lat", location.getLatitude());
            data.put("lon", location.getLongitude());
            if (location.hasAltitude())
            {
              data.put("alt", location.getAltitude());
            }
            if (location.hasAccuracy())
            {
              data.put("accuracy", location.getAccuracy());
            }
            data.put("provider", location.getProvider());

            String dataString = data.toString();
            System.out.println(dataString);
            String url = "http://jpmeijers.com/ttnmapper/api/upload.php";

            postToServer(url, dataString, new Callback()
            {
              @Override
              public void onFailure(Call call, IOException e)
              {
                runOnUiThread(new Runnable()
                {
                  public void run()
                  {
                    Toast.makeText(getApplicationContext(), "error uploading", Toast.LENGTH_SHORT).show();
                  }
                });
                e.printStackTrace();
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException
              {
                if (response.isSuccessful())
                {
                  System.out.println("HTTP response: " + response.body().string());
                  // Do what you want to do with the response.
                } else
                {
                  // Request not successful
                  runOnUiThread(new Runnable()
                  {
                    public void run()
                    {
                      Toast.makeText(getApplicationContext(), "server error", Toast.LENGTH_SHORT).show();
                    }
                  });
                }
              }
            });
          } catch (JSONException e)
          {
            e.printStackTrace();
          } catch (IOException e)
          {
            e.printStackTrace();
          }
        }
      }
    } //end of AddMarker inner class

    // start the inner class runnable
    Runnable addMarker = new AddMarker(jsonData);
    runOnUiThread(addMarker);

  }
}
