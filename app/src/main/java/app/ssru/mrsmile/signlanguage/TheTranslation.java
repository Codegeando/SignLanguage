package app.ssru.mrsmile.signlanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.lang.Math;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;


public class TheTranslation extends Activity implements CvCameraViewListener2{

    //Just for debugging
    private static final String TAG = "Translation :::: ";

    //Color Space used for hand segmentation
    private static final int COLOR_SPACE = Imgproc.COLOR_RGB2Lab;

    //Number of frames collected for each gesture in the training set
    private static final int GES_FRAME_MAX= 10;

    public final Object sync = new Object();

    //Mode that presamples hand colors
    public static final int SAMPLE_MODE = 0;

    //Mode that generates binary image
    public static final int DETECTION_MODE = 1;

    //Mode that displays color image together with contours, fingertips,
    //defect points and so on.
    public static final int TRAIN_REC_MODE = 2;

    //Mode that presamples background colors
    public static final int BACKGROUND_MODE = 3;

    //Mode that is started when user clicks the 'Add Gesture' button.
    public static final int ADD_MODE = 4;

    //Mode that is started when user clicks the 'Test' button.
    public static final int TEST_MODE = 5;

    //Mode that is started when user clicks 'App Test' in the menu.
    public static final int APP_TEST_MODE = 6;

    //Number of frames used for prediction
    private static final int FRAME_BUFFER_NUM = 1;

    //Frame interval between two launching events
    private static final int APP_TEST_DELAY_NUM = 10;

    private int appTestFrameCount = 0;

    private float[][] values = new float[FRAME_BUFFER_NUM][];
    private int[][] indices = new int[FRAME_BUFFER_NUM][];

    private Handler mHandler = new Handler();


    private MyCameraView mOpenCvCameraView;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;


    private List<android.hardware.Camera.Size> mResolutionList;

    //Initial mode is BACKGROUND_MODE to presample the colors of the hand
    private int mode = BACKGROUND_MODE;

    private static final int SAMPLE_NUM = 7;


    private Point[][] samplePoints = null;
    private double[][] avgColor = null;
    private double[][] avgBackColor = null;

    private ArrayList<ArrayList<Double>> averChans = new ArrayList<>();

    private double[][] cLower = new double[SAMPLE_NUM][3];
    private double[][] cUpper = new double[SAMPLE_NUM][3];
    private double[][] cBackLower = new double[SAMPLE_NUM][3];
    private double[][] cBackUpper = new double[SAMPLE_NUM][3];

    private Scalar lowerBound = new Scalar(0, 0, 0);
    private Scalar upperBound = new Scalar(0, 0, 0);
    private int squareLen;

    private Mat sampleColorMat = null;
    private List<Mat> sampleColorMats = null;

    private Mat[] sampleMats = null ;

    private Mat rgbaMat = null;

    private Mat rgbMat = null;
    private Mat bgrMat = null;


    private Mat interMat = null;

    private Mat binMat = null;
    private Mat binTmpMat = null;
    private Mat binTmpMat2 = null;
    private Mat binTmpMat0 = null;
    private Mat binTmpMat3 = null;

    private Mat tmpMat = null;
    private Mat backMat = null;
    private Mat difMat = null;
    private Mat binDifMat = null;


    private Scalar mColorsRGB[] = null;

    //Stores all the information about the hand
    private HandGesture hg = null;

    private int gesFrameCount;
    private int curLabel = 0;

    //Stores string representation of features to be written to train_data.txt
    private ArrayList<String> feaStrs = new ArrayList<>();


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("Android Tutorial", "OpenCV loaded successfully");

                    mOpenCvCameraView.enableView();

