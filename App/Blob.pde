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
    void display() {
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
    void update(Contour newC) {
        
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
    void countDown() {
        timer--;
    }

    // I am deed, delete me
    boolean dead() {
        if (timer < 0) return true;
        return false;
    }
    
    public Rectangle getBoundingBox() {
        return contour.getBoundingBox();
    }


    void calculatePosition() {
        pPosition = position;

        Rectangle boundingBox = contour.getBoundingBox();
        float x = boundingBox.x + (boundingBox.width/2);
        float y = boundingBox.y + (boundingBox.height/2);

        position = new PVector(x, y);        
    }


    public PVector getPosition() {
        return position;
    }


    void calculateFlatPosition() {
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