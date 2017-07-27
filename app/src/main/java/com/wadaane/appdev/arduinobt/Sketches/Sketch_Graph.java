package com.wadaane.appdev.arduinobt.Sketches;

import android.os.Handler;
import android.os.Message;

import com.wadaane.appdev.arduinobt.Activity_Processing;

import processing.core.PApplet;

public class Sketch_Graph extends PApplet {
    private static int maxDist = 0, dist;
    private int mWidth, mHeight;
    private float angle = 0, mAngle = 0, distance = 0, mDistance = 0, radius = 0;

    public Sketch_Graph(int width, int height, Handler sender) {
        mWidth = width;
        mHeight = height;
        radius = min(width, height) / 2f;
//        mSender = sender;

        Activity_Processing.Psender = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 202:
                        setDistance((float) msg.obj);
                        break;
                    case 201:
                        setAngle((float) msg.obj);
                        break;
                    case 200:
                        maxDist = msg.arg1;
                        dist = msg.arg2;
                    default:
                        break;
                }
            }
        };
    }

    public void settings() {
        size(mWidth, mHeight);
        //angle = width/1;
//        x = width/2;
//        y = height/2;
    }

    public void setup() {
        background(0);
        fill(255);
        strokeWeight(10);
        distance = radius;
//        line(width/2f,height,width,height);
        //ellipse(x, y, angle, angle);
    }

    public void draw() {
        pushMatrix();
        if (angle <= 1) fill(0);//angle>=179 ||
        else fill(0, 0);
        noStroke();
        rect(0, 0, width, height);
        stroke(255);
        translate(width / 2f, height / 2f);
        mAngle = lerp(angle, mAngle, 0.5f);
        mDistance = lerp(distance, mDistance, 0.5f);
        rotate(radians(-mAngle));
        line(0, 0, mDistance, 0);
//            stroke(255,0,0);
//            line(mDistance,0,radius,0);

        popMatrix();
        textSize(64);
        fill(0);
        noStroke();
        rect(width / 2f, height / 2f + 64, width / 2f, -64);
        fill(255);
        String text = String.valueOf(maxDist);
        text(dist + " cm/" + maxDist + " cm", width / 2f, height / 2f + 64);
    }

    private void setAngle(float a) {
        angle = a;
//        Log.e("setAngle()", String.valueOf(a));
//        if (width<height) mAngle = constrain(width*ratio,0,width);
//        else mAngle = constrain(height*ratio,0,height);
    }

    private void setDistance(float ratio) {
        distance = ratio * radius;
    }
}
