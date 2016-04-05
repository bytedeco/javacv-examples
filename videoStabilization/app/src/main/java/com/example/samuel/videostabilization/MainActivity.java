/**
 * Created by samuel on 2/4/16.
 * <p/>
 * Author's e-mail: gadesoye at ucsc.edu
 * web: supergee.me
 */

package com.example.samuel.videostabilization;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.bytedeco.javacpp.opencv_calib3d.findHomography;
import static org.bytedeco.javacpp.opencv_core.CV_64F;
import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_core.gemm;
import static org.bytedeco.javacpp.opencv_core.invert;
import static org.bytedeco.javacpp.opencv_core.transpose;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.filter2D;
import static org.bytedeco.javacpp.opencv_imgproc.getGaussianKernel;
import static org.bytedeco.javacpp.opencv_imgproc.goodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.warpPerspective;
import static org.bytedeco.javacpp.opencv_video.calcOpticalFlowPyrLK;


public class MainActivity extends AppCompatActivity {

    public static final String debugTag = "gDebug";
    private final String inputVideo = "/storage/emulated/0/video/testVid_2.avi";
    private final String outputVideo = "/storage/emulated/0/video/stabVideo.avi";

    OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //grab video frame
        FFmpegFrameGrabber grabFrame = new FFmpegFrameGrabber(inputVideo); //change this to your own video location

