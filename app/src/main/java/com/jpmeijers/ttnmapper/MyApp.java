package com.jpmeijers.ttnmapper;

import android.app.Application;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * Created by jpmeijers on 2016/03/10.
 */
public class MyApp extends Application {
    //keep a list of all the markers
    List<MarkerOptions> markers = new ArrayList<>();
    List<PolylineOptions> lines = new ArrayList<>();
    List<MarkerOptions> gwMarkers = new ArrayList<>();
    Map<String, ArrayList<PolygonOptions>> gwCoverage = new HashMap<>();
    Map<String, ArrayList<MarkerOptions>> gwCoverageMarkers = new HashMap<>();
    LatLngBounds lastViewBounds = null;
    CameraPosition lastCameraLocation = null;

    private MqttAndroidClient myMQTTclient;
    private OkHttpClient client;
    private boolean closingDown = false;

    public boolean isClosingDown() {
        return closingDown;
    }

    public void setClosingDown(boolean closingDown) {
        this.closingDown = closingDown;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.client = new OkHttpClient();
    }

    public void setMQTTclient(MqttAndroidClient newMqttClient) {
        this.myMQTTclient = newMqttClient;
    }

    public MqttAndroidClient getMyMQTTclient() {
        return this.myMQTTclient;
    }

    public OkHttpClient getHttpClient() {
        return this.client;
    }

    public void setHttpClient(OkHttpClient newClient) {
        this.client = newClient;
    }

    public void createMqttClient(String serverAddress)
    {
        String mqttClientId = MqttClient.generateClientId();
        System.out.println("Server address: " + serverAddress);
        myMQTTclient = new MqttAndroidClient(this.getApplicationContext(), serverAddress, mqttClientId);
    }

    public List<MarkerOptions> getMarkers() {
        return markers;
    }

    public List<MarkerOptions> getGatewayMarkers() {
        return gwMarkers;
    }

    public List<PolylineOptions> getLines() {
        return lines;
    }

    public Map<String, ArrayList<PolygonOptions>> getGwCoverage() {
        return gwCoverage;
    }

    public Map<String, ArrayList<MarkerOptions>> getGwCoverageMarkers() {
        return gwCoverageMarkers;
    }

    public LatLngBounds getLastViewBounds() {
        return lastViewBounds;
    }

    public void setLastViewBounds(LatLngBounds bounds) {
        lastViewBounds = bounds;
    }

    public void setLastCameraLocation(CameraPosition cameraLocation)
    {
        this.lastCameraLocation = cameraLocation;
    }

    public CameraPosition getLastCameraLocation()
    {
        return lastCameraLocation;
    }
}
