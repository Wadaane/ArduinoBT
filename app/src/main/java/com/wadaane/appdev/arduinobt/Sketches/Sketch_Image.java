package com.wadaane.appdev.arduinobt.Sketches;

import processing.core.PApplet;
import processing.core.PImage;

public class Sketch_Image extends PApplet {
    //String TAG = "Sketch_Image";
    private PImage img;
    //private int count = 0;
    private int[] mPixels;
    private int mWidth, mHeight;
//    int loc = 0;

    public Sketch_Image(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void settings() {
        size(mWidth, mHeight);
    }

    public void setup() {
        img = loadImage("liu.png");
        //img.resize(mHeight*img.width/img.height,mHeight);
        background(0);
        smooth();
        img.loadPixels();
        mPixels = img.pixels;
    }

    public void draw() {
        int time = millis();
        for (int loc = 1; loc < mPixels.length; loc++) {
            int x, y;
            float r, g, b;

//            x = (int)random(img.width);
//            y = (int)random(img.height);
//            loc = x + y*img.width;

            r = red(mPixels[loc]);
            g = green(mPixels[loc]);
            b = blue(mPixels[loc]);

            x = loc % img.width;
            y = loc / img.width;

            noStroke();
            fill(r, g, b, 100);
            int rad = 16;
            ellipse(x - (img.width / 2) + width / 2, y, rad, rad);
        }

        textSize(64);
        fill(255);
        text("Time: " + ((time - millis()) / 1000) + " seconds ", 0, 64);
    }
}
