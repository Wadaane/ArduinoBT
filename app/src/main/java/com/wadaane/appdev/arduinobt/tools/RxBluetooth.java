package com.wadaane.appdev.arduinobt.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableOperator;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class RxBluetooth {
    public final CompositeDisposable disposables = new CompositeDisposable();
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public RxCommunications rxCommunications;
    public Observable<BluetoothSocket> connectObservable;
    public DisposableObserver<BluetoothSocket> connectObserver;
    public Flowable<String> readFlowable;
    private String TAG = "RxBluetooth";
    private Consumer<String> readConsumer;
    private BluetoothAdapter mBluetoothAdapter;
    private android.widget.TextView textview;

    public RxBluetooth(Activity activity, int widget_id) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        textview = activity.findViewById(widget_id);
    }

    public RxBluetooth() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private BluetoothSocket connect(String deviceName) {
        BluetoothDevice mmDevice;
        BluetoothSocket mmSocket;
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        BluetoothDevice device = null;

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                if (bt.getName().equals(deviceName))
                    device = bt;
            }
        }
        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            if (device != null) {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
        } catch (IOException ignored) {
        }
        mmSocket = tmp;

        try {
            if (mmSocket != null) {
                mmSocket.connect();
            }
        } catch (IOException connectException) {
            try {
                mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                mmSocket.connect();
            } catch (Exception e2) {
                try {
                    mmSocket.close();
                } catch (IOException ignored) {
                }
            }
        }

        return mmSocket;
    }

    private Observable<BluetoothSocket> connectObservable(final String deviceName) {
        if (connectObservable == null)
            connectObservable = Observable.defer(new Callable<Observable<BluetoothSocket>>() {
                @Override
                public Observable<BluetoothSocket> call() throws Exception {
                    return Observable.just(connect(deviceName));
                }
            });
        return connectObservable;
    }

    private DisposableObserver<BluetoothSocket> connectObserver() {
        if (connectObserver == null)
            connectObserver = new DisposableObserver<BluetoothSocket>() {
                @Override
                public void onNext(@NonNull BluetoothSocket con) {
                    try {
                        rxCommunications = new RxCommunications(con);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {

                }

                @Override
                public void onComplete() {
                    disposables.add(
                            readObservable()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(readObserver()));
                }
            };
        return connectObserver;
    }

    private Flowable<String> readObservable() {
        if (readFlowable == null)
            readFlowable = Flowable.defer(new Callable<Flowable<String>>() {
                @Override
                public Flowable<String> call() throws Exception {
                    return rxCommunications.observeStringStream();
                }
            });
        return readFlowable;
    }

    private Consumer<String> readObserver() {
        if (readConsumer == null) {
            readConsumer = new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) throws Exception {
                    Log.e(TAG, s);
                    if (textview != null) textview.setText(s);

                }
            };
        }
        return readConsumer;
    }

    public void startBTListening(String deviceName) {
        disposables.clear();
        if (rxCommunications == null)
            disposables.add(
                    connectObservable(deviceName)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.computation())
                            .subscribeWith(connectObserver()));
    }

    public void sendData(String data) {
        if (rxCommunications != null) {
            rxCommunications.send(data);
        }
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public void enableBluetooth(Activity activity, int requestCode) {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestCode);
        }
    }

    public void disableBluetooth() {
        mBluetoothAdapter.disable();
    }

    public void closeConnection() {
        rxCommunications.closeConnection();
    }
}

class RxCommunications {

    private final String TAG = "RxCommunications";
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Flowable<Byte> mObserveInputStream;
    private boolean connected = false;

    RxCommunications(BluetoothSocket socket) throws Exception {
        if (socket == null) {
            throw new InvalidParameterException("Bluetooth socket can't be null");
        }

        this.socket = socket;

        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            connected = true;
        } catch (IOException e) {
            throw new Exception("Can't get stream from bluetooth socket");
        } finally {
            if (!connected) {
                closeConnection();
            }
        }
    }

    private Flowable<Byte> observeByteStream() {
        if (mObserveInputStream == null) {
            mObserveInputStream = Flowable.create(new FlowableOnSubscribe<Byte>() {
                @Override
                public void subscribe(final FlowableEmitter<Byte> subscriber) {
                    while (!subscriber.isCancelled() && connected) {
                        try {
                            subscriber.onNext((byte) inputStream.read());
                        } catch (IOException e) {
                            connected = false;
                        } finally {
                            if (!connected) {
                                closeConnection();
                            }
                        }
                    }
                }
            }, BackpressureStrategy.BUFFER).share();
        }

        return mObserveInputStream;
    }

    Flowable<String> observeStringStream() {
        return observeStringStream('\r', '\n');
    }

    private Flowable<String> observeStringStream(final int... delimiter) {
        return observeByteStream().lift(new FlowableOperator<String, Byte>() {
            @Override
            public Subscriber<? super Byte> apply(final Subscriber<? super String> subscriber) {
                return new Subscriber<Byte>() {
                    ArrayList<Byte> buffer = new ArrayList<>();

                    @Override
                    public void onSubscribe(Subscription d) {
                        subscriber.onSubscribe(d);
                    }

                    @Override
                    public void onComplete() {
                        if (!buffer.isEmpty()) {
                            emit();
                        }
                        subscriber.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!buffer.isEmpty()) {
                            emit();
                        }
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(Byte b) {
                        boolean found = false;
                        for (int d : delimiter) {
                            if (b == d) {
                                found = true;
                                break;
                            }
                        }

                        if (found) {
                            emit();
                        } else {
                            buffer.add(b);
                        }
                    }

                    private void emit() {
                        if (buffer.isEmpty()) {
                            subscriber.onNext("");
                            return;
                        }

                        byte[] bArray = new byte[buffer.size()];

                        for (int i = 0; i < buffer.size(); i++) {
                            bArray[i] = buffer.get(i);
                        }

                        subscriber.onNext(new String(bArray));
                        buffer.clear();
                    }
                };
            }
        }).onBackpressureBuffer();
    }

    private boolean send(byte[] bytes) {
        if (!connected) return false;

        try {
            outputStream.write(bytes);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            // Error occurred. Better to close terminate the connection
            connected = false;
//            Log.e(TAG, "Fail to send data");
            return false;
        } finally {
            if (!connected) {
                closeConnection();
            }
        }
    }

    boolean send(String text) {
        Log.e(TAG, "send: " + text);
        byte[] sBytes = text.getBytes();
        return send(sBytes);
    }

    void closeConnection() {
        try {
            connected = false;

            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
                Log.e(TAG, "closeConnection: input closed");
            }

            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
                Log.e(TAG, "closeConnection: output closed");
            }

            if (socket != null) {
                socket.close();
                socket = null;
                Log.e(TAG, "closeConnection: socket closed");
            }
        } catch (IOException ignored) {
        }
    }
}