        try {
            grabFrame.start();
            grabFrame.setFrameNumber(1);
            Frame prevFrame = grabFrame.grabImage();
            int noFrames = grabFrame.getLengthInFrames();
            int filterWindow = 30;
            double stD = 30;

            //initialize the first frame
            Mat outImagePrev = matConverter.convert(prevFrame).clone();
            Mat outImageNext = null;

            Mat blackOutImagePrev = new Mat(outImagePrev.size(), CV_8UC1);
            Mat blackOutImageNext = new Mat(outImagePrev.size(), CV_8UC1);

            Mat prevCorners = new Mat();
            Mat nextCorners = new Mat();
            Mat status = new Mat();
            Mat err = new Mat();
            Mat outPutHomography = null;

            Frame nextFrame = null;

            //initialize matrix to storee transform values
            Mat trajectoryC = new Mat(9, noFrames - 1, CV_64F);
            Mat trajectorySmooth = new Mat(trajectoryC.size(), trajectoryC.type());

            //we use the indexers to access the elements in matrices
            DoubleIndexer trajectoryCIndexer = trajectoryC.createIndexer(true);
            ;
            DoubleIndexer trajectorySmoothCIndexer;

            //initialize the array used for cumulating transfoms. e.g Frame4 = H3*H2*H1* Frame1
            Mat hMultiplier = Mat.eye(3, 3, CV_64F).asMat();
            Mat hSmoothed = Mat.eye(3, 3, CV_64F).asMat();

            //create indexer for the homography matrices
            DoubleIndexer hMultiplierIndexer;
            DoubleIndexer hSmoothedIndexer;

            //create indexer for the detected and tracked points
            FloatIndexer nextPointIndex;
            FloatIndexer prevPointIndex;

            FloatIndexer nextCleanPointIndex;
            FloatIndexer prevCleanPointIndex;


            //indexer for status returned by Lukas-Kanade.. status=0, implies tracking was not successfull.. status=1 implies otherwise
            UByteIndexer statusIndex;

            //obtain per frame homography asper matlab
            for (int i = 1; i < noFrames; i++) {

                grabFrame.setFrameNumber(i + 1);
                nextFrame = grabFrame.grabImage();

                //we are working with pairs of frames, move to next if we don't have a next frame
                if (matConverter.convert(nextFrame) == null) continue;

                outImageNext = matConverter.convert(nextFrame).clone();


                //convert images.. Feature location and tracking is done on grayscale
                cvtColor(outImagePrev, blackOutImagePrev, CV_BGR2GRAY);
                cvtColor(outImageNext, blackOutImageNext, CV_BGR2GRAY);

                // LK Feature Tracking
                //detect features in frame
                goodFeaturesToTrack(blackOutImagePrev, prevCorners, 400, 0.1, 30);

                //Track in next Frame
                calcOpticalFlowPyrLK(blackOutImagePrev, blackOutImageNext, prevCorners, nextCorners, status, err);


                statusIndex = status.createIndexer(true);
                nextPointIndex = nextCorners.createIndexer(true);
                prevPointIndex = prevCorners.createIndexer(true);

                //delete bad points based on the returned status
                Mat prevCornersClean = new Mat(prevCorners.size(), prevCorners.type());
                Mat nextCornersClean = new Mat(nextCorners.size(), nextCorners.type());

                nextCleanPointIndex = nextCornersClean.createIndexer(true);
                prevCleanPointIndex = prevCornersClean.createIndexer(true);

                int k = 0;
                int j;

                for (j = 0; j < status.rows(); j++) {

                    if (statusIndex.get(j) != 0) {

                        nextCleanPointIndex.put(k, 0, nextPointIndex.get(j, 0));
                        nextCleanPointIndex.put(k, 1, nextPointIndex.get(j, 1));
                        prevCleanPointIndex.put(k, 0, prevPointIndex.get(j, 0));
                        prevCleanPointIndex.put(k, 1, prevPointIndex.get(j, 1));

                        k++;
                    }

                }

                //delete unused space in the corner matrix
                nextCornersClean.pop_back(j - k + 1);
                prevCornersClean.pop_back(j - k + 1);


                //find homography
                outPutHomography = findHomography(prevCornersClean, nextCornersClean);

                //cumulate Homography H_n, H_n-1, ... , H_2, H_1
                gemm(outPutHomography, hMultiplier, 1, hMultiplier, 0, hMultiplier, 0);

                hMultiplierIndexer = hMultiplier.createIndexer(true);

                hMultiplierIndexer.put(0, 0, hMultiplierIndexer.get(0, 0) / hMultiplierIndexer.get(2, 2)); //0
                hMultiplierIndexer.put(0, 1, hMultiplierIndexer.get(0, 1) / hMultiplierIndexer.get(2, 2)); //1
                hMultiplierIndexer.put(0, 2, hMultiplierIndexer.get(0, 2) / hMultiplierIndexer.get(2, 2)); //2
                hMultiplierIndexer.put(1, 0, hMultiplierIndexer.get(1, 0) / hMultiplierIndexer.get(2, 2)); //3
                hMultiplierIndexer.put(1, 1, hMultiplierIndexer.get(1, 1) / hMultiplierIndexer.get(2, 2)); //4
                hMultiplierIndexer.put(1, 2, hMultiplierIndexer.get(1, 2) / hMultiplierIndexer.get(2, 2)); //5
                hMultiplierIndexer.put(2, 0, hMultiplierIndexer.get(2, 0) / hMultiplierIndexer.get(2, 2)); //6
                hMultiplierIndexer.put(2, 1, hMultiplierIndexer.get(2, 1) / hMultiplierIndexer.get(2, 2)); //7
                hMultiplierIndexer.put(2, 2, hMultiplierIndexer.get(2, 2) / hMultiplierIndexer.get(2, 2)); //8


                trajectoryCIndexer.put(0, i - 1, hMultiplierIndexer.get(0, 0)); //0
                trajectoryCIndexer.put(1, i - 1, hMultiplierIndexer.get(0, 1)); //1
                trajectoryCIndexer.put(2, i - 1, hMultiplierIndexer.get(0, 2)); //2
                trajectoryCIndexer.put(3, i - 1, hMultiplierIndexer.get(1, 0)); //3
                trajectoryCIndexer.put(4, i - 1, hMultiplierIndexer.get(1, 1)); //4
                trajectoryCIndexer.put(5, i - 1, hMultiplierIndexer.get(1, 2)); //5
                trajectoryCIndexer.put(6, i - 1, hMultiplierIndexer.get(2, 0)); //6
                trajectoryCIndexer.put(7, i - 1, hMultiplierIndexer.get(2, 1)); //7
                trajectoryCIndexer.put(8, i - 1, hMultiplierIndexer.get(2, 2)); //8


                outImagePrev.release();

                outImagePrev = outImageNext.clone();

            }



            Mat gaussianKenel = getGaussianKernel(filterWindow, -1);
            transpose(gaussianKenel, gaussianKenel);// need vertical


            //Gaussian Smoothening
            filter2D(trajectoryC, trajectorySmooth, -1, gaussianKenel);
            //Log.d(debugTag, "cols " + gaussianKenel.cols() + "rows: " + gaussianKenel.rows() + "cha: " + gaussianKenel.channels());


            //extract individual homographies for warping...

            FFmpegFrameRecorder stableVideoRecorder = FFmpegFrameRecorder.createDefault(outputVideo, outImagePrev.cols(), outImagePrev.rows());//again, use your video

            //start recording frames into the video
            stableVideoRecorder.start();

            trajectorySmoothCIndexer = trajectorySmooth.createIndexer(true);
            hMultiplierIndexer = hMultiplier.createIndexer(true);
            hSmoothedIndexer = hSmoothed.createIndexer(true);

            for (int p = 1; p < noFrames; p++) {

                //obtain the smothed homography
                hSmoothedIndexer.put(0, 0, trajectorySmoothCIndexer.get(0, p - 1)); //0
                hSmoothedIndexer.put(0, 1, trajectorySmoothCIndexer.get(1, p - 1)); //1
                hSmoothedIndexer.put(0, 2, trajectorySmoothCIndexer.get(2, p - 1)); //2
                hSmoothedIndexer.put(1, 0, trajectorySmoothCIndexer.get(3, p - 1)); //3
                hSmoothedIndexer.put(1, 1, trajectorySmoothCIndexer.get(4, p - 1)); //4
                hSmoothedIndexer.put(1, 2, trajectorySmoothCIndexer.get(5, p - 1)); //5
                hSmoothedIndexer.put(2, 0, trajectorySmoothCIndexer.get(6, p - 1)); //6
                hSmoothedIndexer.put(2, 1, trajectorySmoothCIndexer.get(7, p - 1)); //7
                hSmoothedIndexer.put(2, 2, trajectorySmoothCIndexer.get(8, p - 1)); //8


                //obtain previous homography
                hMultiplierIndexer.put(0, 0, trajectoryCIndexer.get(0, p - 1)); //0
                hMultiplierIndexer.put(0, 1, trajectoryCIndexer.get(1, p - 1)); //1
                hMultiplierIndexer.put(0, 2, trajectoryCIndexer.get(2, p - 1)); //2
                hMultiplierIndexer.put(1, 0, trajectoryCIndexer.get(3, p - 1)); //3
                hMultiplierIndexer.put(1, 1, trajectoryCIndexer.get(4, p - 1)); //4
                hMultiplierIndexer.put(1, 2, trajectoryCIndexer.get(5, p - 1)); //5
                hMultiplierIndexer.put(2, 0, trajectoryCIndexer.get(6, p - 1)); //6
                hMultiplierIndexer.put(2, 1, trajectoryCIndexer.get(7, p - 1)); //7
                hMultiplierIndexer.put(2, 2, trajectoryCIndexer.get(8, p - 1)); //8


                //invert the previous
                invert(hMultiplier, hMultiplier);
                hMultiplierIndexer = hMultiplier.createIndexer(true);

                //left multiply smoothed with inverse of previous
                gemm(hSmoothed, hMultiplier, 1, hMultiplier, 0, hMultiplier, 0);

                //warp frames and store into video file
                grabFrame.setFrameNumber(p + 1);
                nextFrame = grabFrame.grabImage();

                if (matConverter.convert(nextFrame) == null) continue;
                outImageNext = matConverter.convert(nextFrame).clone();

                warpPerspective(outImageNext, outImagePrev, hMultiplier, outImagePrev.size()); //out Image previous now contains our warped image

                //finally write image into Frame

                stableVideoRecorder.record(matConverter.convert(outImagePrev));
            }
            grabFrame.stop();
            stableVideoRecorder.stop();



        } catch (Exception e) {
            Log.d(debugTag, e.getMessage());
            e.printStackTrace();
        }

        class serverCommTask extends AsyncTask<String, Void, Void> {
            protected Void doInBackground(String... urlString) {

                try {


                    URL url = new URL(urlString[0]);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setDoOutput(true); // we are going to upload data
                    urlConnection.setChunkedStreamingMode(0); // use default chunk length

                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                    //write to output stream
                    OutputStreamWriter outWriter = new OutputStreamWriter(out);



                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    //read from input stream
                    InputStreamReader inReader = new InputStreamReader(in);

                    char[] buffer = new char[100];

                    inReader.read(buffer);

                    urlConnection.disconnect();


                } catch (Exception e) {
                    Log.d(debugTag, " " + e.getMessage());
                    e.printStackTrace();
                }
                return null; // for returning nothing
            }

        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
