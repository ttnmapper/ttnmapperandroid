package com.jpmeijers.ttnmapper;

/*
Disable auto centre when dragging the map:
http://stackoverflow.com/questions/13702117/how-can-i-handle-map-move-end-using-google-maps-for-android-v2
 */

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends AppCompatActivity /*extends FragmentActivity*/
        implements OnMapReadyCallback, MqttCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnCameraChangeListener {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String TAG = "TTNMapsActivity";
    /**
     * Id to identify a camera permission request.
     */
    private static final int REQUEST_LOCATION_PERMISSION = 0;
    private static final int REQUEST_FILE_PERMISSION = 1;
    BitmapDescriptor circleBlack = null;
    BitmapDescriptor circleBlue = null;
    BitmapDescriptor circleCyan = null;
    BitmapDescriptor circleGreen = null;
    BitmapDescriptor circleYellow = null;
    BitmapDescriptor circleOrange = null;
    BitmapDescriptor circleRed = null;
    Bitmap bmBlack;
    Bitmap bmBlue;
    Bitmap bmCyan;
    Bitmap bmGreen;
    Bitmap bmYellow;
    Bitmap bmOrange;
    Bitmap bmRed;

    //keep a list of all the markers
    List<Marker> markers = new ArrayList<Marker>();

    private int mqtt_retry_count = 0;
    private GoogleMap mMap;

    private boolean createFile;
    private String logType;
    private String loggingFilename;
    private String mqttServer;
    private String mqttUser;
    private String mqttPassword = "";
    private String mqttTopic;
    private String backend;
    private String nodeaddr; //only for croft backend
    private String experimentName;
    private LocationManager mLocationManager;
    private Location currentLocation;
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            /*
            autocentre the map if:
            the setting is on,
            and we moved more than 10 meters
            or we have not centreed the map at our location
             */
            SharedPreferences myPrefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
            if (myPrefs.getBoolean("autocentremap", true)) {
                Location mapCentreLocation = new Location("");
                mapCentreLocation.setLatitude(mMap.getCameraPosition().target.latitude);
                mapCentreLocation.setLongitude(mMap.getCameraPosition().target.longitude);
                if (currentLocation == null || location.distanceTo(mapCentreLocation) > 10) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                }
            }

            if (myPrefs.getBoolean("autozoommap", true)) {
                autoScaleMap();
            }

            //save
            currentLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
    private boolean mMapIsTouched;

    Call postToServer(String url, String json, Callback callback) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        MyApp myApp = (MyApp) getApplication();
        Call call = myApp.getHttpClient().newCall(request);
        call.enqueue(callback);
        return call;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "Touch event");
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMapIsTouched = true;
                break;
            case MotionEvent.ACTION_UP:
                mMapIsTouched = false;
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        //warn and ask if we should stop
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle("Exit");

        // set dialog message
        alertDialogBuilder
                .setMessage("You touched the back button. Do you want to stop logging?")
                .setCancelable(false)
                .setPositiveButton("Stop logging", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        exitApp();
                    }
                })
                .setNegativeButton("Continue logging", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
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

    void exitApp() {
        // if this button is clicked, close
        // current activity
        MyApp myApp = (MyApp) getApplication();

        myApp.setClosingDown(true);

        //MQTT unsubscribe
        try {
            myApp.getMyMQTTclient().disconnect();
//                            myApp.getMyMQTTclient().disconnectForcibly(1);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        //GPS position updates unsubscribe
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
                Log.d(TAG, "GPS updates removed");
            } catch (Exception e) {
                Log.d(TAG, "Can not remove gps updates");
            }
        }

        //force the creation of a new MQTT object after we manually stop the logging.
        //This is only needed to be able to connect to a different backend during the same session.
        myApp.setMQTTclient(null);

        //end this activity
        MapsActivity.this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_options_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        menu.findItem(R.id.menu_action_auto_centre).setChecked(myPrefs.getBoolean("autocentremap", true));
        menu.findItem(R.id.menu_action_auto_zoom).setChecked(myPrefs.getBoolean("autozoommap", true));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        switch (item.getItemId()) {
            case R.id.menu_action_auto_centre:
                if (item.isChecked()) {
                    Log.d(TAG, "Disabling auto centre");
                } else {
                    Log.d(TAG, "Enabling auto centre");
                }
                item.setChecked(!item.isChecked());
                prefsEditor.putBoolean("autocentremap", item.isChecked());
                prefsEditor.apply();
                return true;
            case R.id.menu_action_auto_zoom:
                if (item.isChecked()) {
                    Log.d(TAG, "Disabling auto scale");
                } else {
                    Log.d(TAG, "Enabling auto scale");
                }
                item.setChecked(!item.isChecked());
                prefsEditor.putBoolean("autozoommap", item.isChecked());
                prefsEditor.apply();
                return true;
            case R.id.menu_action_exit:
                //dismiss the menu first
                exitApp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.setRetainInstance(true);

        Intent intent = getIntent();

        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        backend = myPrefs.getString("backend", "staging");
        if (backend.equals("croft")) {
            mqttServer = "tcp://croft.thethings.girovito.nl:1883";
            nodeaddr = myPrefs.getString("nodeaddress", "03FFEEBB");
            mqttTopic = myPrefs.getString("mqtttopiccroft", "nodes/" + nodeaddr + "/packets");
        } else {
            mqttServer = "tcp://staging.thethingsnetwork.org:1883";
            mqttUser = myPrefs.getString("mqttusername", "");
            mqttPassword = myPrefs.getString("mqttpassword", "");
            mqttTopic = myPrefs.getString("mqtttopicstaging", "+/devices/#");
        }
        //nodeAddress = intent.getStringExtra("address");
        createFile = myPrefs.getBoolean("savefile", true);
        logType = myPrefs.getString("logging_type", "global");
        experimentName = myPrefs.getString("experimentname", "experiment " + Math.random() * 999);

        //mqttTopic = "nodes/" + nodeAddress + "/packets";

        loggingFilename = myPrefs.getString("filename", "ttnmapper_logfile.csv");

        String loggingTo = logType;
        if (logType.equals("local")) {
            if (createFile) {
                loggingTo = loggingFilename;
            } else {
                loggingTo = "only your screen";
            }
        }
        Toast.makeText(getApplicationContext(), "Logging to " + loggingTo + " using backend " + mqttServer, Toast.LENGTH_LONG).show();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mqtt_connect();

        // Check permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPositionPermission();
        }
        if (createFile) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestFilePermission();
            }
        }

        //subscribe to position updates
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0/*seconds*/,
                0/*meter*/, mLocationListener);
    }

    private void requestFilePermission() {
        Log.i(TAG, "FILE permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.i(TAG, "Displaying file permission rationale to provide additional context.");
            Snackbar.make(findViewById(R.id.map), R.string.permission_file_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_FILE_PERMISSION);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_FILE_PERMISSION);
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();  // Always call the superclass method first
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        try {
            MyApp myApp = (MyApp) getApplication();
            if (myApp.isClosingDown()) {
                if (myApp.getMyMQTTclient() != null) {
                    myApp.getMyMQTTclient().disconnect();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //GPS position updates unsubscribe
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
                Log.d(TAG, "GPS updates removed");
            } catch (Exception e) {
                Log.d(TAG, "Can not remove gps updates");
            }
        }
//This causes multiple MQTT receptions after rotating the screen.
//        MyApp myApp = (MyApp) getApplication();
//        myApp.setMQTTclient(null);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Zoom in the Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        // set map type
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable MyLocation Layer of Google Map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPositionPermission();
            return;
        }
        mMap.setMyLocationEnabled(true);

        // Show the current location in Google Map
        mMap.moveCamera(CameraUpdateFactory.newLatLng(getLatestLatLng()));

        // Zoom in the Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        mMap.setOnCameraChangeListener(this);


//        new GoogleMap.OnCameraChangeListener() {
//            @Override
//            public void onCameraChange(CameraPosition cameraPosition) {
////                Log.d(TAG, "Camera position changed");
//                SharedPreferences myPrefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
//
//                if (myPrefs.getBoolean("autozoommap", true))
//                {
//                    float maxZoom = 14.0f;
//                    float minZoom = 2.0f;
//
//                    if (cameraPosition.zoom > maxZoom) {
//                        mMap.animateCamera(CameraUpdateFactory.zoomTo(maxZoom));
//                    } else if (cameraPosition.zoom < minZoom) {
//                        mMap.animateCamera(CameraUpdateFactory.zoomTo(minZoom));
//                    }
//                }
//
////                if (mMapIsTouched) {
////                    Log.d(TAG, "Disabling auto centre because of map drag");
////                    SharedPreferences myPrefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
////                    SharedPreferences.Editor prefsEditor = myPrefs.edit();
////                    prefsEditor.putBoolean("autocentremap", true);
////                    prefsEditor.apply();
////                }
//            }
//        });
    }

    private void requestPositionPermission() {
        Log.i(TAG, "LOCATION permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Log.i(TAG, "Displaying location permission rationale to provide additional context.");
            Snackbar.make(findViewById(R.id.map), R.string.permission_location_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "LOCATION permission has now been granted.");
                Snackbar.make(findViewById(R.id.map), R.string.permission_available_location,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "LOCATION permission was NOT granted.");
                Snackbar.make(findViewById(R.id.map), R.string.permissions_not_location,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_FILE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "FILE permission has now been granted.");
                Snackbar.make(findViewById(R.id.map), R.string.permission_available_file,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "FILE permission was NOT granted.");
                Snackbar.make(findViewById(R.id.map), R.string.permissions_not_file,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //get old, but latest location - only use for camera position
    private LatLng getLatestLatLng() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "No permission to get location information.", Toast.LENGTH_LONG).show();
            return new LatLng(0, 0);
        } else {
            // Get Current Location
            Location myLocation = getLatestLocation();

            double latitude = 0;
            double longitude = 0;

            try {
                // Get latitude of the current location
                latitude = myLocation.getLatitude();

                // Get longitude of the current location
                longitude = myLocation.getLongitude();
            } catch (Exception e) {
            }

            // Create a LatLng object for the current location
            return new LatLng(latitude, longitude);
        }
    }

    //get old, but latest location - only use for camera position
    private Location getLatestLocation() {
        // Enable MyLocation Layer of Google Map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "No permission to get location information.", Toast.LENGTH_LONG).show();
            return null;
        }
        // Get LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Create a criteria object to retrieve provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // Get the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Get Current Location
        return locationManager.getLastKnownLocation(provider);
    }

    private Location getCurrentLocation() {
        // Enable MyLocation Layer of Google Map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "No permission to get location information.", Toast.LENGTH_LONG).show();
            return null;
        }
        //    // Get LocationManager object from System Service LOCATION_SERVICE
        //    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //
        //    // Create a criteria object to retrieve provider
        //    Criteria criteria = new Criteria();
        //    criteria.setAccuracy(Criteria.ACCURACY_FINE);
        //
        //    // Get the name of the best provider
        //    String provider = locationManager.getBestProvider(criteria, true);
        //
        //    // Get Current Location
        //    return locationManager.getLastKnownLocation(provider);
        return currentLocation;
    }

    void mqtt_connect() {
        MyApp myApp = (MyApp) getApplication();
        MqttAndroidClient myMQTTclient = myApp.getMyMQTTclient();
        Log.d(TAG, "mqtt connect to " + mqttServer);
        if (myApp.getMyMQTTclient() == null) {
            myApp.createMqttClient(mqttServer);
            myMQTTclient = myApp.getMyMQTTclient();
        }
        try {
            if (myMQTTclient.isConnected()) {
                Log.d(TAG, "mqtt already connected, subscribing");
                mqtt_subscribe();
                return;
            }
        } catch (Exception e) {
            //catch an error with the mqtt handler when we rotate the screen and an instance already exist which throws an illegalArgumentException whe
        }

        try {
            MqttConnectOptions connOpt;
            connOpt = new MqttConnectOptions();
            connOpt.setCleanSession(true);
            connOpt.setKeepAliveInterval(30);
            if (!mqttPassword.isEmpty() && !backend.equals("croft")) {
                Log.d(TAG, "mqtt adding UN/PW " + mqttUser + " " + mqttPassword);
                connOpt.setUserName(mqttUser);
                connOpt.setPassword(mqttPassword.toCharArray());
            }

            IMqttToken token = myMQTTclient.connect(connOpt);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "mqtt connect onSuccess");
                    mqtt_retry_count = 0;
                    mqtt_subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "mqtt connect onFailure");

                    mqtt_retry_count++;

                    if (mqtt_retry_count > 1 && mqtt_retry_count < 5) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Can not connect to MQTT. Check your internet connection, AppEUI and Access Key.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    exception.printStackTrace();

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mqtt_connect();
                        }
                    }, 1000);
                }

            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    void mqtt_subscribe() {
        Log.d(TAG, "mqtt subscribe");
        try {
            MyApp myApp = (MyApp) getApplication();
            MqttAndroidClient myMQTTclient = myApp.getMyMQTTclient();

            myMQTTclient.setCallback(this);

            IMqttToken subToken = myMQTTclient.subscribe(mqttTopic, 1);
            Log.d(TAG, "mqtt subscribing to topic: " + mqttTopic);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The message was published
                    Log.d(TAG, "mqtt subscribe onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                    Log.d(TAG, "mqtt subscribe onFailure");
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mqtt_subscribe();
                        }
                    }, 1000);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    void mqtt_unsubscribe() {
        Log.d(TAG, "mqtt unsubscribe");
        //unsubscribe
        try {
            MyApp myApp = (MyApp) getApplication();
            MqttAndroidClient myMQTTclient = myApp.getMyMQTTclient();

            IMqttToken unsubToken = myMQTTclient.unsubscribe(mqttTopic);
            unsubToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The subscription could successfully be removed from the client
                    Log.d(TAG, "mqtt unsub onSuccess");
                    mqtt_disconnect();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // some error occurred, this is very unlikely as even if the client
                    // did not had a subscription to the topic the unsubscribe action
                    // will be successfully
                    Log.d(TAG, "mqtt unsub onFailure");
                    mqtt_disconnect();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    void mqtt_disconnect() {
        Log.d(TAG, "mqtt disconnect");
        //disconnect
        try {
            MyApp myApp = (MyApp) getApplication();
            MqttAndroidClient myMQTTclient = myApp.getMyMQTTclient();

            IMqttToken disconToken = myMQTTclient.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // we are now successfully disconnected
                    Log.d(TAG, "mqtt disconnect onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // something went wrong, but probably we are disconnected anyway
                    Log.d(TAG, "mqtt unsub onFailure");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "mqtt connection lost");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MyApp myApp = (MyApp) getApplication();
                if (!myApp.isClosingDown()) {
                    mqtt_connect();
                }
            }
        }, 1000);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        Log.d(TAG, "mqtt message arrived");
        System.out.println("Message arrived: " + message.toString());
        process_packet(message.toString(), topic);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "mqtt delivery complete");
    }


    void process_packet(String jsonData, String topic) {
    /* New format:
  {
    "payload":"WA==","port":1,"counter":3293,
    "metadata":
    [
      {"frequency":868.1,"datarate":"SF7BW125","codingrate":"4/5","gateway_timestamp":472142790,"gateway_time":"2016-05-01T19:44:53.877604706Z","channel":0,"server_time":"2016-05-01T19:44:53.756166662Z","rssi":-66,"lsnr":10,"rfchain":0,"crc":1,"modulation":"LORA","gateway_eui":"B827EBFFFF5FE05C","altitude":0,"longitude":4.63576,"latitude":52.37447},
      {"frequency":868.1,"datarate":"SF7BW125","codingrate":"4/5","gateway_timestamp":2320255000,"gateway_time":"2016-05-01T19:44:53.93683773Z","channel":0,"server_time":"2016-05-01T19:44:53.769196285Z","rssi":-46,"lsnr":10,"rfchain":0,"crc":1,"modulation":"LORA","gateway_eui":"00FE34FFFFD4578D","altitude":0,"longitude":0,"latitude":0}
    ]
  }
    */

        class AddMarker implements Runnable {

            private String topic;
            private String jsonData;

            public AddMarker(String jsonData, String topic) {
                this.topic = topic;
                this.jsonData = jsonData;
            }

            @Override
            public void run() {
                Log.d(TAG, "adding marker");

                //don't log if we don't know our location
                Location location = getCurrentLocation();
                if (location == null) {
                    Log.d(TAG, "Location is null");

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "GPS location not accurate enough. Try going outside.", Toast.LENGTH_LONG).show();
                        }
                    });

                    return;
                }

                if (backend.equals("croft")) //Old croft MQTT format
                {
                    try {
                        JSONObject received_data = new JSONObject(jsonData);

                        addMarker(location, received_data.getDouble("rssi"));
                        if (createFile) {
                            savePacket(location, received_data, nodeaddr);
                        }
                        if (logType.equals("global")) {
                            uploadPacket(location, received_data, nodeaddr, null, topic);
                        } else if (logType.equals("experiment")) {
                            uploadPacket(location, received_data, nodeaddr, null, topic);
                        } else {
                            //local only
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else { //Staging server MQTT format
                    String[] apart = topic.split("/");

                    //filter incorrect topics
                    if (apart.length != 4) {
                        return;
                    }

                    if (!apart[3].equals("up")) {
                        return;
                    }

                    String devEUI = apart[2];
                    String appEUI = apart[0];

                    //correct type of packet
                    System.out.println(topic);
                    System.out.println(jsonData);

                    try {
                        JSONObject received_data = new JSONObject(jsonData);
                        JSONArray gateways = received_data.getJSONArray("metadata");

                        for (int i = 0; i < gateways.length(); i++) {
                            JSONObject gateway = gateways.getJSONObject(i);
                            try {
                                addMarker(location, gateway.getDouble("rssi"));
                                if (createFile) {
                                    savePacket(location, gateway, topic);
                                }
                                if (logType.equals("global")) {
                                    uploadPacket(location, gateway, devEUI, appEUI, topic);
                                    uploadGateway(gateway);
                                } else if (logType.equals("experiment")) {
                                    uploadPacket(location, gateway, devEUI, appEUI, topic);
                                    //we do not trust experiments to update our gateway table
                                } else {
                                    //local only
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } //end of AddMarker inner class

        // start the inner class runnable
        Runnable addMarker = new AddMarker(jsonData, topic);
        runOnUiThread(addMarker);

    }

    void createMarkers() {
        int d = 30; // diameter
        if (circleBlack == null) {
            bmBlack = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmBlack);
            Paint p = new Paint();
            p.setColor(0x7f000000);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleBlack = BitmapDescriptorFactory.fromBitmap(bmBlack);
        }
        if (circleBlue == null) {
            bmBlue = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmBlue);
            Paint p = new Paint();
            p.setColor(0x7f0000ff);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleBlue = BitmapDescriptorFactory.fromBitmap(bmBlue);
        }
        if (circleCyan == null) {
            bmCyan = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmCyan);
            Paint p = new Paint();
            p.setColor(0x7f00ffff);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleCyan = BitmapDescriptorFactory.fromBitmap(bmCyan);
        }
        if (circleGreen == null) {
            bmGreen = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmGreen);
            Paint p = new Paint();
            p.setColor(0x7f00ff00);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleGreen = BitmapDescriptorFactory.fromBitmap(bmGreen);
        }
        if (circleYellow == null) {
            bmYellow = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmYellow);
            Paint p = new Paint();
            p.setColor(0x7fffff00);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleYellow = BitmapDescriptorFactory.fromBitmap(bmYellow);
        }
        if (circleOrange == null) {
            bmOrange = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmOrange);
            Paint p = new Paint();
            p.setColor(0x7fff7f00);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleOrange = BitmapDescriptorFactory.fromBitmap(bmOrange);
        }
        if (circleRed == null) {
            bmRed = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmRed);
            Paint p = new Paint();
            p.setColor(0x7fff0000);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleRed = BitmapDescriptorFactory.fromBitmap(bmRed);
        }
    }


    void addMarker(Location location, double rssi) {
        if (mMap != null) {
            createMarkers();
            MarkerOptions options = new MarkerOptions();

            if (rssi == 0) {
                options.icon(circleBlack);
            } else if (rssi < -120) {
                options.icon(circleBlue);
            } else if (rssi < -110) {
                options.icon(circleCyan);
            } else if (rssi < -100) {
                options.icon(circleGreen);
            } else if (rssi < -90) {
                options.icon(circleYellow);
            } else if (rssi < -80) {
                options.icon(circleOrange);
            } else {
                options.icon(circleRed);
            }
            options.position(new LatLng(location.getLatitude(), location.getLongitude()));
            options.anchor((float) 0.5, (float) 0.5);
            Marker marker = mMap.addMarker(options);
            markers.add(marker);

            SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
            if (myPrefs.getBoolean("autozoommap", true)) {
                autoScaleMap();
            }
        }
    }

    int getNavigationBarHeight() {
        Resources resources = getApplicationContext().getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    void autoScaleMap() {
        //http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
        int numberOfPoints = 0;
        double latMin = 0;
        double latMax = 0;
        double lonMin = 0;
        double lonMax = 0;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            LatLng markerPosition = marker.getPosition();
            builder.include(markerPosition);
            numberOfPoints++;

            if (latMin == 0 || markerPosition.latitude < latMin) {
                latMin = markerPosition.latitude;
            }
            if (latMax == 0 || markerPosition.latitude > latMax) {
                latMax = markerPosition.latitude;
            }
            if (lonMin == 0 || markerPosition.longitude < lonMin) {
                lonMin = markerPosition.longitude;
            }
            if (lonMax == 0 || markerPosition.longitude > lonMax) {
                lonMax = markerPosition.longitude;
            }
        }
        if (currentLocation != null) {
            builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));

            numberOfPoints++;
        }

        if (numberOfPoints > 0) {

            //if we keep the map centred, add as much left as right, and top as bottom
            SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
            if (myPrefs.getBoolean("autocentremap", true) && currentLocation != null) {
                if (currentLocation.getLatitude() > latMin && latMin != 0) {
                    builder.include(
                            new LatLng(
                                    currentLocation.getLatitude() + (currentLocation.getLatitude() - latMin),
                                    currentLocation.getLongitude()
                            )
                    );
                }

                if (latMax > currentLocation.getLatitude() && latMax != 0) {
                    builder.include(
                            new LatLng(
                                    currentLocation.getLatitude() - (latMax - currentLocation.getLatitude()),
                                    currentLocation.getLongitude()
                            )
                    );
                }

                if (currentLocation.getLongitude() > lonMin && lonMin != 0) {
                    builder.include(
                            new LatLng(
                                    currentLocation.getLatitude(),
                                    currentLocation.getLongitude() + (currentLocation.getLongitude() - lonMin)
                            )
                    );
                }

                if (lonMax > currentLocation.getLongitude() && lonMax != 0) {
                    builder.include(
                            new LatLng(
                                    currentLocation.getLatitude(),
                                    currentLocation.getLongitude() - (lonMax - currentLocation.getLongitude())
                            )
                    );
                }
            }

            LatLngBounds bounds = builder.build();

            int padding = getNavigationBarHeight(); // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);


            //Finally move the map:
            //mMap.moveCamera(cu);
            //Or if you want an animation:
            mMap.animateCamera(cu);
        }
    }

    void savePacket(Location location, JSONObject mqttJSONdata, String topic) {
        String gatewayAddr;
        String time;
        double freq;
        String dataRate;
        double rssi;
        double snr;

        try {
            if (backend.equals("croft")) {
                // {"gatewayEui":"00FE34FFFFD30DA7",
                // "nodeEui":"02017201",
                // "time":"2016-06-06T01:12:09.101797367Z",
                // "frequency":868.099975,
                // "dataRate":"SF7BW125",
                // "rssi":-46,
                // "snr":9,
                // "rawData":"QAFyAQIAxAABJeu0TLc=",
                // "data":"IQ=="}

                gatewayAddr = mqttJSONdata.getString("gatewayEui");
                time = mqttJSONdata.getString("time");
                freq = mqttJSONdata.getDouble("frequency");
                dataRate = mqttJSONdata.getString("dataRate");
                rssi = mqttJSONdata.getDouble("rssi");
                snr = mqttJSONdata.getDouble("snr");
            } else {
                //{"frequency":868.1,
                // "datarate":"SF7BW125",
                // "codingrate":"4/5",
                // "gateway_timestamp":472142790,
                // "gateway_time":"2016-05-01T19:44:53.877604706Z",
                // "channel":0,
                // "server_time":"2016-05-01T19:44:53.756166662Z",
                // "rssi":-66,
                // "lsnr":10,
                // "rfchain":0,
                // "crc":1,
                // "modulation":"LORA",
                // "gateway_eui":"B827EBFFFF5FE05C",
                // "altitude":0,
                // "longitude":4.63576,
                // "latitude":52.37447}
                gatewayAddr = mqttJSONdata.getString("gateway_eui");
                time = mqttJSONdata.getString("server_time");
                freq = mqttJSONdata.getDouble("frequency");
                dataRate = mqttJSONdata.getString("datarate");
                rssi = mqttJSONdata.getDouble("rssi");
                snr = mqttJSONdata.getDouble("lsnr");
            }


            Log.d(TAG, "adding to file");
    /*
      02031701

      time, nodeaddr, gwaddr, datarate, snr, rssi, freq, lat, lon
     */
            String data =
                    time + "," + topic + "," + gatewayAddr + "," +
                            dataRate + "," + snr + "," + rssi + "," +
                            freq + "," + location.getLatitude() + "," + location.getLongitude();

            // Find the root of the external storage.
            // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

            File root = android.os.Environment.getExternalStorageDirectory();
            Log.d(TAG, "\nExternal file system root: " + root);

            // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

            File dir = new File(root.getAbsolutePath() + "/ttnmapper_logs");
            dir.mkdirs();
            File file = new File(dir, loggingFilename);

            try {
                FileOutputStream f = new FileOutputStream(file, true);
                PrintWriter pw = new PrintWriter(f);
                pw.println(data);
                pw.flush();
                pw.close();
                f.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.i(TAG, "******* File not found. Did you" +
                        " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "\n\nFile written to " + file);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void uploadPacket(Location location, JSONObject mqttJSONdata, String devEUI, String appEUI, String topic) {
        String gatewayAddr;
        String time;
        double freq;
        String dataRate;
        double rssi;
        double snr;

        try {

            //object for storing Json
            JSONObject data = new JSONObject();

            if (backend.equals("croft")) {
                gatewayAddr = mqttJSONdata.getString("gatewayEui");
                time = mqttJSONdata.getString("time");
                freq = mqttJSONdata.getDouble("frequency");
                dataRate = mqttJSONdata.getString("dataRate");
                rssi = mqttJSONdata.getDouble("rssi");
                snr = mqttJSONdata.getDouble("snr");
            } else {
                gatewayAddr = mqttJSONdata.getString("gateway_eui");
                time = mqttJSONdata.getString("server_time");
                freq = mqttJSONdata.getDouble("frequency");
                dataRate = mqttJSONdata.getString("datarate");
                rssi = mqttJSONdata.getDouble("rssi");
                snr = mqttJSONdata.getDouble("lsnr");
                data.put("appeui", appEUI);
            }

            Log.d(TAG, "logging packet to server");
            data.put("time", time);
            data.put("nodeaddr", devEUI);
            data.put("gwaddr", gatewayAddr);
            data.put("datarate", dataRate);
            data.put("snr", snr);
            data.put("rssi", rssi);
            data.put("freq", freq);
            data.put("lat", location.getLatitude());
            data.put("lon", location.getLongitude());
            if (location.hasAltitude()) {
                data.put("alt", location.getAltitude());
            }
            if (location.hasAccuracy()) {
                data.put("accuracy", location.getAccuracy());
            }
            data.put("provider", location.getProvider());
            data.put("mqtt_topic", topic);

            //add version name of this app
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = pInfo.versionName;
                int verCode = pInfo.versionCode;
                data.put("user_agent", "Android" + android.os.Build.VERSION.RELEASE + " App" + verCode + ":" + version);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            //the only difference between a normal upload and an experiment is the experiment name parameter
            if (logType.equals("experiment")) {
                data.put("experiment", experimentName);
            }

            String dataString = data.toString();
            System.out.println(dataString);
            String url = "http://ttnmapper.org/api/upload.php";

            //post packet
            postToServer(url, dataString, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "error uploading", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final String returnedString = response.body().string();
                        System.out.println("HTTP response: " + returnedString);
                        if (!returnedString.contains("New records created successfully")) {
                            // Request not successful
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "server error: " + returnedString, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        // Do what you want to do with the response.
                    } else {
                        // Request not successful
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "server error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void uploadGateway(JSONObject gateway) {
        /*
        {"payload":"IQ==","port":1,"counter":2,"dev_eui":"0000000002017202","metadata":
        [
            {"frequency":868.1,"datarate":"SF7BW125","codingrate":"4/5","gateway_timestamp":149785848,"channel":0,"server_time":"2016-06-07T16:19:44.21718223Z","rssi":-46,"lsnr":9,"rfchain":0,"crc":1,"modulation":"LORA","gateway_eui":"00FE34FFFFD30DA7","altitude":0,"longitude":0,"latitude":0}
        ]}
         */
        try {
            String gatewayAddr = gateway.getString("gateway_eui");
            String time = gateway.getString("server_time");
            double lat = gateway.getDouble("latitude");
            double lon = gateway.getDouble("longitude");
            double alt = gateway.getDouble("altitude");

            Log.d(TAG, "logging gateway to server");

            //object for storing Json
            JSONObject data = new JSONObject();
            data.put("time", time);
            data.put("gwaddr", gatewayAddr);
            data.put("lat", lat);
            data.put("lon", lon);
            data.put("alt", alt);

            String dataString = data.toString();
            System.out.println(dataString);
            String url = "http://ttnmapper.org/api/update_gateway.php";

            //post packet
            postToServer(url, dataString, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "error uploading", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final String returnedString = response.body().string();
                        System.out.println("HTTP response: " + returnedString);
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) { //this is onnly called after an animation is done
//                Log.d(TAG, "Camera position changed");
//                SharedPreferences myPrefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
//
//                if (myPrefs.getBoolean("autozoommap", true))
//                {
//                    float maxZoom = 14.0f;
//                    float minZoom = 2.0f;
//
//                    if (cameraPosition.zoom > maxZoom) {
//                        mMap.animateCamera(CameraUpdateFactory.zoomTo(maxZoom));
//                    } else if (cameraPosition.zoom < minZoom) {
//                        mMap.animateCamera(CameraUpdateFactory.zoomTo(minZoom));
//                    }
//                }

//                if (mMapIsTouched) {
//                    Log.d(TAG, "Disabling auto centre because of map drag");
//                    SharedPreferences myPrefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
//                    SharedPreferences.Editor prefsEditor = myPrefs.edit();
//                    prefsEditor.putBoolean("autocentremap", true);
//                    prefsEditor.apply();
//                }
    }
}
