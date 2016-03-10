package com.jpmeijers.ttnmapper;

import android.app.Application;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import okhttp3.OkHttpClient;

/**
 * Created by jpmeijers on 2016/03/10.
 */
public class MyApp extends Application {
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

    public void createMqttClient() {
        String mqttClientId = MqttClient.generateClientId();
        myMQTTclient = new MqttAndroidClient(this.getApplicationContext(), "tcp://croft.thethings.girovito.nl:1883", mqttClientId);
    }
}
