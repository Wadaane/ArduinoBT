package com.wadaane.appdev.arduinobt.tools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MyBluetooth {

    private static final MyBluetooth ourInstance = new MyBluetooth();
    private static boolean connect = false;
    private static boolean connected = false;
    private static boolean stopAll = false;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ConnectThread mConnectThread = null;
    private String ADDRESS = "";
    private Handler mHandler;

    private MyBluetooth() {
    }

    public static MyBluetooth getInstance() {
        return ourInstance;
    }

    public void init(Context con, String deviceName, Handler mmhandler) {
        Context context = con;
        this.ADDRESS = deviceName;
        this.mHandler = mmhandler;

        BluetoothAdapter.getDefaultAdapter().enable();
        //noinspection StatementWithEmptyBody
        while (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
        }
        connect();
    }

    public void stop() {
        stopAll = true;
        restartConnection();
    }

    public void write(String s) {
        mConnectThread.write((s + "#").getBytes());
    }

    private void connect() {
//        if (!mBluetoothAdapter.isEnabled()) {
//            mBluetoothAdapter.enable();
//        }
        for (BluetoothDevice bt : mBluetoothAdapter.getBondedDevices()) {
            if (bt.getName().equals(ADDRESS)) {
                mConnectThread = new ConnectThread(bt, mHandler);
                mConnectThread.start();
                break;
            }
        }
    }

    private void restartConnection() {
        connected = false;
        connect = false;

        if (mConnectThread != null) mConnectThread.cancel();

        mConnectThread = null;

        while (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mBluetoothAdapter.disable();
        }

        if (!stopAll) {
            while (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothAdapter.enable();
            }

            connect();
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        InputStream mmInStream;
        OutputStream mmOutStream;
        private BluetoothSocket mmSocket;
        private Handler han;

        ConnectThread(BluetoothDevice device, Handler hand) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            han = hand;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                connect = true;
            } catch (IOException e) {
                connect = false;
            }
            mmSocket = tmp;
        }

        public void run() {
            if (connect) {
                InputStream tmpIn = null;
                OutputStream tmpOut = null;
                mBluetoothAdapter.cancelDiscovery();
                try {
                    mmSocket.connect();
                    try {
                        tmpIn = mmSocket.getInputStream();
                        tmpOut = mmSocket.getOutputStream();
                    } catch (IOException ignored) {
                    }
                    mmInStream = tmpIn;
                    mmOutStream = tmpOut;
                    connected = true;
                } catch (IOException connectException) {
                    try {
                        mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                        mmSocket.connect();
                        try {
                            tmpIn = mmSocket.getInputStream();
                            tmpOut = mmSocket.getOutputStream();
                        } catch (IOException ignored) {
                        }
                        mmInStream = tmpIn;
                        mmOutStream = tmpOut;
                        connected = true;
                    } catch (Exception e2) {
                        try {
                            mmSocket.close();
                            restartConnection();
                        } catch (IOException ignored) {
                        }
                    }
                }

                if (connected) {
                    byte[] buffer = new byte[1024];
                    int begin = 0;
                    int bytes = 0;
                    while (true) {
                        try {
                            bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                            for (int i = begin; i < bytes; i++) {
                                if (buffer[i] == "#".getBytes()[0]) {
                                    String string = new String(buffer);
                                    string = string.substring(begin, i);
                                    han.obtainMessage(1234, string).sendToTarget();
                                    begin = i + 1;
                                    if (i == bytes - 1) {
                                        bytes = 0;
                                        begin = 0;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            restartConnection();
                            break;
                        }
                    }
                }
            }
        }

        void write(byte[] bytes) {
            try {
                if (connected) mmOutStream.write(bytes);
            } catch (IOException e) {
                restartConnection();
            }
        }

        void cancel() {
            try {
                if (connect) {
                    mmInStream.close();
                    mmOutStream.close();
                }
                mmSocket.close();
                connected = false;
                connect = false;

            } catch (IOException ignored) {
            }
        }
    }
}
