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
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wadaane.appdev.arduinobt.Sketches.Sketch_Graph;
import com.wadaane.appdev.arduinobt.Sketches.Sketch_Image;
import com.wadaane.appdev.arduinobt.Sketches.Sketch_LightSensor;
import com.wadaane.appdev.arduinobt.Sketches.Sketch_Particles;
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
import processing.android.PFragment;
import processing.core.PApplet;

public class Activity_Processing extends AppCompatActivity {

    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final CompositeDisposable disposables = new CompositeDisposable();
    public static Handler Psender = null;
    public static Handler Preceiver = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        }
    };
    static float maxValue = 1;
    static float distance = 0, angle = 0;
    static SeekBar seekBar;
    static EditText editText;
    private static String TAG = "Processing Activity";
    private static String CHOICE = "";
    private static RxBluetooth bluetooth;
    private static Context this_context;
    private static SharedPreferences preferences;
    private static BluetoothConnection bluetoothConnection;
    private static Observable<BluetoothSocket> connectObservable;
    private static DisposableObserver<BluetoothSocket> connectObserver;
    private static Consumer<String> readConsumer;
    private static Flowable<String> readFlowable;
    PApplet sketch;
    PFragment fragment;
    FloatingActionButton fab;
    private FrameLayout container;

    static BluetoothSocket connect() {
        Log.e(TAG, "connect()");
        boolean connect = false;
        boolean connected = false;
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
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            connect = true;
            Log.e(TAG, "connect()" + true);
        } catch (IOException e) {
            connect = false;
            Log.e(TAG, "connect()" + false);
        }
        mmSocket = tmp;

        try {
            mmSocket.connect();
            connected = true;
            Log.e(TAG, "connect()" + true);
        } catch (IOException connectException) {
            try {
                mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                mmSocket.connect();
                connected = true;
                Log.e(TAG, "connect()" + true);
            } catch (Exception e2) {
                Log.e(TAG, "connect()" + false);
                try {
                    mmSocket.close();
                    connected = false;
                } catch (IOException ignored) {
                }
            }
        }

        return mmSocket;
    }

    static Observable<BluetoothSocket> connectObservable() {
        Log.e(TAG, "connectObservable()");
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
        Log.e(TAG, "connectObserver()");
        if (connectObserver == null)
            connectObserver = new DisposableObserver<BluetoothSocket>() {
                @Override
                public void onNext(@NonNull BluetoothSocket con) {
                    try {
                        bluetoothConnection = new BluetoothConnection(con);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "onNext: " + (con != null));
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
                    if (s.startsWith("d")) {
                        distance = Integer.parseInt(s.substring(1));
                        if (Psender != null) {
                            maxValue = Math.max(maxValue, distance);
                            Psender.obtainMessage(202, distance / maxValue).sendToTarget();
                            Psender.obtainMessage(200, (int) maxValue, (int) distance).sendToTarget();
                        }
                    } else if (s.startsWith("a")) {
                        angle = Integer.parseInt(s.substring(1));

                        if (Psender != null) {
                            Psender.obtainMessage(201, angle).sendToTarget();
                        }
                    }
                }
            };
        }
        return readConsumer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate()");
        init(R.layout.content_processing);
        this_context = this;

        CHOICE = getIntent().getExtras().getString("Sketch");
        preferences = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        if (CHOICE.equals("light")) {
            bluetooth = new RxBluetooth(this);
            if (bluetooth.isBluetoothEnabled()) startBTListening();
            else bluetooth.enableBluetooth(this, 1);
        }
    }

    void init(int id) {
        setContentView(id);

        container = (FrameLayout) findViewById(R.id.cont);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        editText = (EditText) findViewById(R.id.editTextAngle);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawSketch(CHOICE);
                if (bluetoothConnection != null)
                    bluetoothConnection.send("s#");
            }
        });

        container.post(new Runnable() {
            @Override
            public void run() {
                drawSketch(CHOICE);
            }
        });
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                editText.setText(String.valueOf(i));
                if (bluetoothConnection != null) bluetoothConnection.send(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String a = editText.getText().toString();
                Log.e(TAG, "onClick: " + a);
                if (bluetoothConnection != null) bluetoothConnection.send(a);
                seekBar.setProgress(Integer.parseInt(a));
                editText.setText("");
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                return false;
            }
        });
    }

    private void startBTListening() {
        Log.e(TAG, "startBTListening()");
        disposables.clear();
        if (bluetoothConnection == null)
            disposables.add(
                    connectObservable()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.computation())
                            .subscribeWith(connectObserver()));
    }

    @Override
    public void onBackPressed() {
        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");
        if (CHOICE.equals("light")) {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_processing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.about:
                showDialog();
                break;

            case R.id.btSpeedUp:
                if (bluetoothConnection != null)
                    bluetoothConnection.send("u#");
                break;

            case R.id.btresetSpeed:
                if (bluetoothConnection != null)
                    bluetoothConnection.send("r#");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void drawSketch(final String choice) {
        if (fragment != null) {
            maxValue = 1;
        } else {
            final Context context = this;
            switch (choice) {
                case "light":
                    sketch = new Sketch_Graph(container.getWidth(), container.getHeight(), Preceiver);
                    break;
                case "particles":
                    sketch = new Sketch_Particles(container.getWidth(), container.getHeight());
                    break;
                case "image":
                    sketch = new Sketch_Image(container.getWidth(), container.getHeight());
                    break;
                default:
                    sketch = new Sketch_LightSensor(container.getWidth(), container.getHeight(), Preceiver);
            }
            fragment = new PFragment(sketch);
            maxValue = 1;
            fragment.setView(container, (FragmentActivity) context);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startBTListening();
    }
}
