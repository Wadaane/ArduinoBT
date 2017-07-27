package com.wadaane.appdev.arduinobt;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_Sensor extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener, View.OnClickListener {
    final static double EPSILON = 1e-12;
    public static boolean STATUS = false;
    public static String EXTRA_SENSOR = "SENSOR";
    public static String EXTRA_MIN = "";
    public static String EXTRA_MAX = "";
    public static int CHOICE = -1;
    public static Handler receiver = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        }
    };
    public static Handler sender = null;
    static Service_BT serviceBT;
    private static boolean bound = false;
    SharedPreferences preferences;
    TextView tvValue, tvMin, tvMax, tvService;
    float minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
    Button btStart, btX, btY, btZ, btStop;
    EditText editMin, editMax;
    float angle = 0;
    private SensorManager mSensorManager;
    private Sensor sensor;
    private ServiceConnection con = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Service_BT.MyBinder binder = (Service_BT.MyBinder) service;
            serviceBT = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //  bound = false;
        }
    };

    public static float map(float value) {
        float startCoord2 = 0;
        float endCoord2 = 1023;
        float startCoord1 = Float.parseFloat(EXTRA_MIN);
        float endCoord1 = Float.parseFloat(EXTRA_MAX);
/*
        if (value < startCoord1) value = startCoord1;
        if (value > endCoord1) value = endCoord1;
*/
        if (Math.abs(endCoord1 - startCoord1) < EPSILON) {
            throw new ArithmeticException("/ 0");
        }

        float ratio = (endCoord2 - startCoord2) / (endCoord1 - startCoord1);
        value = ratio * (value - startCoord1) + startCoord2;
        if (value < 0) value = 0;
        if (value > 1023) value = 1023;

        return value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init(R.layout.activity_sensor);

        TextView tvType = (TextView) findViewById(R.id.tvSensorName);
        tvValue = (TextView) findViewById(R.id.tvValue);
        tvMin = (TextView) findViewById(R.id.tvMin);
        tvMax = (TextView) findViewById(R.id.tvMax);
        tvService = (TextView) findViewById(R.id.tvService);
        editMin = (EditText) findViewById(R.id.editTextMin);
        editMax = (EditText) findViewById(R.id.editTextMax);
        btStart = (Button) findViewById(R.id.btStart);
        btX = (Button) findViewById(R.id.btX);
        btY = (Button) findViewById(R.id.btY);
        btZ = (Button) findViewById(R.id.btZ);
        btStop = (Button) findViewById(R.id.btStop);

        btX.setOnClickListener(this);
        btY.setOnClickListener(this);
        btZ.setOnClickListener(this);
        btStop.setOnClickListener(this);
        btStart.setOnClickListener(this);

        preferences = getSharedPreferences("SETTINGS", MODE_PRIVATE);


        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }

        Intent newint = getIntent();
        final String sensorType = newint.getStringExtra("SENSOR");
        EXTRA_SENSOR = sensorType;
        editMin.setText(newint.getStringExtra("MIN"));
        editMax.setText(newint.getStringExtra("MAX"));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Integer.valueOf(sensorType));
        tvType.setText(sensor.getName());
        if (sensor == null) {
            Toast.makeText(getApplicationContext(), "Sensor Not Available", Toast.LENGTH_LONG).show();
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

    }

    void init(int id) {

//        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
//        getWindow().setEnterTransition(new Slide(Gravity.LEFT));
//        getWindow().setExitTransition(new Fade());

        setContentView(id);
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


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    public void onClick(View src) {
        switch (src.getId()) {
            case R.id.btStart:
                if (CHOICE != -1) {

                    if (!editMin.getText().toString().equals(""))
                        preferences.edit().putString("EXTRA_MIN", editMin.getText().toString()).apply();
                    else preferences.edit().putString("EXTRA_MIN", String.valueOf(minX)).apply();

                    if (!editMax.getText().toString().equals(""))
                        preferences.edit().putString("EXTRA_MAX", editMax.getText().toString()).apply();
                    else
                        preferences.edit().putString("EXTRA_MAX", String.valueOf(maxX)).apply();// EXTRA_MAX = String.valueOf(maxX);

                    Intent i = new Intent(Activity_Sensor.this, Service_BT.class);

                    if (!STATUS) startService(i);
                    else
                        Toast.makeText(getApplicationContext(), "Service alive", Toast.LENGTH_SHORT).show();
                    if (!bound) bindService(i, con, Context.BIND_AUTO_CREATE);

                    editMin.setEnabled(false);
                    editMax.setEnabled(false);

                } else Toast.makeText(this, "Please Select X Y or Z", Toast.LENGTH_LONG).show();

                break;

            case R.id.btStop:
                Intent in = new Intent(Activity_Sensor.this, Service_BT.class);
                if (bound) unbindService(con);
                bound = false;
                if (STATUS) stopService(in);
                else
                    Toast.makeText(getApplicationContext(), "Service dead", Toast.LENGTH_SHORT).show();

                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.cancel(1989);

                editMin.setEnabled(true);
                editMax.setEnabled(true);

                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    BluetoothAdapter.getDefaultAdapter().disable();
                }
                break;


            case R.id.btX:
                CHOICE = 0;
                EXTRA_MIN = String.valueOf(minX);
                EXTRA_MAX = String.valueOf(maxX);
                editMin.setText(EXTRA_MIN);
                editMax.setText(EXTRA_MAX);
                break;

            case R.id.btY:
                CHOICE = 1;
                EXTRA_MIN = String.valueOf(minY);
                EXTRA_MAX = String.valueOf(maxY);
                editMin.setText(EXTRA_MIN);
                editMax.setText(EXTRA_MAX);
                break;

            case R.id.btZ:
                CHOICE = 2;
                EXTRA_MIN = String.valueOf(minZ);
                EXTRA_MAX = String.valueOf(maxZ);
                editMin.setText(EXTRA_MIN);
                editMax.setText(EXTRA_MAX);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        Intent i = new Intent(Activity_Sensor.this, Service_BT.class);
        if (bound) {
            bound = false;
            bindService(i, con, Context.BIND_AUTO_CREATE);
            editMin.setEnabled(false);
            editMax.setEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (bound) unbindService(con);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (bound) {
            tvService.setText("Arduino: " + serviceBT.arduinoMsg + '\n');
            if (Float.valueOf(EXTRA_MIN) < Float.valueOf(EXTRA_MAX))
                tvService.append("Sensor (" + EXTRA_MIN + " ~ " + EXTRA_MAX + "): " + String.valueOf(angle) + '\n');
            else
                tvService.append("Sensor (" + EXTRA_MAX + " ~ " + EXTRA_MIN + "): " + String.valueOf(angle) + '\n');
            tvService.append("Sent (0 ~ 1023): " + String.valueOf(map(angle)));
        } else tvService.setText("Service Off.\n \n");

        tvValue.setText("Value\n");
        angle = event.values[0];
        int i = 0;
        tvMin.setText("Min\n");
        tvMax.setText("Max\n");

        for (float value : event.values) {
            tvValue.append(String.valueOf(value) + "\n");

            switch (i) {
                case 0:
                    if (value < minX) {
                        minX = value;
                    }
                    tvMin.append(String.valueOf(minX));
                    if (value > maxX) {
                        maxX = value;
                    }
                    tvMax.append(String.valueOf(maxX));
                    break;
                case 1:
                    if (value < minY) {
                        minY = value;
                    }
                    tvMin.append('\n' + String.valueOf(minY));
                    if (value > maxY) {
                        maxY = value;
                    }
                    tvMax.append('\n' + String.valueOf(maxY));
                    break;
                case 2:
                    if (value < minZ) {
                        minZ = value;
                    }
                    tvMin.append('\n' + String.valueOf(minZ));
                    if (value > maxZ) {
                        maxZ = value;
                    }
                    tvMax.append('\n' + String.valueOf(maxZ));
                    break;
            }
            /////tvValue.setText();
            i++;
        }
        if (sender != null) sender.obtainMessage(202, event.values[0] / maxX).sendToTarget();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            supportFinishAfterTransition();
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent starter;
        switch (id) {
            case R.id.arduino:
                starter = new Intent(this, Activity_SensorsList.class);
                start(starter);
                break;

            case R.id.processing_image:
                starter = new Intent(this, Activity_Processing.class);
                starter.putExtra("Sketch", "image");
                start(starter);
                break;
            case R.id.processing_particles:
                starter = new Intent(this, Activity_Processing.class);
                starter.putExtra("Sketch", "particles");
                start(starter);
                break;
            case R.id.processing_light:
                starter = new Intent(this, Activity_Processing.class);
                starter.putExtra("Sketch", "light");
                start(starter);
                break;
            default:
                Toast.makeText(this, "Action not Set", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void start(Intent starter) {
//        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Pair<View, String> p1 = Pair.create(this.findViewById(R.id.app_bar), "appBar");
        Pair<View, String> p2 = Pair.create(this.findViewById(R.id.fab), "FAB");
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, p1, p2);
        startActivity(starter, options.toBundle());
//        finish();
        supportFinishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
