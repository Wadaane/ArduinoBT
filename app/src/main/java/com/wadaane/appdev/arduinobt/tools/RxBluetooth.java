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
    public static final CompositeDisposable disposables = new CompositeDisposable();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static RxCommunications bluetoothCommunications;
    public static Observable<BluetoothSocket> connectObservable;
    public static DisposableObserver<BluetoothSocket> connectObserver;
    public static Flowable<String> readFlowable;
    private static String TAG = "RxBluetooth";
    private static Consumer<String> readConsumer;
    private BluetoothAdapter mBluetoothAdapter;

    public RxBluetooth() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private static BluetoothSocket connect(String deviceName) {
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

    private static Observable<BluetoothSocket> connectObservable(final String deviceName) {
        if (connectObservable == null)
            connectObservable = Observable.defer(new Callable<Observable<BluetoothSocket>>() {
                @Override
                public Observable<BluetoothSocket> call() throws Exception {
                    return Observable.just(connect(deviceName));
                }
            });
        return connectObservable;
    }

    private static DisposableObserver<BluetoothSocket> connectObserver() {
        if (connectObserver == null)
            connectObserver = new DisposableObserver<BluetoothSocket>() {
                @Override
                public void onNext(@NonNull BluetoothSocket con) {
                    try {
                        bluetoothCommunications = new RxCommunications(con);
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

    private static Flowable<String> readObservable() {
        if (readFlowable == null)
            readFlowable = Flowable.defer(new Callable<Flowable<String>>() {
                @Override
                public Flowable<String> call() throws Exception {
                    return bluetoothCommunications.observeStringStream('#');
                }
            });
        return readFlowable;
    }

    private static Consumer<String> readObserver() {
        if (readConsumer == null) {
            readConsumer = new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) throws Exception {
                    Log.e(TAG, s);
                }
            };
        }
        return readConsumer;
    }

    public void startBTListening(String deviceName) {
        disposables.clear();
        if (bluetoothCommunications == null)
            disposables.add(
                    connectObservable(deviceName)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.computation())
                            .subscribeWith(connectObserver()));
    }

    public void sendData(String data) {
        if (bluetoothCommunications != null) {
            bluetoothCommunications.send(data);
        }
    }

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     * <p>Equivalent to:
     * <code>getBluetoothState() == STATE_ON</code>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     *
     * @return true if the local adapter is turned on
     */
    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * This will issue a request to enable Bluetooth through the system settings (without stopping
     * your application) via ACTION_REQUEST_ENABLE action Intent.
     *
     * @param activity    Activity
     * @param requestCode request code
     */
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
        bluetoothCommunications.closeConnection();
    }
}

class RxCommunications {

    private static final String TAG = RxCommunications.class.getName();

    private BluetoothSocket socket;

    private InputStream inputStream;
    private OutputStream outputStream;

    private Flowable<Byte> mObserveInputStream;

    private boolean connected = false;

    /**
     * Container for simplifying read and write from/to {@link BluetoothSocket}.
     *
     * @param socket bluetooth socket
     * @throws Exception if can't get input/output stream from the socket
     */
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

    /**
     * Observes byte from bluetooth's {@link InputStream}. Will be emitted per byte.
     *
     * @return RxJava Observable with {@link Byte}
     */
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

    /**
     * Observes string from bluetooth's {@link InputStream}.
     *
     * @param delimiter char(s) used for string delimiter
     * @return RxJava Observable with {@link String}
     */
    Flowable<String> observeStringStream(final int... delimiter) {
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

    /**
     * Send array of bytes to bluetooth output stream.
     *
     * @param bytes data to send
     * @return true if success, false if there was error occurred or disconnected
     */
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

    /**
     * Send string of text to bluetooth output stream.
     *
     * @param text text to send
     * @return true if success, false if there was error occurred or disconnected
     */
    boolean send(String text) {
        Log.e(TAG, "send: " + text);
        byte[] sBytes = text.getBytes();
        return send(sBytes);
    }

    /**
     * Close the streams and socket connection.
     */
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