/**
 * MultipleColorTracking
 * Select 4 colors to track them separately
 *
 * It uses the OpenCV for Processing library by Greg Borenstein
 * https://github.com/atduskgreg/opencv-processing
 *
 * @author: Jordi Tost
 * @updated: 06/10/2014
 * 
 * University of Applied Sciences Potsdam, 2014
 *
 *
 * Instructions:
 * Press one numerical key [1-4] and click on one color to track it
 */
 
import java.util.*;
import gab.opencv.*;
import processing.video.*;
import java.awt.Rectangle;
import oscP5.*;
import netP5.*;

Capture video;
OpenCV opencv;
PImage src;

int maxColors = 4;

// List of my blob groups
BlobGroup[] blobGroups;
float[] volumes;
float fadingSpeed = 0.05;
float minVol = 0.2;

OscP5 osc;
NetAddress remote;

int colorToChange = -1;

void setup() {
    //size(830, 480, P2D);
    //video = new Capture(this, 640, 480, "USB 2.0 Camera");
    video = new Capture(this, 640, 480);
    opencv = new OpenCV(this, video.width, video.height);
    blobGroups = new BlobGroup[maxColors];
    volumes = new float[maxColors];

    size(opencv.width + opencv.width/4 + 30, opencv.height, P2D);
    
    video.start();

    // set up osc
    osc = new OscP5(this, 12000);
    remote = new NetAddress("127.0.0.1", 8000);
}

void draw() {
    
    background(150);
    
    if (video.available()) {
        video.read();
    }

    // <2> Load the new frame of our movie in to OpenCV
    opencv.loadImage(video);
    
    // Tell OpenCV to use color information
    opencv.useColor();
    src = opencv.getSnapshot();
    
    // <3> Tell OpenCV to work in HSV color space.
    opencv.useColor(HSB);
    
    image(src, 0, 0);

    for(int i = 0; i < blobGroups.length; i++) {
        if(blobGroups[i] != null) {
            blobGroups[i].detectBlobs(src);

            // draw reference image and rectangle
            noStroke();
            fill(blobGroups[i].colr);
            rect(src.width, i*src.height/4, 30, src.height/4);
            image(blobGroups[i].output, width-src.width/4, i*src.height/4, src.width/4, src.height/4);

            if(blobGroups[i].visible) {
                PVector[] positions = blobGroups[i].getFlatPositions();
                float[] velocities = blobGroups[i].getFlatVelocities();
                float distance = blobGroups[i].getDistance();
                //print("Position " + positions[0].x + "  " + positions[0].y + "  ---  Velocity " + velocities[0] + "  ---  Distance " + distance);

                // draw positions
                blobGroups[i].displayFlatPositions();


                if(volumes[i] < 1) {
                    volumes[i] = volumes[i] + fadingSpeed;
                } else {
                    volumes[i] = 1;
                }

                ///////////////////
                // SEND MESSAGES //
                ///////////////////

                for(int j=0; j<positions.length; j++) {
                    if(positions[j] != null) {
                        OscMessage oscPos = new OscMessage("/color-" + i + "/blob-" + j + "/position");
                        oscPos.add(positions[j].x);
                        oscPos.add(positions[j].y);
                        osc.send(oscPos, remote);
                    }
                }

                for(int j=0; j<velocities.length; j++) {
                    if(velocities[j] > 0) {
                        OscMessage oscPos = new OscMessage("/color-" + i + "/blob-" + j + "/velocity");
                        oscPos.add(velocities[j]);
                        osc.send(oscPos, remote);
                    }
                }

                if(distance >= 0) {
                    // send osc message for distance of two blobs
                    OscMessage oscDist = new OscMessage("/color-" + i + "/distance");
                    oscDist.add(distance);
                    osc.send(oscDist, remote);
                }
            } else {
                if(volumes[i] > minVol) {
                    volumes[i] = volumes[i] - fadingSpeed;
                } else {
                    volumes[i] = minVol;
                }
            }

            // send osc message for the volume/visibility of a color
            OscMessage oscVol = new OscMessage("/color-" + i + "/visibility");
            oscVol.add(volumes[i]);
            osc.send(oscVol, remote);
        }
    }

    // Show images
    
    // Print text if new color expected
    textSize(20);
    stroke(255);
    fill(255);
    
    if (colorToChange > -1) {
        text("click to change color " + colorToChange, 10, 25);
    } else {
        text("press key [1-4] to select color", 10, 25);
    }
}

float mapValue(float val, float v1Min, float v1Max, float v2Min, float v2Max) {
    float delta1 = v1Max - v1Min;
    float delta2 = v2Max - v2Min;

    float scaled = val - v1Min / delta1;
    return v2Min + (scaled * delta2);
}

//////////////////////
// Keyboard / Mouse
//////////////////////

void mousePressed() {
        
    if (colorToChange > -1) {
        
        color c = get(mouseX, mouseY);
        println("r: " + red(c) + " g: " + green(c) + " b: " + blue(c));
        int hue = int(map(hue(c), 0, 255, 0, 180));;

        blobGroups[colorToChange-1] = new BlobGroup(this, c);
        
        println("color index " + (colorToChange-1) + ", value: " + hue);
    }
}

void keyPressed() {
    
    if (key == '1') {
        colorToChange = 1;
        
    } else if (key == '2') {
        colorToChange = 2;
        
    } else if (key == '3') {
        colorToChange = 3;
        
    } else if (key == '4') {
        colorToChange = 4;
    }
}

void keyReleased() {
    colorToChange = -1; 
}