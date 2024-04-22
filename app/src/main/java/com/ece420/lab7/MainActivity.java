package com.ece420.lab7;

import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.findContours;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.text.Text;
import org.opencv.tracking.TrackerKCF;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint2f;
import org.opencv.android.Utils;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    // UI Variables
    private Button startButton;
    private Button captureButton;

    // Declare OpenCV based camera view base
    private CameraBridgeViewBase mOpenCvCameraView;
    // Camera size
    private int myWidth;
    private int myHeight;

    // Mat to store RGBA and Grayscale camera preview frame
    private Mat mRgba;
    private Mat mGray;
    private Mat transformRgba;

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
                Mat edges = new Mat();
                Imgproc.Canny(mGray, edges,5, 15, 7, false);

                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                MatOfPoint outerContour = null;
                double maxArea = -1;
                for (MatOfPoint contour : contours) {
                    double area = Imgproc.contourArea(contour);
                    if (area > maxArea) {
                        maxArea = area;
                        outerContour = contour;
                    }
                }

                MatOfInt hull = new MatOfInt();
                Imgproc.convexHull(outerContour, hull);

                Point[] hullPoints = new Point[hull.rows()];
                for (int i = 0; i < hull.rows(); i++) {
                    int index = (int) hull.get(i, 0)[0];
                    hullPoints[i] = outerContour.toArray()[index];
                }

                Point topLeft = hullPoints[0];
                Point topRight = hullPoints[0];
                Point bottomLeft = hullPoints[0];
                Point bottomRight = hullPoints[0];
                for (Point point : hullPoints) {
                    if (point.x + point.y < topLeft.x + topLeft.y) topLeft = point;
                    if (point.x - point.y > topRight.x - topRight.y) topRight = point;

                    if (point.x - point.y < bottomLeft.x - bottomLeft.y) bottomLeft = point;
                    if (point.x + point.y > bottomRight.x + bottomRight.y) bottomRight = point;
                }

                // Create point source and point destination
                Point[] pts_dst = new Point[]{
                        new Point(0, 0),
                        new Point(transWidth - 1, 0),
                        new Point(transWidth - 1, transHeight - 1),
                        new Point(0, transHeight - 1)
                };
                MatOfPoint2f pts_src = new MatOfPoint2f();
                Point[] corner_points_array = new Point[]{
                        topLeft,
                        topRight,
                        bottomRight,
                        bottomLeft
                };
                pts_src.fromArray(corner_points_array);

                Log.d("coord topleft", "(" + topLeft.x + "," + topLeft.y + ")");
                Log.d("coord topright", "(" + topRight.x + "," + topRight.y + ")");
                Log.d("coord botleft", "(" + bottomLeft.x + "," + bottomLeft.y + ")");
                Log.d("coord botright", "(" + bottomRight.x + "," + bottomRight.y + ")");

                // Perspective transform
                Mat perspective_matrix = Imgproc.getPerspectiveTransform(pts_src, new MatOfPoint2f(pts_dst));
                Imgproc.warpPerspective(mRgba, transformRgba, perspective_matrix, new Size(transWidth, transHeight));

                // Image View on app
                ImageView imageView = (ImageView) findViewById(R.id.transformView);
                Bitmap bitmap = Bitmap.createBitmap(transformRgba.cols(), transformRgba.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(transformRgba, bitmap);
                imageView.setImageBitmap(bitmap);

                // Release Memory
                edges.release();
                hierarchy.release();
                hull.release();
            }
        });

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
        transformRgba = new Mat(transWidth, transHeight, CvType.CV_8UC4);
        myWidth = width;
        myHeight = height;
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
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