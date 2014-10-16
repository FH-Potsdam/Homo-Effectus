class BlobComparator implements Comparator<Blob> {
	int compare(Blob blob1, Blob blob2) {
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