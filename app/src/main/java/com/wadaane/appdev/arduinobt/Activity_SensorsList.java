package com.wadaane.appdev.arduinobt;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wadaane.appdev.arduinobt.tools.MyAdapter;
import com.wadaane.appdev.arduinobt.tools.MyOnClickListener;

import java.util.ArrayList;
import java.util.Set;

public class Activity_SensorsList extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MyOnClickListener {
    public static String EXTRA_ADDRESS = "";
    static String DEVICE = "0";
    Button btnPaired;
    TextView tvDeviceName;
    RecyclerView recyclerView;
    RecyclerView.Adapter myAdapter;
    RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init(R.layout.activity_sensorlist);

        btnPaired = (Button) findViewById(R.id.btScanSensor);
        tvDeviceName = (TextView) findViewById(R.id.textViewDeviceName);
        SensorManager mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        myAdapter = new MyAdapter(mySensorManager.getSensorList(Sensor.TYPE_ALL), this, this);
        recyclerView.setAdapter(myAdapter);


        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(true);
            }
        });

        DEVICE = getSharedPreferences("SETTINGS", MODE_PRIVATE).getString("DEVICE", "0");

        if (DEVICE.equals("0")) {
            showDialog(true);
        } else tvDeviceName.setText(DEVICE);
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

    public void showDialog(boolean showDevice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!showDevice) {

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
        } else {
            //newGame();

            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
            }

            while (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            }

            Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            ArrayList list = new ArrayList<>();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice bt : pairedDevices) {
                    list.add(bt.getName()); //Get the device's name and the address
                }
            } else {
                Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
            }

            final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);

            builder.setTitle("Select Arduino Device");
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String info = adapter.getItem(which).toString();
                    EXTRA_ADDRESS = info;

                    SharedPreferences preferences = getSharedPreferences("SETTINGS", MODE_PRIVATE);
                    preferences.edit().putString("DEVICE", EXTRA_ADDRESS).apply();
                    tvDeviceName.setText(info);

                    dialog.dismiss();
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about) {
            showDialog(false);
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
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

    public void start(Intent starter) {
//        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Pair<View, String> p1 = Pair.create(this.findViewById(R.id.app_bar), "appBar");
        Pair<View, String> p2 = Pair.create(this.findViewById(R.id.fab), "FAB");
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, p1, p2);
        startActivity(starter, options.toBundle());
        finish();
//        supportFinishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("Acitivity_SensorList", "onDestroy");
        Intent i = new Intent(Activity_SensorsList.this, Service_BT.class);
        stopService(i);
    }

    @Override
    public void myOnClick(Intent intent) {
        start(intent);
    }
}

