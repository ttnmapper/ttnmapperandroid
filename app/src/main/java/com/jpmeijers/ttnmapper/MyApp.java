package com.jpmeijers.ttnmapper;

import android.app.Application;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * Created by jpmeijers on 2016/03/10.
 */
public class MyApp extends Application {
    //keep a list of all the markers
    List<MarkerOptions> markers = new ArrayList<>();
    List<PolylineOptions> lines = new ArrayList<>();
    List<MarkerOptions> gwMarkers = new ArrayList<>();
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
        myMQTTclient = new MqttAndroidClient(this.getApplicationContext(), /*"tcp://staging.thethingsnetwork.org:1883"*/serverAddress/*"tcp://croft.thethings.girovito.nl:1883"*/, mqttClientId);
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
}
