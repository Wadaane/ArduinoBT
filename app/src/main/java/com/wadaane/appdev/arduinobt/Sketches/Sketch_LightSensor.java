package com.wadaane.appdev.arduinobt.Sketches;

import android.os.Handler;
import android.os.Message;

import com.wadaane.appdev.arduinobt.Activity_Processing;

import processing.core.PApplet;

public class Sketch_LightSensor extends PApplet {
    private static Handler mSender;
    private int mWidth, mHeight, sec = 0, x, y, r = 150, g = 150, b = 150;
    private float rad = 10, mRad = 1;

    public Sketch_LightSensor(int width, int height, Handler sender) {
        mWidth = width;
        mHeight = height;
        mSender = sender;

        Activity_Processing.Psender = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 202) {
                    setRadius((float) msg.obj);
                }
            }
        };
    }

    public void settings() {
        size(mWidth, mHeight);
        rad = width / 10;
        x = width / 2;
        y = height / 2;
    }

    public void setup() {
        background(0);
        fill(255);
        ellipse(x, y, rad, rad);
    }

    public void draw() {
        background(0);
        fill(255);

        rad = lerp(rad, mRad, 0.1f);
        ellipse(x, y, rad, rad);
//        mSender.obtainMessage(101,rad).sendToTarget();
    }

    private void setRadius(float ratio) {
        if (width < height) mRad = constrain(width * ratio, 0, width);
        else mRad = constrain(height * ratio, 0, height);
    }

}
