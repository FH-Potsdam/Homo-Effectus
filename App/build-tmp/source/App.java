import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.*; 
import gab.opencv.*; 
import processing.video.*; 
import java.awt.Rectangle; 
import oscP5.*; 
import netP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class App extends PApplet {

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
 







Capture video;
OpenCV opencv;
PImage src;

int maxColors = 4;

// List of my blob groups
BlobGroup[] blobGroups;
float[] volumes;
float fadingSpeed = 0.05f;
float minVol = 0.2f;

OscP5 osc;
NetAddress remote;

int colorToChange = -1;

public void setup() {
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

public void draw() {
    
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

public float mapValue(float val, float v1Min, float v1Max, float v2Min, float v2Max) {
    float delta1 = v1Max - v1Min;
    float delta2 = v2Max - v2Min;

    float scaled = val - v1Min / delta1;
    return v2Min + (scaled * delta2);
}

//////////////////////
// Keyboard / Mouse
//////////////////////

public void mousePressed() {
        
    if (colorToChange > -1) {
        
        int c = get(mouseX, mouseY);
        println("r: " + red(c) + " g: " + green(c) + " b: " + blue(c));
        int hue = PApplet.parseInt(map(hue(c), 0, 255, 0, 180));;

        blobGroups[colorToChange-1] = new BlobGroup(this, c);
        
        println("color index " + (colorToChange-1) + ", value: " + hue);
    }
}

public void keyPressed() {
    
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

public void keyReleased() {
    colorToChange = -1; 
}
/**
 * Blob Class
 *
 * Based on this example by Daniel Shiffman:
 * http://shiffman.net/2011/04/26/opencv-matching-faces-over-time/
 * 
 * @author: Jordi Tost @jorditost
 * @modified: 06/10/2014
 * 
 * University of Applied Sciences Potsdam, 2014
 */

class Blob {
    
    private PApplet parent;
    
    // Contour
    public Contour contour;
    
    // Am I available to be matched?
    public boolean available;
    
    // Should I be deleted?
    public boolean delete;

    private PVector position;
    private PVector pPosition;
    private PVector flatPosition;
    private PVector pFlatPosition;
    private PVector velocity;
    private PVector flatVelocity;
    
    // How long should I live if I have disappeared?
    private int initTimer = 5; //127;
    public int timer;

    private int bufferSize = 6;
    private int bufferIndex = 0;
    private PVector[] buffer;
    
    // Unique ID for each blob
    int id;
    
    // Make me
    Blob(PApplet parent, int id, Contour c) {
        this.parent = parent;
        this.id = id;
        this.contour = new Contour(parent, c.pointMat);
        
        available = true;
        delete = false;
        
        timer = initTimer;
        Rectangle boundingBox = c.getBoundingBox();
        float x = boundingBox.x + (boundingBox.width/2);
        float y = boundingBox.y + (boundingBox.height/2);

        position = pPosition = flatPosition = pFlatPosition = new PVector(x, y);
        buffer = new PVector[bufferSize];
        buffer[bufferIndex] = position;
    }
    
    // Show me
    public void display() {
        Rectangle r = contour.getBoundingBox();
        
        float opacity = map(timer, 0, initTimer, 0, 127);
        fill(0,0,255,opacity);
        stroke(0,0,255);
        rect(r.x, r.y, r.width, r.height);
        fill(255,2*opacity);
        textSize(26);
        text(""+id, r.x+10, r.y+30);
    }

    // Give me a new contour for this blob (shape, points, location, size)
    // Oooh, it would be nice to lerp here!
    public void update(Contour newC) {
        
        contour = new Contour(parent, newC.pointMat);
        
        // Is there a way to update the contour's points without creating a new one?
        /*ArrayList<PVector> newPoints = newC.getPoints();
        Point[] inputPoints = new Point[newPoints.size()];
        
        for(int i = 0; i < newPoints.size(); i++){
            inputPoints[i] = new Point(newPoints.get(i).x, newPoints.get(i).y);
        }
        contour.loadPoints(inputPoints);*/

        // update position of bounding box
        calculatePosition();

        // update buffer
        bufferIndex++;

        if(bufferIndex >= bufferSize) {
            bufferIndex = 0;
        }
        buffer[bufferIndex] = position;

        calculateFlatPosition();
        calculateVelocity();
        calculateFlatVelocity();

        timer = initTimer;
    }

    // Count me down, I am gone
    public void countDown() {
        timer--;
    }

    // I am deed, delete me
    public boolean dead() {
        if (timer < 0) return true;
        return false;
    }
    
    public Rectangle getBoundingBox() {
        return contour.getBoundingBox();
    }


    public void calculatePosition() {
        pPosition = position;

        Rectangle boundingBox = contour.getBoundingBox();
        float x = boundingBox.x + (boundingBox.width/2);
        float y = boundingBox.y + (boundingBox.height/2);

        position = new PVector(x, y);        
    }


    public PVector getPosition() {
        return position;
    }


    public void calculateFlatPosition() {
        pFlatPosition = flatPosition;
        PVector sum = buffer[bufferIndex].get();
        int counter = 0;

        for(int i = 0; i < buffer.length; i++) {
            if(buffer[i] != null) {
                if(i != bufferIndex) {
                    sum.add(buffer[i]);    
                }
                counter++;
            }
        }

        sum.div(counter);

        flatPosition = sum;
    }


    public PVector getFlatPosition() {
        return flatPosition;
    }


    public void calculateVelocity() {
        velocity = PVector.sub(pPosition, position);
    }


    public PVector getVelocity() {
        return velocity;
    }


    public void calculateFlatVelocity() {
        PVector flatVelocity = PVector.sub(pFlatPosition, flatPosition);
        //println(flatVelocity.x + "  " + flatVelocity.y);
    }


    public PVector getFlatVelocity() {
        return flatVelocity;
    }
}
class BlobComparator implements Comparator<Blob> {
	public int compare(Blob blob1, Blob blob2) {
        Rectangle r1 = blob1.contour.getBoundingBox();
        Rectangle r2 = blob2.contour.getBoundingBox();
        float size1 = r1.width * r1.height;
        float size2 = r2.width * r2.height;

		if(size1 < size2)
			return 10;

		if(size1 > size2)
			return -10;

		return 0; 
	}
}
class BlobGroup {

    PApplet parent;
    private int hue;
    private int colr;
    private int numBlobs = 1;

    private int blobCount;

    private PImage output;

    private ArrayList<Contour> contours;

    private ArrayList<Contour> newBlobContours;

    private ArrayList<Blob> blobList;
    private ArrayList<Blob> biggestBlobs;

    private int loops = 0;
    private int srcWidth;
    private int srcHeight;
    private boolean visible = false;

    // settings
    private int rangeWidth = 10;
    private int blobSizeThreshold = 30;


    BlobGroup(PApplet parent, int colr) {
        this.parent = parent;
        this.colr = colr;
        this.hue = PApplet.parseInt(map(hue(colr), 0, 255, 0, 180));
        contours = new ArrayList<Contour>();
        blobList = new ArrayList<Blob>();
        biggestBlobs = new ArrayList<Blob>();
    }

    //////////////////////
    // Detect Functions
    //////////////////////

    /**
     * Returns blobs/contours from a list of contours which qualify as blob
     * i.e. which have a size bigger than blobSizeThreshold.
     *
     * @param src the source image which should be used as input for the detection
     * @return ArrayList<Contour> ArrayList of contours
     */
    public void detectBlobs(PImage src) {
        
        srcWidth = src.width;
        srcHeight = src.height;

        opencv.loadImage(src);
        opencv.useColor(HSB);

        // <4> Copy the Hue channel of our image into 
        //     the gray channel, which we process.
        opencv.setGray(opencv.getH().clone());       

        // <5> Filter the image based on the range of 
        //     hue values that match the object we want to track.
        opencv.inRange(hue - rangeWidth / 2, hue + rangeWidth / 2);
        
        //opencv.dilate();
        opencv.erode();

        // <6> Save the processed image for reference.
        output = opencv.getSnapshot();

        // Contours detected in this frame
        // Passing 'true' sorts them by descending area.
        contours = opencv.findContours(true, true);
        
        newBlobContours = getBlobsFromContours(contours);

        if(newBlobContours.size() == 0) {
            visible = false;
        } else {
            visible = true;
        }

        // Check if the detected blobs already exist are new or some has disappeared. 
        
        // SCENARIO 1 
        // blobList is empty
        if (blobList.isEmpty()) {
            // Just make a Blob object for every face Rectangle
            for (int i = 0; i < newBlobContours.size(); i++) {
                println("+++ New blob detected with ID: " + blobCount);
                blobList.add(new Blob(parent, blobCount, newBlobContours.get(i)));
                blobCount++;
            }
        
        // SCENARIO 2 
        // We have fewer Blob objects than face Rectangles found from OpenCV in this frame
        } else if (blobList.size() <= newBlobContours.size()) {
            boolean[] used = new boolean[newBlobContours.size()];
            // Match existing Blob objects with a Rectangle
            for (Blob b : blobList) {
                 // Find the new blob newBlobContours.get(index) that is closest to blob b
                 // set used[index] to true so that it can't be used twice
                 float record = 50000;
                 int index = -1;
                 for (int i = 0; i < newBlobContours.size(); i++) {
                     float d = dist(newBlobContours.get(i).getBoundingBox().x, newBlobContours.get(i).getBoundingBox().y, b.getBoundingBox().x, b.getBoundingBox().y);
                     //float d = dist(blobs[i].x, blobs[i].y, b.r.x, b.r.y);
                     if (d < record && !used[i]) {
                         record = d;
                         index = i;
                     } 
                 }
                 // Update Blob object location
                 used[index] = true;
                 b.update(newBlobContours.get(index));
            }
            // Add any unused blobs
            for (int i = 0; i < newBlobContours.size(); i++) {
                if (!used[i]) {
                    println("+++ New blob detected with ID: " + blobCount);
                    blobList.add(new Blob(parent, blobCount, newBlobContours.get(i)));
                    //blobList.add(new Blob(blobCount, blobs[i].x, blobs[i].y, blobs[i].width, blobs[i].height));
                    blobCount++;
                }
            }
        
        // SCENARIO 3 
        // We have more Blob objects than blob Rectangles found from OpenCV in this frame
        } else {
            // All Blob objects start out as available
            for (Blob b : blobList) {
                b.available = true;
            } 
            // Match Rectangle with a Blob object
            for (int i = 0; i < newBlobContours.size(); i++) {
                // Find blob object closest to the newBlobContours.get(i) Contour
                // set available to false
                float record = 50000;
                int index = -1;
                for (int j = 0; j < blobList.size(); j++) {
                    Blob b = blobList.get(j);
                    float d = dist(newBlobContours.get(i).getBoundingBox().x, newBlobContours.get(i).getBoundingBox().y, b.getBoundingBox().x, b.getBoundingBox().y);
                    //float d = dist(blobs[i].x, blobs[i].y, b.r.x, b.r.y);
                    if (d < record && b.available) {
                        record = d;
                        index = j;
                    }
                }
                // Update Blob object location
                Blob b = blobList.get(index);
                b.available = false;
                b.update(newBlobContours.get(i));
            } 
            // Start to kill any left over Blob objects
            for (Blob b : blobList) {
                if (b.available) {
                    b.countDown();
                    if (b.dead()) {
                        b.delete = true;
                    } 
                }
            } 
        }
        
        // Delete any blob that should be deleted
        for (int i = blobList.size()-1; i >= 0; i--) {
            Blob b = blobList.get(i);
            if (b.delete) {
                blobList.remove(i);
            } 
        }

        Collections.sort(blobList, new BlobComparator());
    }

    /**
     * Returns blobs/contours from a list of contours which qualify as blob
     * i.e. which have a size bigger than blobSizeThreshold.
     *
     * @return      ArrayList of contours
     */
    public ArrayList<Contour> getBlobsFromContours(ArrayList<Contour> newContours) {
        
        ArrayList<Contour> newBlobs = new ArrayList<Contour>();
        
        // Which of these contours are blobs?
        for (int i=0; i<newContours.size(); i++) {
            
            Contour contour = newContours.get(i);
            Rectangle r = contour.getBoundingBox();
            
            if (//(contour.area() > 0.9 * src.width * src.height) ||
                    (r.width < blobSizeThreshold || r.height < blobSizeThreshold))
                continue;
            
            newBlobs.add(contour);
        }
        
        return newBlobs;
    }


    /**
     * Returns the distance between the two biggest blobs. The distance is returned as
     * number relative to the maximal distance (screen diagonal)
     * hence it is a number between 0 and 1.
     *
     * @return      Distance between biggest Blobs relative to window size
     */

    public float getDistance() {
        if(blobList.size() > 1) {
            Contour contour1 = blobList.get(0).contour;
            Contour contour2 = blobList.get(1).contour;
            float maxDist = sqrt(sq(src.width) + sq(src.height));

            Rectangle r1 = contour1.getBoundingBox();
            Rectangle r2 = contour2.getBoundingBox();

            float d = dist(r1.x + r1.width/2, r1.y + r1.height/2, r2.x + r2.width/2, r2.y + r2.height/2);
            d = map(d, 0, maxDist, 0, 1);
            return d;
        }

        return -1;
    }


    /**
     * Returns the distance between the two biggest blobs. The distance is returned as
     * number relative to the maximal distance (screen diagonal)
     * hence it is a number between 0 and 1.
     *
     * @return      Position
     */

    public PVector[] getPositions() {
        PVector[] positions = new PVector[numBlobs];

        for(int i=0; i<blobList.size(); i++) {
            if(i >= numBlobs)
                break;

            positions[i] = blobList.get(i).getPosition().get();
            positions[i].x = positions[i].x / srcWidth;
            positions[i].y = positions[i].y / srcHeight;
        }

        return positions;
    }

    public PVector[] getFlatPositions() {
        PVector[] positions = new PVector[blobList.size()];
        if(visible) {
            
            for(int i=0; i<blobList.size(); i++) {
                if(i >= numBlobs)
                    break;

                positions[i] = blobList.get(i).getFlatPosition().get();
                positions[i].x = positions[i].x / srcWidth;
                positions[i].y = positions[i].y / srcHeight;
            }
        }
        return positions;
    }

    public float[] getVelocities(/*int numBlobs*/) {
        PVector[] velocities = new PVector[numBlobs];
        float[] v = new float[numBlobs];

        for(int i=0; i<blobList.size(); i++) {
            if(i >= numBlobs)
                break;
            if(blobList.get(i).getVelocity() != null) {
                velocities[i] = blobList.get(i).getVelocity().get();
                velocities[i].x = velocities[i].x / srcWidth;
                velocities[i].y = velocities[i].y / srcHeight;
                v[i] = velocities[i].mag();
            }
        }

        return v;      
    }


    public float[] getFlatVelocities(/*int numBlobs*/) {
        PVector[] velocities = new PVector[numBlobs];
        float[] v = new float[numBlobs];

        for(int i=0; i<blobList.size(); i++) {
            if(i >= numBlobs)
                break;

            if(blobList.get(i).getFlatVelocity() != null) {
                velocities[i] = blobList.get(i).getFlatVelocity().get();

/*                velocities[i].x = velocities[i].x / srcWidth;
                velocities[i].y = velocities[i].y / srcHeight;*/
                //v[i] = velocities[i].mag();
            }
        }

        return v;      
    }


    /**
     * Render the boundingboxes of the biggest blobs. number of blobs rendered is 
     * specified by numBlobs
     */

    public void displayBoundingBoxes() {
        for(int i=0; i<blobList.size(); i++) {
            if(i >= numBlobs)
                break;

            Contour contour = blobList.get(i).contour;
            Rectangle r = contour.getBoundingBox();  
            
            stroke(colr);
            fill(colr, 150);
            strokeWeight(2);
            rect(r.x, r.y, r.width, r.height);
        }
    }

    public void displayPositions() {
        for(int i=0; i<blobList.size(); i++) {
            if(i >= numBlobs)
                break;

            PVector position = blobList.get(i).getPosition(); 
            
            stroke(255, 255, 255);
            fill(colr);
            strokeWeight(2);
            ellipse(position.x, position.y, 10, 10);
        }
    }

    public void displayFlatPositions() {
        for(int i=0; i<blobList.size(); i++) {
            if(i >= numBlobs)
                break;

            PVector position = blobList.get(i).getFlatPosition();
                     
            stroke(255, 255, 255);
            fill(colr);
            strokeWeight(2);
            ellipse(position.x, position.y, 10, 10);
        }
    }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "App" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
