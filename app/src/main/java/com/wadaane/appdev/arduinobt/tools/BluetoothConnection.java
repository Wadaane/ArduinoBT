package com.wadaane.appdev.arduinobt.tools;


import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableOperator;

public class BluetoothConnection {

    private static final String TAG = BluetoothConnection.class.getName();

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
    public BluetoothConnection(BluetoothSocket socket) throws Exception {
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
    public Flowable<Byte> observeByteStream() {
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
     * Observes string from bluetooth's {@link InputStream} with '\r' (Carriage Return)
     * and '\n' (New Line) as delimiter.
     *
     * @return RxJava Observable with {@link String}
     */
    public Flowable<String> observeStringStream() {
        return observeStringStream('\r', '\n');
    }

    /**
     * Observes string from bluetooth's {@link InputStream}.
     *
     * @param delimiter char(s) used for string delimiter
     * @return RxJava Observable with {@link String}
     */
    public Flowable<String> observeStringStream(final int... delimiter) {
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
     * Send one byte to bluetooth output stream.
     *
     * @param oneByte a byte
     * @return true if success, false if there was error occurred or disconnected
     */
    public boolean send(byte oneByte) {
        return send(new byte[]{oneByte});
    }

    /**
     * Send array of bytes to bluetooth output stream.
     *
     * @param bytes data to send
     * @return true if success, false if there was error occurred or disconnected
     */
    public boolean send(byte[] bytes) {
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
    public boolean send(String text) {
        Log.e(TAG, "send: " + text);
        byte[] sBytes = text.getBytes();
        return send(sBytes);
    }

    /**
     * Close the streams and socket connection.
     */
    public void closeConnection() {
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