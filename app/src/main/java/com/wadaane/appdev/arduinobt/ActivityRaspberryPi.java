package com.wadaane.appdev.arduinobt;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wadaane.appdev.arduinobt.tools.BluetoothConnection;
import com.wadaane.appdev.arduinobt.tools.RxBluetooth;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class ActivityRaspberryPi extends AppCompatActivity {

    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final CompositeDisposable disposables = new CompositeDisposable();
    static String TAG = "ActivityRaspberryPi";

    private static BluetoothConnection bluetoothConnection;
    private static Observable<BluetoothSocket> connectObservable;
    private static DisposableObserver<BluetoothSocket> connectObserver;
    private static Consumer<String> readConsumer;
    private static Flowable<String> readFlowable;
    private static Context this_context;
    private static SharedPreferences preferences;
    private static RxBluetooth bluetooth;
    EditText edit_send;
    Button bt_send;

    static BluetoothSocket connect() {
        BluetoothDevice mmDevice;
        BluetoothSocket mmSocket;
        String deviceName = preferences.getString("DEVICE", "0");

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

    static Observable<BluetoothSocket> connectObservable() {
        if (connectObservable == null)
            connectObservable = Observable.defer(new Callable<Observable<BluetoothSocket>>() {
                @Override
                public Observable<BluetoothSocket> call() throws Exception {
                    return Observable.just(connect());
                }
            });
        return connectObservable;
    }

    static DisposableObserver<BluetoothSocket> connectObserver() {
        if (connectObserver == null)
            connectObserver = new DisposableObserver<BluetoothSocket>() {
                @Override
                public void onNext(@NonNull BluetoothSocket con) {
                    try {
                        bluetoothConnection = new BluetoothConnection(con);
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

    static Flowable<String> readObservable() {
        if (readFlowable == null)
            readFlowable = Flowable.defer(new Callable<Flowable<String>>() {
                @Override
                public Flowable<String> call() throws Exception {
                    return bluetoothConnection.observeStringStream('#');
                }
            });
        return readFlowable;
    }

    static Consumer<String> readObserver() {
        if (readConsumer == null) {
            readConsumer = new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) throws Exception {
                    Toast.makeText(this_context, s, Toast.LENGTH_LONG).show();
                }
            };
        }
        return readConsumer;
    }

    private void startBTListening() {
        disposables.clear();
        if (bluetoothConnection == null)
            disposables.add(
                    connectObservable()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.computation())
                            .subscribeWith(connectObserver()));
    }

    private void sendData(String data) {
        if (bluetoothConnection != null)
            bluetoothConnection.send(data);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raspberyy_pi);
        this_context = this;
        preferences = getSharedPreferences("SETTINGS", MODE_PRIVATE);

        edit_send = (EditText) findViewById(R.id.edit_send);
        bt_send = (Button) findViewById(R.id.btsend);

        edit_send.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String data = edit_send.getText().toString();
                if (data.length() > 0) sendData(data);
                edit_send.setText("");
                return true;
            }
        });

        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = edit_send.getText().toString();
                if (data.length() > 0) sendData(data);
                edit_send.setText("");
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        bluetooth = new RxBluetooth(this);
        if (bluetooth.isBluetoothEnabled()) startBTListening();
        else bluetooth.enableBluetooth(this, 1);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        Log.e(TAG, "disposables.clear()");
        if (connectObservable != null)
            connectObservable.unsubscribeOn(Schedulers.computation());
        if (readFlowable != null) readFlowable.unsubscribeOn(Schedulers.io());
        if (bluetoothConnection != null) {
            bluetoothConnection.closeConnection();
            bluetoothConnection = null;
        }
        bluetooth.disableBluetooth();
        connectObserver = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.about) {
            showDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About us");
        builder.setMessage("    Wadaane, Mohamed Salim");
        builder.setPositiveButton("Email: salimrichab@gmail.com", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "salimrichab@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "ArduinoBt");
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startBTListening();
    }

}
