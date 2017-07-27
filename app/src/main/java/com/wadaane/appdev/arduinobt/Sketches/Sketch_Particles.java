package com.wadaane.appdev.arduinobt.Sketches;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

public class Sketch_Particles extends PApplet {
    private int mWidth, mHeight, fps, fpsCounter;
    private ParticleSystem ps;
    private PImage sprite;
    private String TAG = "Sketch_Particles";

    public Sketch_Particles(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void settings() {
        size(mWidth, mHeight, P2D);
    }

    @Override
    public void setup() {
        sprite = loadImage("sprite.png");
        ps = new ParticleSystem(2000);

        hint(DISABLE_DEPTH_MASK);
    }

    @Override
    public void draw() {
        background(0);
        if (fpsCounter <= 10) {
            fps += PApplet.parseInt(frameRate);
            fpsCounter++;
        } else {
            fps /= fpsCounter;
            if (fps - 40 < -10) ps.decreaseParticles();
            if (fps - 60 > 0) ps.increaseParticles();
            fpsCounter = 0;
            fps = 0;
        }
        ps.update();
        ps.display();
        ps.setEmitter(mouseX, mouseY);

        fill(255);
        textSize(16);
        text("Frame rate: " + PApplet.parseInt(frameRate) + " Particles: " + ps.particles.size(), 10, 20);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Particle p : ps.particles) {
            p.dispose();
            p = null;
        }
        ps.particleShape = null;
        ps = null;
        sprite = null;
    }

    class Particle {

        PVector velocity;
        float lifespan = 255;

        PShape part;
        float partSize;

        PVector gravity = new PVector(0, 0.1f);


        Particle() {
            partSize = random(10, 60);
            part = createShape();
            part.beginShape(QUAD);
            part.noStroke();
            part.texture(sprite);
            part.normal(0, 0, 1);
            part.vertex(-partSize / 2, -partSize / 2, 0, 0);
            part.vertex(+partSize / 2, -partSize / 2, sprite.width, 0);
            part.vertex(+partSize / 2, +partSize / 2, sprite.width, sprite.height);
            part.vertex(-partSize / 2, +partSize / 2, 0, sprite.height);
            part.endShape();

            rebirth(width / 2, height / 2);
            lifespan = random(255);
        }

        PShape getShape() {
            return part;
        }

        void rebirth(float x, float y) {
            float a = random(TWO_PI);
            float speed = random(0.5f, 4);
            velocity = new PVector(cos(a), sin(a));
            velocity.mult(speed);
            lifespan = 255;
            part.resetMatrix();
            part.translate(x, y);
        }

        boolean isDead() {
            if (lifespan < 0) {
                return true;
            } else {
                return false;
            }
        }

        public void update() {
            lifespan = lifespan - 1;
            velocity.add(gravity);

            part.setTint(color(255, lifespan));
            part.translate(velocity.x, velocity.y);
        }

        public void dispose() {
            part = null;
            velocity = null;
            gravity = null;
        }
    }

    class ParticleSystem {
        ArrayList<Particle> particles;

        PShape particleShape;

        ParticleSystem(int n) {
            particles = new ArrayList<Particle>();
            particleShape = createShape(PShape.GROUP);

            for (int i = 0; i < n; i++) {
                Particle p = new Particle();
                particles.add(p);
                particleShape.addChild(p.getShape());
            }
        }

        void decreaseParticles() {
            int n = particles.size() - 1;
            for (int i = n; i >= n * 0.75; i--) {
                particles.remove(i);
                particleShape.removeChild(i);
            }
        }

        void increaseParticles() {
            int n = particles.size() - 1;
            for (int i = n; i < n * 1.25f; i++) {
                Particle p = new Particle();
                particles.add(p);
                particleShape.addChild(p.getShape());
            }
        }

        void update() {
            for (Particle p : particles) {
                p.update();
            }
        }

        void setEmitter(float x, float y) {
            for (Particle p : particles) {
                if (p.isDead()) {
                    p.rebirth(x, y);
                }
            }
        }

        void display() {
            shape(particleShape);
        }

    }
}
