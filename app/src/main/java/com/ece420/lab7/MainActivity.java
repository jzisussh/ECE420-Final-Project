package com.ece420.lab7;

import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.findContours;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.Manifest;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.GridLayout;
import android.view.Gravity;
import android.util.TypedValue;
import android.widget.ImageView;
import android.graphics.Bitmap;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.text.Text;
import org.opencv.tracking.TrackerKCF;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint2f;
import org.opencv.android.Utils;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    // UI Variables
    private Button startButton;
    private Button captureButton;

    // 2D array to store references to all TextViews
    private TextView[][] textViewArray = new TextView[4][4];

    // Declare OpenCV based camera view base
    private CameraBridgeViewBase mOpenCvCameraView;
    // Camera size
    private int myWidth;
    private int myHeight;

    // Mat to store RGBA and Grayscale camera preview frame
    private Mat mRgba;
    private Mat mGray;
    private Mat transformRgba;
    private Mat transformGray;

    private int opencv_loaded_flag = -1;
    private int start_flag = -1;
    private int transWidth = 600;
    private int transHeight = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Request User Permission on Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        // OpenCV Loader and Avoid using OpenCV Manager
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        // Setup start button
        startButton = (Button) findViewById((R.id.startButton));

        // Setup the output matrix table
        // Retrieve references to all TextViews
        textViewArray[0][0] = (TextView) findViewById(R.id.A00);
        textViewArray[0][1] = (TextView) findViewById(R.id.A01);
        textViewArray[0][2] = (TextView) findViewById(R.id.A02);
        textViewArray[0][3] = (TextView) findViewById(R.id.A03);
        textViewArray[1][0] = (TextView) findViewById(R.id.A10);
        textViewArray[1][1] = (TextView) findViewById(R.id.A11);
        textViewArray[1][2] = (TextView) findViewById(R.id.A12);
        textViewArray[1][3] = (TextView) findViewById(R.id.A13);
        textViewArray[2][0] = (TextView) findViewById(R.id.A20);
        textViewArray[2][1] = (TextView) findViewById(R.id.A21);
        textViewArray[2][2] = (TextView) findViewById(R.id.A22);
        textViewArray[2][3] = (TextView) findViewById(R.id.A23);
        textViewArray[3][0] = (TextView) findViewById(R.id.A30);
        textViewArray[3][1] = (TextView) findViewById(R.id.A31);
        textViewArray[3][2] = (TextView) findViewById(R.id.A32);
        textViewArray[3][3] = (TextView) findViewById(R.id.A33);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (start_flag == -1 && opencv_loaded_flag == 1) {
                    start_flag = 1;
                    startButton.setText("STOP"); // Modify text
                    mOpenCvCameraView.enableView();
                } else if (start_flag == 1) {
                    start_flag = -1;
                    startButton.setText("START"); // Modify text
                    mOpenCvCameraView.disableView();
                }
            }
        });

        captureButton = (Button) findViewById((R.id.captureButton));
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (start_flag == -1) return;
        ///////////////////////////////// Start of functions ///////////////////////////////////////////////////////
                // Canny Edge Detection
                Mat edges = new Mat();
                Imgproc.Canny(mGray, edges, 50, 150, 5, false);

                // Contour detection
                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                Log.d("Debug", "reach line 129"+edges);

                // Fetch the Outmost Contour
                MatOfPoint outerContour = null;
                double maxArea = -1;
                for (MatOfPoint contour : contours) {
                    double area = Imgproc.contourArea(contour);
                    if (area > maxArea) {
                        maxArea = area;
                        outerContour = contour;
                    }
                }

                Log.d("Debug", "reach line 132"+outerContour);

                // Calculate the convex hull of the outer contour
                MatOfInt hull = new MatOfInt();
                Imgproc.convexHull(outerContour, hull);

                // Extract the corner points from the convex hull
                Point[] hullPoints = new Point[hull.rows()];
                for (int i = 0; i < hull.rows(); i++) {
                    int index = (int) hull.get(i, 0)[0];
                    hullPoints[i] = outerContour.toArray()[index];
                }

                Log.d("Debug", "reach line 142");

                // Define a custom comparator to sort based on x-coordinate
                Comparator<Point> sortByX = Comparator.comparingDouble(p -> p.x);
                // Define a custom comparator to sort based on y-coordinate
                Comparator<Point> sortByY = Comparator.comparingDouble(p -> p.y);

                // Sort the corner points based on x coordinates using the custom comparator
                List<Point> cornerPointsList = Arrays.asList(hullPoints);
                cornerPointsList.sort(sortByX);

                Log.d("Debug", "reach line 151"+cornerPointsList);

                // Sort the corner points based on y coordinates
                List<Point> cornerPointsSortedY = new ArrayList<>(cornerPointsList);
                cornerPointsSortedY.sort(sortByY);

                Log.d("Debug", "reach line 160"+cornerPointsSortedY);

                // Find the extreme points
                Point xMax = cornerPointsList.get(cornerPointsList.size() - 1);
                Point xMin = cornerPointsList.get(0);
                Point yMax = cornerPointsSortedY.get(cornerPointsSortedY.size() - 1);
                Point yMin = cornerPointsSortedY.get(0);

                // Find the points
                List<Point> smallYPoints = cornerPointsList.stream().filter(point -> point.y < (yMax.y - yMax.y / 10 + yMin.y)).collect(Collectors.toList());
                List<Point> sortedX = new ArrayList<>(smallYPoints);
                sortedX.sort(sortByX);
                Point yMinXMax = sortedX.get(sortedX.size() - 1);

                double minDis = cornerPointsList.get(0).dot(cornerPointsList.get(0));
                Point xMinYMin = cornerPointsList.get(0);
                for (Point point : cornerPointsList) {
                    double dis = point.dot(point);
                    if (dis < minDis) {
                        minDis = dis;
                        xMinYMin = point;
                    }
                }

                List<Point> largeYPoints = cornerPointsList.stream().filter(point -> point.y > (yMax.y - yMax.y / 10)).collect(Collectors.toList());
                List<Point> sortedXLargeY = new ArrayList<>(largeYPoints);
                sortedXLargeY.sort(sortByX.reversed());
                Point yMaxXMax = sortedXLargeY.get(0);

                // Sort largeYPoints based on x-coordinate
                List<Point> sortedXMinYMax = largeYPoints.stream()
                        .sorted(Comparator.comparingDouble(point -> point.x)) // Sorting by x-coordinate
                        .collect(Collectors.toList());
                Point xMinYMax = sortedXMinYMax.get(0);

                // Display the four points using log
                Log.d("CornerPoints", "Point with minimum x and minimum y coordinates: " + xMinYMin);
                Log.d("CornerPoints", "Point with minimum x and maximum y coordinates: " + xMinYMax);
                Log.d("CornerPoints", "Point with maximum x and maximum y coordinates: " + yMaxXMax);
                Log.d("CornerPoints", "Point with maximum x and minimum y coordinates: " + yMinXMax);

                // Create point source and point destination
                Point[] pts_dst = new Point[]{
                        new Point(0, 0),
                        new Point(transWidth - 1, 0),
                        new Point(transWidth - 1, transHeight - 1),
                        new Point(0, transHeight - 1)
                };
                MatOfPoint2f pts_src = new MatOfPoint2f();
                Point[] corner_points_array = new Point[]{
                        xMinYMin,
                        yMinXMax,
                        yMaxXMax,
                        xMinYMax
                };
                pts_src.fromArray(corner_points_array);

                // Perspective transform matrix
                Mat perspective_matrix = Imgproc.getPerspectiveTransform(pts_src, new MatOfPoint2f(pts_dst));

                // Perspective transform to grayscale image for the next step processing
                Imgproc.warpPerspective(mGray, transformGray, perspective_matrix, new Size(transWidth, transHeight));

                // Perspective transform to RGB image for display
                Imgproc.warpPerspective(mRgba, transformRgba, perspective_matrix, new Size(transWidth, transHeight));

                // Image View on app
                ImageView imageView = (ImageView) findViewById(R.id.transformView);
                Bitmap bitmap = Bitmap.createBitmap(transformRgba.cols(), transformRgba.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(transformRgba, bitmap);
                imageView.setImageBitmap(bitmap);


                // Obtain the Context instance associated with the clicked view
                Context context = v.getContext();
                // Load the screenList
                List<Pair<Integer, Mat>> screenList = TemplateLoader.loadTemplates(context);
                Log.d("Templates", "template images" + screenList);

                ///////////////////// Image cropping and Digit recognition /////////////
                // Crop the transformed gray image into 16 small images and recognize digits in each small image
                int rows = 4;
                int cols = 4;
                int HEIGHT = transHeight / rows;
                int WIDTH =  transWidth / cols;

                int [][] matrix = new int[rows][cols];

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        // Calculate ROI coordinates
                        int x = col * WIDTH;
                        int y = row * HEIGHT;
                        int roiWidth = (col == cols - 1) ? transWidth - x : WIDTH;
                        int roiHeight = (row == rows - 1) ? transHeight - y : HEIGHT;

                        Rect roi = new Rect(x, y, roiWidth, roiHeight);
                        Mat croppedImage = new Mat(transformGray, roi.clone()); // Clone the ROI to ensure independence
                        Log.d("debug", "reach line 279" );
                        Log.d("CroppedImage", "croppedImage" + croppedImage);

                        //############# Digit Recogntition ##########//
//                        List<Pair<Integer, Mat>> results = new ArrayList<>();

                        //create a list containing all the matched digit images and corresponding value
                        List<Pair<Integer, Double>> Matches = new ArrayList<>();

                        // Now you can use the loaded screenList as needed
                        // For example, you can iterate over the list and print the values
                        for (Pair<Integer, Mat> pair : screenList) {
                            int num = pair.first;
                            Mat template = pair.second;
                            Mat result = new Mat();
                            Imgproc.matchTemplate(croppedImage, template, result, Imgproc.TM_CCOEFF_NORMED);
                            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
                            double maxVal = mmr.maxVal;
                            Log.d("Max Match", "Match Pairs"+ maxVal );

                            // Store the result along with the index of the template
                            Matches.add(new Pair<>(num, maxVal));
                        }

                        // Find the index of the template with the highest maximum value
                        int bestIndex = -1;
                        double maxVal = 0.0;

                        Log.d("Matches", "Match Pairs"+Matches );

                        for (Pair<Integer, Double> Match : Matches) {
                            if (Match.second > maxVal) {
                                maxVal = Match.second;
                                bestIndex = Match.first;
                            }
                        }

                        // Return the recognized value (template index + 1), or 0 if recognition fails
                        int digit = (maxVal >= 0.5) ? (bestIndex ) : 0;
                        // Perform recognition on the cropped image
//                        int result = recognize(croppedImage, ScreenList);

                        // add to the matrix
                        matrix[row][col] = digit;

                    }
                }
                Log.d("matrix", "matrix"+matrix );

                // Display the output matrix in the table UI
                // Iterate through the matrix and set the corresponding digit to each TextView
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        textViewArray[i][j].setText(String.valueOf(matrix[i][j]));
                    }
                }


                ///////////////////// End of Image cropping and Digit recognition /////////////

                // Release memory
                edges.release();
                hierarchy.release();
                hull.release();

            }

        });
        //////////////////////  End of Functions ////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Setup OpenCV Camera View
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_camera_preview);
        // Use main camera with 0 or front camera with 1
        mOpenCvCameraView.setCameraIndex(0);
        // Force camera resolution
        // mOpenCvCameraView.setMaxFrameSize(1280, 720);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    opencv_loaded_flag = 1;
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // OpenCV Camera Functionality Code
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        transformGray = new Mat(transWidth, transHeight, CvType.CV_8UC1);
        transformRgba = new Mat(transWidth, transHeight, CvType.CV_8UC4);
        myWidth = width;
        myHeight = height;
        Log.d("check frame size", "The value of frame height is: " + height);
        Log.d("check frame size", "The value of frame width is: " + width);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        transformGray.release();
        transformRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // Grab camera frame in rgba and grayscale format
        mRgba = inputFrame.rgba();
        // Grab camera frame in gray format
        mGray = inputFrame.gray();
        // Returned frame will be displayed on the screen
        return mRgba;
    }
}