                    mOpenCvCameraView.setOnTouchListener(new OnTouchListener() {

                        //Called when user touch the view screen
                        //Mode flow: BACKGROUND_MODE --> SAMPLE_MODE --> DETECTION_MODE <--> TRAIN_REC_MODE
                        public boolean onTouch(View v, MotionEvent event) {
                            // ... Respond to touch events
                            int action = MotionEventCompat.getActionMasked(event);

                            switch(action) {
                                case (MotionEvent.ACTION_DOWN) :
                                    Log.d(TAG,"Action was DOWN");
                                    String toastStr = null;
                                    if (mode == SAMPLE_MODE) {
                                        mode = DETECTION_MODE;
                                        toastStr = "Sampling Finished!";
                                    } else if (mode == DETECTION_MODE) {
                                        mode = TRAIN_REC_MODE;
                                        toastStr = "Binary Display Finished!";
                                    } else if (mode == TRAIN_REC_MODE){
                                        mode = DETECTION_MODE;
                                        toastStr = "train finished!";
                                    } else if (mode == BACKGROUND_MODE) {
                                        toastStr = "First background sampled!";
                                        rgbaMat.copyTo(backMat);
                                        mode = SAMPLE_MODE;
                                    }

                                    Toast.makeText(getApplicationContext(), toastStr, Toast.LENGTH_LONG).show();
                                    return false;
                                case (MotionEvent.ACTION_MOVE) :
                                    Log.d(TAG,"Action was MOVE");
                                    return true;
                                case (MotionEvent.ACTION_UP) :
                                    Log.d(TAG,"Action was UP");
                                    return true;
                                case (MotionEvent.ACTION_CANCEL) :
                                    Log.d(TAG,"Action was CANCEL");
                                    return true;
                                case (MotionEvent.ACTION_OUTSIDE) :
                                    Log.d(TAG,"Movement occurred outside bounds " +
                                            "of current screen element");
                                    return true;
                                default :
                                    return true;
                            }
                        }
                    });
                } break;
                default: {
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_the_translation);

        mOpenCvCameraView = (MyCameraView) findViewById(R.id.HandGestureApp);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        samplePoints = new Point[SAMPLE_NUM][2];
        for (int i = 0; i < SAMPLE_NUM; i++){
            for (int j = 0; j < 2; j++) {
                samplePoints[i][j] = new Point();
            }
        }
        avgColor = new double[SAMPLE_NUM][3];
        avgBackColor = new double[SAMPLE_NUM][3];

        for (int i = 0; i < 3; i++)
            averChans.add(new ArrayList<Double>());

        //Lab
        initCLowerUpper(50, 50, 10, 10, 10, 10);
        initCBackLowerUpper(50, 50, 3, 3, 3, 3);

        Log.i(TAG, "Created!");
    }

    //Things triggered by clicking any items in the menu start here
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        switch (item.getItemId()) {
            case R.id.action_save:
                //isPictureSaved = true;
                return true;
            case R.id.data_collection:
                return true;
            case R.id.map_apps:
                return true;
            case R.id.app_test:
                if (mode == APP_TEST_MODE) {
                    mode = TRAIN_REC_MODE;
                    Toast.makeText(getApplicationContext(), "App testing ends!", Toast.LENGTH_LONG).show();
                } else {
                    mode = APP_TEST_MODE;
                    Toast.makeText(getApplicationContext(), "App testing begins!", Toast.LENGTH_LONG).show();
                    appTestFrameCount = 0;
                }
                return true;
        }

        int groupId = item.getGroupId();

        if (item.getGroupId() == 2) {
            int id = item.getItemId();
            Camera.Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    //Just initialize boundaries of the first sample
    private void initCLowerUpper( double cl1, double cu1, double cl2,
                                  double cu2, double cl3, double cu3 ) {

        cLower[0][0] = cl1;
        cUpper[0][0] = cu1;
        cLower[0][1] = cl2;
        cUpper[0][1] = cu2;
        cLower[0][2] = cl3;
        cUpper[0][2] = cu3;

    }

    private void initCBackLowerUpper( double cl1, double cu1, double cl2,
                                      double cu2, double cl3, double cu3 ) {

        cBackLower[0][0] = cl1;
        cBackUpper[0][0] = cu1;
        cBackLower[0][1] = cl2;
        cBackUpper[0][1] = cu2;
        cBackLower[0][2] = cl3;
        cBackUpper[0][2] = cu3;

    }

    //Initialize menu and resolution list.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Camera.Size> resolutionItr = mResolutionList.listIterator();
        int idx = 0;
        while(resolutionItr.hasNext()) {
            Camera.Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
        }
        checkCameraParameters();
        return true;
    }

    private void checkCameraParameters() {
        if (mOpenCvCameraView.isAutoWhiteBalanceLockSupported()) {

            if (mOpenCvCameraView.getAutoWhiteBalanceLock()) {

                Log.d("AutoWhiteBalanceLock", "Locked");

            } else {

                Log.d("AutoWhiteBalanceLock", "Not Locked");
                mOpenCvCameraView.setAutoWhiteBalanceLock(true);

                if (mOpenCvCameraView.getAutoWhiteBalanceLock()) {
                    Log.d("AutoWhiteBalanceLock", "Locked");
                }

            }

        } else {
            Log.d("AutoWhiteBalanceLock", "Not Supported");
        }
    }

    private void releaseCVMats() {

        releaseCVMat( sampleColorMat );
        sampleColorMat = null;

        if ( sampleColorMats != null ) {
            for (int i = 0; i < sampleColorMats.size(); i++) {
                releaseCVMat(sampleColorMats.get(i));
            }
        }

        sampleColorMats = null;

        if (sampleMats != null) {
            for (int i = 0; i < sampleMats.length; i++) {
                releaseCVMat(sampleMats[i]);
            }
        }

        sampleMats = null;

        releaseCVMat(rgbMat);
        rgbMat = null;

        releaseCVMat(bgrMat);
        bgrMat = null;

        releaseCVMat(interMat);
        interMat = null;

        releaseCVMat(binMat);
        binMat = null;

        releaseCVMat(binTmpMat0);
        binTmpMat0 = null;

        releaseCVMat(binTmpMat3);
        binTmpMat3 = null;

        releaseCVMat(binTmpMat2);
        binTmpMat2 = null;

        releaseCVMat(tmpMat);
        tmpMat = null;

        releaseCVMat(backMat);
        backMat = null;

        releaseCVMat(difMat);
        difMat = null;

        releaseCVMat(binDifMat);
        binDifMat = null;
    }

    private void releaseCVMat( Mat img ) {
        if (img != null) {
            img.release();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // TODO Auto-generated method stub
        Log.i(TAG, "On camera view started!");

        if (isNullValue(sampleColorMat)) {
            sampleColorMat = new Mat();
        }

        if (isNullValue(sampleColorMats)) {
            sampleColorMats = new ArrayList<>();
        }

        if (isNullValue(sampleMats)) {
            sampleMats = new Mat[SAMPLE_NUM];
            for (int i = 0; i < SAMPLE_NUM; i++) {
                sampleMats[i] = new Mat();
            }
        }

        if (isNullValue(rgbMat)) {
            rgbMat = new Mat();
        }

        if (isNullValue(bgrMat)) {
            bgrMat = new Mat();
        }

        if (isNullValue(interMat)) {
            interMat = new Mat();
        }

        if (isNullValue(binMat)) {
            binMat = new Mat();
        }

        if (isNullValue(binTmpMat)) {
            binTmpMat = new Mat();
        }

        if (isNullValue(binTmpMat2)) {
            binTmpMat2 = new Mat();
        }

        if (isNullValue(binTmpMat0)) {
            binTmpMat0 = new Mat();
        }

        if (isNullValue(binTmpMat3)) {
            binTmpMat3 = new Mat();
        }

        if (isNullValue(tmpMat)) {
            tmpMat = new Mat();
        }

        if (isNullValue(backMat)) {
            backMat = new Mat();
        }

        if (isNullValue(difMat)) {
            difMat = new Mat();
        }

        if (isNullValue(binDifMat)) {
            binDifMat = new Mat();
        }

        if (isNullValue(hg)) {
            hg = new HandGesture();
        }

        mColorsRGB = new Scalar[] {
                        new Scalar(255, 0, 0, 255),
                            new Scalar(0, 255, 0, 255),
                                new Scalar(0, 0, 255, 255)
        };

    }

    private boolean isNullValue(Object object) {
        return object == null;
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "On camera view stopped!");
        releaseCVMats();
    }

    //Called when each frame data gets received
    //inputFrame contains the data for each frame
    //Mode flow: BACKGROUND_MODE --> SAMPLE_MODE --> DETECTION_MODE <--> TRAIN_REC_MODE
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        rgbaMat = inputFrame.rgba();

        Imgproc.GaussianBlur(rgbaMat, rgbaMat, new Size(5,5), 5, 5);

        Imgproc.cvtColor(rgbaMat, rgbMat, Imgproc.COLOR_RGBA2RGB);

        //Convert original RGB colorspace to the colorspace indicated by COLR_SPACE
        Imgproc.cvtColor(rgbaMat, interMat, COLOR_SPACE);

        if (mode == SAMPLE_MODE) { //Second mode which presamples the colors of the hand
            preSampleHand(rgbaMat);
        } else if (mode == DETECTION_MODE) { //Third mode which generates the binary image containing the
            //segmented hand represented by white color
            produceBinImg(interMat, binMat);
            return binMat;
        } else if ((mode == TRAIN_REC_MODE)
                    ||(mode == ADD_MODE)
                        || (mode == TEST_MODE)
                            || (mode == APP_TEST_MODE)){

            produceBinImg(interMat, binMat);

            try {
                makeContours();
            } catch (Exception e) {

            }


            String entry = hg.featureExtraction(rgbaMat, curLabel);

            //Collecting the frame data of a certain gesture and storing it in the file train_data.txt.
            //This mode stops when the number of frames processed equals GES_FRAME_MAX
            if (mode == ADD_MODE) {
                gesFrameCount++;
                Core.putText(rgbaMat, Integer.toString(gesFrameCount), new Point(10,
                        10), Core.FONT_HERSHEY_SIMPLEX, 0.6, Scalar.all(0));

                feaStrs.add(entry);

                if (gesFrameCount == GES_FRAME_MAX) {

                    Runnable runnableShowBeforeAdd = new Runnable() {
                        @Override
                        public void run() {
                            {
                                /*showDialogBeforeAdd("Add or not", "Add this new gesture labeled as "
                                        + curLabel + "?");*/
                            }
                        }
                    };
                    mHandler.post(runnableShowBeforeAdd);
                    try {
                        synchronized(sync) {
                            sync.wait();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mode = TRAIN_REC_MODE;
                }
            } else if ((mode == TEST_MODE)||(mode == APP_TEST_MODE)) {
                Double[] doubleValue = hg.features.toArray(new Double[hg.features.size()]);
                values[0] = new float[doubleValue.length];
                indices[0] = new int[doubleValue.length];

                for (int i = 0; i < doubleValue.length; i++) {
                    values[0][i] = (float)(doubleValue[i]*1.0f);
                    indices[0][i] = i+1;
                }

                int[] returnedLabel = {0};
                double[] returnedProb = {0.0};

                //Predicted labels are stored in returnedLabel
                //Since currently prediction is made for each frame, only returnedLabel[0] is useful.
                int r = 0;//doClassificationNative(values, indices, isProb, modelFile, returnedLabel, returnedProb);

                if (r == 0) {
                    if (mode == TEST_MODE)
                        Core.putText(rgbaMat, Integer.toString(returnedLabel[0]), new Point(15,
                                15), Core.FONT_HERSHEY_SIMPLEX, 0.6, mColorsRGB[0]);
                    else if (mode == APP_TEST_MODE) { //Launching other apps
                        Core.putText(rgbaMat, Integer.toString(returnedLabel[0]), new Point(15,
                                15), Core.FONT_HERSHEY_SIMPLEX, 0.6, mColorsRGB[2]);

                        if (returnedLabel[0] != 0) {
                            if (appTestFrameCount == APP_TEST_DELAY_NUM) {
                                //Call other apps according to the predicted label
                                //This is done every APP_TEST_DELAY_NUM frames
                                //callAppByLabel(returnedLabel[0]);
                            } else {
                                appTestFrameCount++;
                            }
                        }
                    }
                }
            }
        } else if (mode == BACKGROUND_MODE) { //First mode which presamples background colors
            preSampleBack(rgbaMat);
        }
        return rgbaMat;
    }

    //Presampling hand colors.
    //Output is avgColor, which is essentially a 7 by 3 matrix storing the colors sampled by seven squares
    private void preSampleHand(Mat img) {
        int cols = img.cols();
        int rows = img.rows();
        squareLen = rows/20;
        Scalar color = mColorsRGB[2];  //Blue Outline

        samplePoints[0][0].x = cols/2;
        samplePoints[0][0].y = rows/4;
        samplePoints[1][0].x = cols*5/12;
        samplePoints[1][0].y = rows*5/12;
        samplePoints[2][0].x = cols*7/12;
        samplePoints[2][0].y = rows*5/12;
        samplePoints[3][0].x = cols/2;
        samplePoints[3][0].y = rows*7/12;
        samplePoints[4][0].x = cols/1.5;
        samplePoints[4][0].y = rows*7/12;
        samplePoints[5][0].x = cols*4/9;
        samplePoints[5][0].y = rows*3/4;
        samplePoints[6][0].x = cols*5/9;
        samplePoints[6][0].y = rows*3/4;

        for (int i = 0; i < SAMPLE_NUM; i++) {
            samplePoints[i][1].x = samplePoints[i][0].x+squareLen;
            samplePoints[i][1].y = samplePoints[i][0].y+squareLen;
        }
        for (int i = 0; i < SAMPLE_NUM; i++) {
            Core.rectangle(img,  samplePoints[i][0], samplePoints[i][1], color, 1);
        }
        for (int i = 0; i < SAMPLE_NUM; i++) {
            for (int j = 0; j < 3; j++) {
                avgColor[i][j] = (interMat.get((int)(samplePoints[i][0].y+squareLen/2), (int)(samplePoints[i][0].x+squareLen/2)))[j];
            }
        }
    }

    //Presampling background colors.
    //Output is avgBackColor, which is essentially a 7 by 3 matrix storing the colors sampled by seven squares
    private void preSampleBack(Mat img) {
        int cols = img.cols();
        int rows = img.rows();
        squareLen = rows/20;
        Scalar color = mColorsRGB[2];  //Blue Outline

        samplePoints[0][0].x = cols/6;
        samplePoints[0][0].y = rows/3;
        samplePoints[1][0].x = cols/6;
        samplePoints[1][0].y = rows*2/3;
        samplePoints[2][0].x = cols/2;
        samplePoints[2][0].y = rows/6;
        samplePoints[3][0].x = cols/2;
        samplePoints[3][0].y = rows/2;
        samplePoints[4][0].x = cols/2;
        samplePoints[4][0].y = rows*5/6;
        samplePoints[5][0].x = cols*5/6;
        samplePoints[5][0].y = rows/3;
        samplePoints[6][0].x = cols*5/6;
        samplePoints[6][0].y = rows*2/3;

        for (int i = 0; i < SAMPLE_NUM; i++) {
            samplePoints[i][1].x = samplePoints[i][0].x+squareLen;
            samplePoints[i][1].y = samplePoints[i][0].y+squareLen;
        }
        for (int i = 0; i < SAMPLE_NUM; i++){
            Core.rectangle(img,  samplePoints[i][0], samplePoints[i][1], color, 1);
        }
        for (int i = 0; i < SAMPLE_NUM; i++) {
            for (int j = 0; j < 3; j++) {
                avgBackColor[i][j] = (interMat.get((int)(samplePoints[i][0].y+squareLen/2), (int)(samplePoints[i][0].x+squareLen/2)))[j];
            }
        }

    }

    private void boundariesCorrection() {

        for (int i = 1; i < SAMPLE_NUM; i++) {
            for (int j = 0; j < 3; j++) {
                cLower[i][j] = cLower[0][j];
                cUpper[i][j] = cUpper[0][j];

                cBackLower[i][j] = cBackLower[0][j];
                cBackUpper[i][j] = cBackUpper[0][j];
            }
        }

        for (int i = 0; i < SAMPLE_NUM; i++) {
            for (int j = 0; j < 3; j++) {

                if (avgColor[i][j] - cLower[i][j] < 0) {
                    cLower[i][j] = avgColor[i][j];
                }

                if (avgColor[i][j] + cUpper[i][j] > 255) {
                    cUpper[i][j] = 255 - avgColor[i][j];
                }
                if (avgBackColor[i][j] - cBackLower[i][j] < 0){
                    cBackLower[i][j] = avgBackColor[i][j];
                }

                if (avgBackColor[i][j] + cBackUpper[i][j] > 255){
                    cBackUpper[i][j] = 255 - avgBackColor[i][j];
                }
            }
        }
    }

    void adjustBoundingBox(Rect initRect, Mat img) {

    }

    //Generates binary image containing user's hand
    void produceBinImg(Mat imgIn, Mat imgOut) {

        int colNum = imgIn.cols();
        int rowNum = imgIn.rows();
        int boxExtension = 0;

        boundariesCorrection();

        produceBinHandImg(imgIn, binTmpMat);

        produceBinBackImg(imgIn, binTmpMat2);

        Core.bitwise_and(binTmpMat, binTmpMat2, binTmpMat);
        binTmpMat.copyTo(tmpMat);
        binTmpMat.copyTo(imgOut);

        Rect roiRect = makeBoundingBox(tmpMat);
        adjustBoundingBox(roiRect, binTmpMat);

        if ( roiRect != null ) {
            roiRect.x = Math.max(0, roiRect.x - boxExtension);
            roiRect.y = Math.max(0, roiRect.y - boxExtension);
            roiRect.width = Math.min(roiRect.width+boxExtension, colNum);
            roiRect.height = Math.min(roiRect.height+boxExtension, rowNum);
            Mat roi1 = new Mat(binTmpMat, roiRect);
            Mat roi3 = new Mat(imgOut, roiRect);
            imgOut.setTo(Scalar.all(0));

            roi1.copyTo(roi3);

            Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
            Imgproc.dilate(roi3, roi3, element, new Point(-1, -1), 2);

            Imgproc.erode(roi3, roi3, element, new Point(-1, -1), 2);
        }

    }

    //Generates binary image thresholded only by sampled hand colors
    private void produceBinHandImg(Mat imgIn, Mat imgOut){
        for (int i = 0; i < SAMPLE_NUM; i++) {
            lowerBound.set(new double[]{avgColor[i][0]-cLower[i][0], avgColor[i][1]-cLower[i][1],
                    avgColor[i][2]-cLower[i][2]});
            upperBound.set(new double[]{avgColor[i][0]+cUpper[i][0], avgColor[i][1]+cUpper[i][1],
                    avgColor[i][2]+cUpper[i][2]});
            Core.inRange(imgIn, lowerBound, upperBound, sampleMats[i]);
        }

        imgOut.release();
        sampleMats[0].copyTo(imgOut);
        for (int i = 1; i < SAMPLE_NUM; i++) {
            Core.add(imgOut, sampleMats[i], imgOut);
        }
        Imgproc.medianBlur(imgOut, imgOut, 3);
    }

    //Generates binary image thresholded only by sampled background colors
    private void produceBinBackImg(Mat imgIn, Mat imgOut) {
        for (int i = 0; i < SAMPLE_NUM; i++) {

            lowerBound.set(new double[]{avgBackColor[i][0]-cBackLower[i][0], avgBackColor[i][1]-cBackLower[i][1],
                    avgBackColor[i][2]-cBackLower[i][2]});
            upperBound.set(new double[]{avgBackColor[i][0]+cBackUpper[i][0], avgBackColor[i][1]+cBackUpper[i][1],
                    avgBackColor[i][2]+cBackUpper[i][2]});
            Core.inRange(imgIn, lowerBound, upperBound, sampleMats[i]);
        }

        imgOut.release();
        sampleMats[0].copyTo(imgOut);

        for (int i = 1; i < SAMPLE_NUM; i++) {
            Core.add(imgOut, sampleMats[i], imgOut);
        }

        Core.bitwise_not(imgOut, imgOut);

        Imgproc.medianBlur(imgOut, imgOut, 7);
    }

   private void makeContours() {

        try {
            hg.contours.clear();
            Imgproc.findContours(binMat, hg.contours, hg.hie, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            //Find biggest contour and return the index of the contour, which is hg.cMaxId
            hg.findBiggestContour();

            if (hg.cMaxId > -1) {

                hg.approxContour.fromList(hg.contours.get(hg.cMaxId).toList());
                Imgproc.approxPolyDP(hg.approxContour, hg.approxContour, 2, true);
                hg.contours.get(hg.cMaxId).fromList(hg.approxContour.toList());

                //hg.contours.get(hg.cMaxId) represents the contour of the hand
                Imgproc.drawContours(rgbaMat, hg.contours, hg.cMaxId, mColorsRGB[0], 1);

                //Palm center is stored in hg.inCircle, radius of the inscribed circle is stored in hg.inCircleRadius
                hg.findInscribedCircle(rgbaMat);


                hg.boundingRect = Imgproc.boundingRect(hg.contours.get(hg.cMaxId));

                Imgproc.convexHull(hg.contours.get(hg.cMaxId), hg.hullI, false);

                hg.hullP.clear();
                for (int i = 0; i < hg.contours.size(); i++)
                    hg.hullP.add(new MatOfPoint());

                int[] cId = hg.hullI.toArray();
                List<Point> lp = new ArrayList<Point>();
                Point[] contourPts = hg.contours.get(hg.cMaxId).toArray();

                for (int i = 0; i < cId.length; i++) {
                    lp.add(contourPts[cId[i]]);
                    //Core.circle(rgbaMat, contourPts[cId[i]], 2, new Scalar(241, 247, 45), -3);
                }

                //hg.hullP.get(hg.cMaxId) returns the locations of the points in the convex hull of the hand
                hg.hullP.get(hg.cMaxId).fromList(lp);
                lp.clear();

                hg.fingerTips.clear();
                hg.defectPoints.clear();
                hg.defectPointsOrdered.clear();

                hg.fingerTipsOrdered.clear();
                hg.defectIdAfter.clear();

                if ((contourPts.length >= 5)
                        && hg.detectIsHand(rgbaMat) && (cId.length >=5)){
                    Imgproc.convexityDefects(hg.contours.get(hg.cMaxId), hg.hullI, hg.defects);
                    List<Integer> dList = hg.defects.toList();


                    Point prevPoint = null;

                    for (int i = 0; i < dList.size(); i++) {

                        int id = i % 4;
                        Point curPoint;

                        if (id == 2) { //Defect point
                            double depth = (double)dList.get(i+1)/256.0;
                            curPoint = contourPts[dList.get(i)];

                            Point curPoint0 = contourPts[dList.get(i-2)];
                            Point curPoint1 = contourPts[dList.get(i-1)];
                            Point vec0 = new Point(curPoint0.x - curPoint.x, curPoint0.y - curPoint.y);
                            Point vec1 = new Point(curPoint1.x - curPoint.x, curPoint1.y - curPoint.y);
                            double dot = vec0.x*vec1.x + vec0.y*vec1.y;
                            double lenth0 = Math.sqrt(vec0.x*vec0.x + vec0.y*vec0.y);
                            double lenth1 = Math.sqrt(vec1.x*vec1.x + vec1.y*vec1.y);
                            double cosTheta = dot/(lenth0*lenth1);

                            if ((depth > hg.inCircleRadius*0.7)&&(cosTheta>=-0.7)
                                    && (!isClosedToBoundary(curPoint0, rgbaMat))
                                    &&(!isClosedToBoundary(curPoint1, rgbaMat))
                                    ){

                                hg.defectIdAfter.add((i));

                                Point finVec0 = new Point(curPoint0.x-hg.inCircle.x,
                                        curPoint0.y-hg.inCircle.y);
                                double finAngle0 = Math.atan2(finVec0.y, finVec0.x);
                                Point finVec1 = new Point(curPoint1.x-hg.inCircle.x,
                                        curPoint1.y - hg.inCircle.y);
                                double finAngle1 = Math.atan2(finVec1.y, finVec1.x);

                                if (hg.fingerTipsOrdered.size() == 0) {
                                    hg.fingerTipsOrdered.put(finAngle0, curPoint0);
                                    hg.fingerTipsOrdered.put(finAngle1, curPoint1);
                                } else {
                                    hg.fingerTipsOrdered.put(finAngle0, curPoint0);
                                    hg.fingerTipsOrdered.put(finAngle1, curPoint1);
                                }

                            }
                        }
                    }
                }
            }
            if (hg.detectIsHand(rgbaMat)) {
                //hg.boundingRect represents four coordinates of the bounding box.
                Core.rectangle(rgbaMat, hg.boundingRect.tl(), hg.boundingRect.br(), mColorsRGB[1], 2);
                Imgproc.drawContours(rgbaMat, hg.hullP, hg.cMaxId, mColorsRGB[2]);
            }
        } catch (Exception e) {
            Log.e(TAG ,e.toString());
        }

    }

    private boolean isClosedToBoundary(Point pt, Mat img) {
        int margin = 5;
        if ((pt.x > margin) && (pt.y > margin) &&
                (pt.x < img.cols()-margin) &&
                (pt.y < img.rows()-margin)) {
            return false;
        }
        return true;
    }

    @Nullable
    private Rect makeBoundingBox(Mat img) {
        hg.contours.clear();
        Imgproc.findContours(img, hg.contours, hg.hie, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        hg.findBiggestContour();

        if (hg.cMaxId > -1) {
            hg.boundingRect = Imgproc.boundingRect(hg.contours.get(hg.cMaxId));
        }

        if (hg.detectIsHand(rgbaMat)) {
            return hg.boundingRect;
        } else
            return null;
    }

    @Override
    public void onPause(){
        Log.i(TAG, "Paused!");
        super.onPause();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
        Log.i(TAG, "Resumed!");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroyed!");
        releaseCVMats();
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
