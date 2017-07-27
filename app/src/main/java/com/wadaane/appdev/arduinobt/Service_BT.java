package com.wadaane.appdev.arduinobt;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.wadaane.appdev.arduinobt.tools.MyBluetooth;

public class Service_BT extends Service implements SensorEventListener {
    final static double EPSILON = 1e-12;
    public static String minX = "", maxX = "";
    static boolean connect = false;
    final IBinder myBinder = new MyBinder();
    public float angle = 0;
    public String arduinoMsg = "";
    int choice = 0;
    String address;
    boolean yourTurn = false;
    String ADDRESS = "";
    boolean processing = false;
    MyBluetooth bluetooth;
    Handler BTreceiver = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!processing) yourTurn = !yourTurn;
            String writeBuf = (String) msg.obj;
            if (msg.what == 1234) {
                if (writeBuf.equals("READY")) {
                    setArduino();
                } else {
                    arduinoMsg = writeBuf;
                }
            }
        }
    };
    private SensorManager mSensorManager;
    private Sensor sensor;


    public Service_BT() {
    }

    public static float map(float value) {
        float startCoord2 = 0;
        float endCoord2 = 1023;
        float startCoord1 = Float.valueOf(minX);
        float endCoord1 = Float.valueOf(maxX);
/*
        if (value < startCoord1) value = startCoord1;
        if (value > endCoord1) value = endCoord1;
*/
        if (Math.abs(endCoord1 - startCoord1) < EPSILON) {
            throw new ArithmeticException("/ 0");
        }

        float offset = startCoord2;
        float ratio = (endCoord2 - startCoord2) / (endCoord1 - startCoord1);
        return ratio * (value - startCoord1) + offset;
    }

    @Override
    public void onCreate() {
        SharedPreferences preferences = getSharedPreferences("SETTINGS", MODE_PRIVATE);

        minX = preferences.getString("EXTRA_MIN", "0");
        maxX = preferences.getString("EXTRA_MAX", "0");

        //minX = Activity_Sensor.EXTRA_MIN;
        Activity_Sensor.STATUS = true;
        //Toast.makeText(this, "onCreate()", Toast.LENGTH_LONG).show();
        address = Activity_Sensor.EXTRA_SENSOR;
        choice = Activity_Sensor.CHOICE;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Integer.valueOf(address));
        if (sensor == null) {
            Toast.makeText(this, "Sensor Not Available", Toast.LENGTH_LONG).show();
        }

        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        connectedNotification("CON");
        ADDRESS = preferences.getString("DEVICE", "0");
        bluetooth = MyBluetooth.getInstance();
        bluetooth.init(this, ADDRESS, BTreceiver);
    }

    private void setArduino() {
        bluetooth.write("START_SETUP");
        /*
        mConnectedThread.write((minX + "#").getBytes());
        mConnectedThread.write((maxX + "#").getBytes());
        */
        bluetooth.write("END_SETUP#");
        connect = true;
    }

    private void connectedNotification(String status) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (status.equals("DIS")) {
            manager.cancel(1989);
        } else {
            Notification.Builder mBuilder = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("BTArduino");
            switch (status) {
                case "ON":
                    mBuilder.setContentText("Arduino: " + arduinoMsg);
                    break;
                case "OFF":
                    arduinoMsg = "Disconnected !!";
                    mBuilder.setContentText(arduinoMsg);
                    break;
                case "CON":
                    arduinoMsg = "Connecting ...";
                    mBuilder.setContentText(arduinoMsg);
                    break;
                case "BLU":
                    arduinoMsg = "Turn On Bluetooth ...";
                    mBuilder.setContentText(arduinoMsg);
                    break;
            }
            Intent intent = new Intent(this, Activity_Sensor.class);
            intent.putExtra("SENSOR", String.valueOf(sensor.getType()));
            intent.putExtra("MIN", minX);
            intent.putExtra("MAX", maxX);
            PendingIntent i = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(i);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                manager.notify(1989, mBuilder.build());
            }
        }
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        Activity_Sensor.STATUS = false;
        connectedNotification("DIS");
        bluetooth.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        angle = event.values[choice];
        connectedNotification("ON");
        if (!yourTurn) {
            yourTurn = !yourTurn;
            bluetooth.write(String.valueOf(map(angle)));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class MyBinder extends Binder {
        Service_BT getService() {
            return Service_BT.this;
        }
    }

}
