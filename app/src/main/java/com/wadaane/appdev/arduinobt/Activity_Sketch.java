package com.wadaane.appdev.arduinobt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.wadaane.appdev.arduinobt.Sketches.Sketch_Particles;

import processing.android.PFragment;
import processing.core.PApplet;

public class Activity_Sketch extends AppCompatActivity {
    private PApplet sketch;
    private FrameLayout container;
    private Button button;
    private Activity_Sketch context = this;
    private PFragment fragment;
    private boolean isShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch);

        container = (FrameLayout) findViewById(R.id.sketch);
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isShowing) {

                    container = (FrameLayout) findViewById(R.id.sketch);
                    sketch = new Sketch_Particles(container.getWidth(), container.getHeight());
                    fragment = new PFragment(sketch);
                    fragment.setView(container, context);
                    isShowing = true;
                } else {
                    getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                    isShowing = false;
                    container = null;
                    sketch = null;
                    fragment = null;
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
