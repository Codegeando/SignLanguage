package app.ssru.mrsmile.signlanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;


public class HandGesture {

	public List<MatOfPoint> contours = new ArrayList<>();
	public int cMaxId = -1;
	public Mat hie = new Mat();
	public List<MatOfPoint> hullP = new ArrayList<>();
	public MatOfInt hullI = new MatOfInt();
	public Rect boundingRect;
	public MatOfInt4 defects = new MatOfInt4();
	
	public ArrayList<Integer> defectIdAfter = new ArrayList<>();
	
	
	public List<Point> fingerTips = new ArrayList<>();
	public Map<Double, Point> fingerTipsOrdered = new TreeMap<>();
	
	public MatOfPoint2f defectMat = new MatOfPoint2f();
	public List<Point> defectPoints = new ArrayList<>();
	public Map<Double, Integer> defectPointsOrdered = new TreeMap<>();

	public MatOfPoint2f approxContour = new MatOfPoint2f();
	
	public Point inCircle = new Point();

	public double inCircleRadius;
	
	public List<Double> features = new ArrayList<>();
	
	private boolean isHand = false;
	
	public void findBiggestContour() {
		int idx = -1;
		int cNum = 0;
		
		for (int i = 0; i < contours.size(); i++) {
			int curNum = contours.get(i).toList().size();
			if (curNum > cNum) {
				idx = i;
				cNum = curNum;
			}
		}
		
		cMaxId = idx;
	}
	
	boolean detectIsHand( Mat img ) {

		int centerX = 0;
		int centerY = 0;
		if (boundingRect != null) {
			centerX = boundingRect.x + boundingRect.width/2;
			centerY = boundingRect.y + boundingRect.height/2;
		}

		if (cMaxId == -1) {
			isHand = false;
		} else if (boundingRect == null) {
			isHand = false;
		} else if ((boundingRect.height == 0)
					|| (boundingRect.width == 0)) {
			isHand = false;
		} else if ( (centerX < img.cols()/4)
					|| (centerX > img.cols()*3/4) ) {
			isHand = false;
		} else {
			isHand = true;
		}
		return isHand;
	}
	
	//Convert the feature indicated by label to the string used in SVM input file
	public String feature2SVMString(int label) {

		String ret = Integer.toString(label) + " ";
		int i;
		for (i = 0; i < features.size(); i++) {
			int id = i + 1;
			ret = ret + id + ":" + features.get(i) + " ";
		}
		ret = ret + "\n";
		return ret;
	}

	//Extract hand features from img 
	public String featureExtraction(Mat img, int label) {

		String ret = null;
		if ((detectIsHand(img))) {

			defectMat.fromList(defectPoints);

			List<Integer> dList = defects.toList();
			Point[] contourPts = contours.get(cMaxId).toArray();
			Point prevDefectVec = null;
			int i;
			for (i = 0; i < defectIdAfter.size(); i++) {
				int curDlistId = defectIdAfter.get(i);
				int curId = dList.get(curDlistId);

				Point curDefectPoint = contourPts[curId];
				Point curDefectVec = new Point();
				curDefectVec.x = curDefectPoint.x - inCircle.x;
				curDefectVec.y = curDefectPoint.y - inCircle.y;

				if (prevDefectVec != null) {
					double dotProduct = curDefectVec.x * prevDefectVec.x +
							curDefectVec.y * prevDefectVec.y;
					double crossProduct = curDefectVec.x * prevDefectVec.y -
							prevDefectVec.x * curDefectVec.y;

					if (crossProduct <= 0) {
						break;
					}
				}

				prevDefectVec = curDefectVec;

			}

			int startId = i;
			int countId = 0;

			ArrayList<Point> finTipsTemp = new ArrayList<>();

			if (defectIdAfter.size() > 0) {
				boolean end = false;

				for (int j = startId; ; j++) {
					if (j == defectIdAfter.size()) {

						if (end == false) {
							j = 0;
							end = true;
						} else{
							break;
						}
					}

					if ((j == startId) && (end == true)) {
						break;
					}

					int curDlistId = defectIdAfter.get(j);
					int curId = dList.get(curDlistId);

					Point curDefectPoint = contourPts[curId];
					Point fin0 = contourPts[dList.get(curDlistId - 2)];
					Point fin1 = contourPts[dList.get(curDlistId - 1)];
					finTipsTemp.add(fin0);
					finTipsTemp.add(fin1);

					//Valid defect point is stored in curDefectPoint
					Core.circle(img, curDefectPoint, 2, new Scalar(0, 0, 255), -5);

					countId++;
				}

			}

			int count = 0;
			features.clear();
			for (int fid = 0; fid < finTipsTemp.size(); ) {

				if (count > 5){
					break;
				}

				Point curFinPoint = finTipsTemp.get(fid);

				if ((fid % 2 == 0)) {

					if (fid != 0) {
						Point prevFinPoint = finTipsTemp.get(fid - 1);
						curFinPoint.x = (curFinPoint.x + prevFinPoint.x) / 2;
						curFinPoint.y = (curFinPoint.y + prevFinPoint.y) / 2;
					}

					if (fid == (finTipsTemp.size() - 2)) {
						fid++;
					}else {
						fid += 2;
					}

				} else {
					fid++;
				}

				Point disFinger = new Point(curFinPoint.x - inCircle.x, curFinPoint.y - inCircle.y);
				double dis = Math.sqrt(disFinger.x * disFinger.x + disFinger.y * disFinger.y);
				Double f1 = (disFinger.x) / inCircleRadius;
				Double f2 = (disFinger.y) / inCircleRadius;
				features.add(f1);
				features.add(f2);

				//curFinPoint stores the location of the finger tip
				Core.line( img, inCircle, curFinPoint, new Scalar(24, 77, 9), 2 );
				Core.circle( img, curFinPoint, 2, Scalar.all(0), -5 );

				Core.putText( img ,
							  Integer.toString(count) ,
							  new Point(curFinPoint.x - 10 ,
							  curFinPoint.y - 10) ,
						 	  Core.FONT_HERSHEY_SIMPLEX ,
							  0.5 ,
							  Scalar.all(0)
				);

				count++;

			}

			ret = feature2SVMString(label);
		}
		return ret;
	}

	
	// Find the location of inscribed circle and return the radius and the center location
	void findInscribedCircle(Mat img) {

        try {
            Point tl = boundingRect.tl();
            Point br = boundingRect.br();

            double[] cirx = new double[]{0};
            double[] ciry = new double[]{0};
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
}
