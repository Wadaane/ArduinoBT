package com.wadaane.appdev.arduinobt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.wadaane.appdev.arduinobt.tools.RxBluetooth;

import io.reactivex.schedulers.Schedulers;

public class ActivityRaspberryPi extends AppCompatActivity {

    //    static String TAG = "ActivityRaspberryPi";
    private static RxBluetooth bluetooth;
    private static SharedPreferences preferences;
    EditText edit_send;
    Button bt_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raspberyy_pi);
        preferences = getSharedPreferences("SETTINGS", MODE_PRIVATE);

        edit_send = (EditText) findViewById(R.id.edit_send);
        bt_send = (Button) findViewById(R.id.btsend);

        edit_send.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String data = edit_send.getText().toString();
                if (data.length() > 0) bluetooth.sendData(data);
                edit_send.setText("");
                return true;
            }
        });

        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = edit_send.getText().toString();
                if (data.length() > 0) bluetooth.sendData(data);
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

        bluetooth = new RxBluetooth();
        if (bluetooth.isBluetoothEnabled())
            bluetooth.startBTListening(preferences.getString("DEVICE", "0"));
        else bluetooth.enableBluetooth(this, 1);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBluetooth.disposables.clear();
        if (RxBluetooth.connectObservable != null)
            RxBluetooth.connectObservable.unsubscribeOn(Schedulers.computation());
        if (RxBluetooth.readFlowable != null)
            RxBluetooth.readFlowable.unsubscribeOn(Schedulers.io());
        if (RxBluetooth.bluetoothCommunications != null) {
            bluetooth.closeConnection();
            RxBluetooth.bluetoothCommunications = null;
        }
        bluetooth.disableBluetooth();
        RxBluetooth.connectObserver = null;
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
        bluetooth.startBTListening(preferences.getString("DEVICE", "0"));
    }

}
