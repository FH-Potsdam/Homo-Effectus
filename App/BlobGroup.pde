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
        this.hue = int(map(hue(colr), 0, 255, 0, 180));
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
    ArrayList<Contour> getBlobsFromContours(ArrayList<Contour> newContours) {
        
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

    float getDistance() {
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

    PVector[] getPositions() {
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

    PVector[] getFlatPositions() {
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

    float[] getVelocities(/*int numBlobs*/) {
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


    float[] getFlatVelocities(/*int numBlobs*/) {
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

    void displayBoundingBoxes() {
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

    void displayPositions() {
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

    void displayFlatPositions() {
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