/*
 * Copyright (C) 2015 Ivan Baranov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wadaane.appdev.arduinobt.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;

/**
 * Enables clients to listen to bluetooth events using RxJava Observables.
 */
public class RxBluetooth {
    private BluetoothAdapter mBluetoothAdapter;
    private Context context;

    public RxBluetooth(Context context) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
    }

    /**
     * Return true if Bluetooth is available.
     *
     * @return true if mBluetoothAdapter is not null or it's address is empty, otherwise Bluetooth is
     * not supported on this hardware platform
     */
    public boolean isBluetoothAvailable() {
        return !(mBluetoothAdapter == null || TextUtils.isEmpty(mBluetoothAdapter.getAddress()));
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

    /**
     * Return the set of {@link BluetoothDevice} objects that are bonded
     * (paired) to the local adapter.
     * <p>If Bluetooth state is not {@link BluetoothAdapter#STATE_ON}, this API
     * will return an empty set. After turning on Bluetooth,
     * wait for {@link BluetoothAdapter#ACTION_STATE_CHANGED} with {@link BluetoothAdapter#STATE_ON}
     * to get the updated value.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}.
     *
     * @return unmodifiable set of {@link BluetoothDevice}, or null on error
     */
    public Set<BluetoothDevice> getBondedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    /**
     * Create connection to {@link BluetoothDevice} and returns a connected {@link BluetoothSocket}
     * on successful connection. Notifies observers with {@link IOException} {@code onError()}.
     *
     * @param bluetoothDevice bluetooth device to connect
     * @param uuid            uuid for SDP record
     * @return observable with connected {@link BluetoothSocket} on successful connection
     */
    public Observable<BluetoothSocket> observeConnectDevice(final BluetoothDevice bluetoothDevice,
                                                            final UUID uuid) {
        return Observable.defer(
                new Callable<ObservableSource<? extends BluetoothSocket>>() {
                    @Override
                    public ObservableSource<? extends BluetoothSocket> call() throws Exception {
                        return Observable.create(new ObservableOnSubscribe<BluetoothSocket>() {
                            @Override
                            public void subscribe(@NonNull ObservableEmitter<BluetoothSocket> emitter)
                                    throws Exception {
                                try {
                                    BluetoothSocket bluetoothSocket =
                                            bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                                    bluetoothSocket.connect();
                                    emitter.onNext(bluetoothSocket);
                                } catch (IOException e) {
                                    emitter.onError(e);
                                }
                            }
                        });
                    }
                });
    }
}