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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

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
//        implements NavigationView.OnNavigationItemSelectedListener {

    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final CompositeDisposable disposables = new CompositeDisposable();
    public static Handler Psender = null;
    public static Handler Preceiver = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        }
    };
    static float maxValue = 1;
    static SeekBar seekBar;
    static EditText editText;
    static float distance = 0, angle = 0;
    private static String TAG = "Processing Activity";
    private static String CHOICE = "";
    private static RxBluetooth bluetooth;
    private static SharedPreferences preferences;
    private static BluetoothConnection bluetoothConnection;
    //    DrawerLayout drawer;
    private static Observable<BluetoothSocket> connectObservable;
    private static DisposableObserver<BluetoothSocket> connectObserver;
    private static Consumer<String> readConsumer;
    private static Flowable<String> readFlowable;
    PApplet sketch;
    PFragment fragment;
    LinearLayout layout;
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
//        Log.e(TAG, "readObservable()");
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
//        Log.e(TAG, "readObserver()");
        if (readConsumer == null)
            readConsumer = new Consumer<String>() {
                @Override
                public void accept(@NonNull String s) throws Exception {
                    if (s.startsWith("d")) {
                        distance = Integer.parseInt(s.substring(1));
                        if (Psender != null) {
                            maxValue = Math.max(maxValue, distance);
                            Psender.obtainMessage(202, distance / maxValue).sendToTarget();
                            Psender.obtainMessage(200, (int) maxValue, (int) distance).sendToTarget();
                        }
                    } else if (s.startsWith("a")) {
                        angle = Integer.parseInt(s.substring(1));
//                        seekBar.setProgress((int) angle);
//                        editText.setText(String.valueOf((int)angle));

                        if (Psender != null) {
                            Psender.obtainMessage(201, angle).sendToTarget();
                        }
                    }
//                    Log.e(TAG, "accept()" + s);
                }
            };
        return readConsumer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate()");
        init(R.layout.content_processing);

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

//        layout = new LinearLayout(this);
//        layout.setOrientation(LinearLayout.VERTICAL);
//        LinearLayoutCompat.LayoutParams layoutParams = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        layout.setLayoutParams(layoutParams);

        container = (FrameLayout) findViewById(R.id.cont);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        editText = (EditText) findViewById(R.id.editTextAngle);
//        setContentView(layout,layoutParams);
//        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        layoutParams2.setMargins(0,10,0,0);
//        container = new FrameLayout(this);
//        container.setId(R.id.sketch_cont);
//        container.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
//        container.setElevation(0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
//        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawSketch(CHOICE);
                if (bluetoothConnection != null)
                    bluetoothConnection.send("s#");
            }
        });
//
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawer.setDrawerListener(toggle);
//        toggle.syncState();
//        drawer.openDrawer(Gravity.START);

//        container.setVisibility(View.INVISIBLE);
//        fab.setVisibility(View.INVISIBLE);
//        drawer.setVisibility(View.INVISIBLE);
        container.post(new Runnable() {
            @Override
            public void run() {
                drawSketch(CHOICE);
//                container.setVisibility(View.VISIBLE);
//                fab.setVisibility(View.VISIBLE);
//                drawer.setVisibility(View.VISIBLE);
            }
        });
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//        navigationView.setNavigationItemSelectedListener(this);
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

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

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: ");
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
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        super.onBackPressed();
//        }
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

//        sketch = null;
//        connectObservable = null;
//        readFlowable = null;
//        readConsumer = null;
//        Preceiver = null;
//        Psender = null;

//        if (bluetooth.isBluetoothEnabled())bluetooth.disableBluetooth();
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_processing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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
//            container.post(new Runnable() {
//                @Override
//                public void run() {
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
//                }
//            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startBTListening();
        Log.e(TAG, "onActivityResult()");
    }

    //    @SuppressWarnings("StatementWithEmptyBody")
//    @Override
//    public boolean onNavigationItemSelected(MenuItem item) {
//        // Handle navigation view item clicks here.
//        int id = item.getItemId();
//        Intent starter;
//        switch (id) {
//            case R.id.arduino:
//                starter = new Intent(this, Activity_SensorsList.class);
//                start(starter);
//                break;
//
//            case R.id.processing_image:
//                starter = new Intent(this, Activity_Processing.class);
//                starter.putExtra("Sketch", "image");
//                start(starter);
//                break;
//            case R.id.processing_particles:
//                starter = new Intent(this, Activity_Processing.class);
//                starter.putExtra("Sketch", "particles");
//                start(starter);
//                break;
//            case R.id.processing_light:
//                starter = new Intent(this, Activity_Processing.class);
//                starter.putExtra("Sketch", "light");
//                start(starter);
//                break;
//            default:
//                Toast.makeText(this, "Action not Set", Toast.LENGTH_SHORT).show();
//        }
//
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
//        return true;
//    }
//    private void start(Intent starter) {
////        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        Pair<View, String> p1 = Pair.create(this.findViewById(R.id.app_bar), "appBar");
//        Pair<View, String> p2 = Pair.create(this.findViewById(R.id.fab), "FAB");
//        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, p1, p2);
////        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this,findViewById(R.id.fab),"FAB");
//        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
//        startActivity(starter, options.toBundle());
//        finish();
////        supportFinishAfterTransition();
////        startActivity(starter);
//    }
}
