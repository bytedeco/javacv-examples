OpenCV2_Cookbook
================

| [Next: Chapter 1>](src/main/scala/opencv2_cookbook/chapter01) |

[OpenCV](http://opencv.org/) (Open Source Computer Vision) is a library of several hundred algorithms for computer vision and video analysis. 
It started in the late 90’s as a C library; in version 2 a C++ API was added. 

OpenCV can be used in Java Virtal Machine and Android without directly dealing with the native C++ libraries. 
Here we will conventrate on the facilities provided by Bytedeco: 
[JavaCV](https://github.com/bytedeco/javacv) and [OpenCV JavaCPP Presets](https://github.com/bytedeco/javacpp-presets).

[JavaCV](https://github.com/bytedeco/javacv) is a library based on the 
[JavaCPP Presets](https://github.com/bytedeco/javacpp-presets) that that depends commonly used 
native libraries in the field of computer vision, like OpenCV, to facilitate the development of those applications 
on the Java platform. 
It provides easy-to-use interfaces to grab frames from cameras and audio/video streams, process them, 
and record them back on disk or send them over the network.

The `OpenCV_Cookbok` example project illustrates use of OpenCV, through JavaCV, in JVM. 
It is intended as a companion to the book 
“[OpenCV 2 Computer Vision Application Programming Cookbook](http://www.laganiere.name/opencvCookbook/)” by Robert Laganière. 
The original examples in the Cookbook are written in C++. Here we show how to use JavaCV to perform the same tasks.

The code in the example module is primatily written in [Scala](http://www.scala-lang.org), one of the leading JVM languages. 
It can be easily converted to Java and other languages running on JVM, for instance, [Groovy](http://groovy.codehaus.org/).
Some of the examples are also provded in Java, for comparison.

Quick Sample
------------

Here is a quick preview that compares an original C++ example with code in Scala and Java using JavaCV wrapper.

Here is the original C++ example that opens an image (without error checking), creates a window,
displays image in the window, and waits for 5 seconds before exiting.

``` c
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>

int main() {
    // read an image
    cv::Mat image = cv::imread("boldt.jpg");

    // create image window named "My Image"
    cv::namedWindow("My Image");

    // show image on window
    cv::imshow("My Image", image);

    // wait key for 5000 ms
    cv::waitKey(5000);

    return 1;
}
```

The above C++ example translated to Scala using JavaCV wrapper, the functional difference is only that image window 
stays open till user coses it:

``` scala
import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.cpp.opencv_highgui._

object MyFirstOpenCVApp extends App {   
  // Read an image.
  val image = imread("boldt.jpg")

  // Create image window named "My Image."
  val canvas = new CanvasFrame("My Image", 1)

  // Request closing of the application when the image window is closed.
  canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)

  // Show image on window.
  canvas.showImage(image)
}
```

Now the same example expressed in a Java. Note that use of JavaCV API is exactly the same in Scala and Java code. 
The only practical difference is that in Java you have to explicily provide type for each variable, in Scala it is optional. 

``` java
import com.googlecode.javacv.CanvasFrame;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class MyFirstOpenCVApp {

    public static void main(String[] args) {
    
        // Read an image.
        final Mat image = imread("boldt.jpg");
    
        // Create image window named "My Image".
        final CanvasFrame canvas = new CanvasFrame("My Image", 1);
    
        // Request closing of the application when the image window is closed.
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            
        // Show image on window.
        canvas.showImage(image);
    }
}
```

OpenCV Documentation is Your Friend
-----------------------------------

If you are looking for a particular OpenCV operation, use the [OpenCV documentation](http://docs.opencv.org/). 
The Quick Search box is particularly helpful. 
The documentation contains descriptions of alternative ways how C/C++ OpenCV API can be used. 
The JavaCV equivalent maybe similar to the OpenCV C API.


How to use JavaCV Examples
--------------------------

The `OpenCV_Cookbok` example project is intended as a companion to the Cookbook. 
The recommended way is to read the Cookbook and refer to JavaCV examples when in doubt how to translate 
the Cookbook's C++ code to JavaCV. 
The Cookbook provides explanation how the algorithms work. 
The JavaCV examples provide only very brief comments related to specifics of JavaCV API.

Simplest way to use the JavaCV examples is to browse the code located in [src/main] online. 
You can also donload it to you computer either use Git or as a ZIP file.
 
With a minimal setup you can easily execute the examples on you own computer. 
This is one of the benefits of JavaCV - it proved all binaries needed to run OpenCV on various platfoms. 
The setup is explained in README for [Chapter 1](src/main/scala/opencv2_cookbook/chapter01).


Organization of the Example Code
--------------------------------

The code is organized into packages that correspond to chapters in the Cookbook 1st edition, 
for instance [opencv2_cookbook.chapter8](src/main/scala/opencv2_cookbook/chapter08). 
It is quite similar to the 2nd edition. 
Individual examples roughly correspond to sections within each chapter of the book.

[Chapter 1](src/main/scala/opencv2_cookbook/chapter01) describes IDE setup to run the examples, 
gives a basic example of loading and displaying an image, 
and an example of a basic GUI example that does basic image processing.
 
 
List of Examples
----------------

* [Chapter 1: Playing with Images](src/main/scala/opencv2_cookbook/chapter01)
  - *`Ex1MyFirstOpenCVApp`* - Load an image and show it in a window (CanvasFrame)
  - *`Ex2MyFirstGUIApp`* - Simple GUI application build using Scala Swing. The application has two buttons on the left "Open Image" and "Process". The opened image is displayed in the middle. When "Process" button is pressed the image is flipped upside down and its red and blue channels are swapped.
  - *`Ex3LoadAndSave`* - Reading, saving, displaying, and drawing on an image.
* [Chapter 2: Manipulating the Pixels](src/main/scala/opencv2_cookbook/chapter02)
  * *`Ex1Salt`* - Set individual, randomly selected, pixels to a fixed value. Use ImageJ's ImageProcessor to access pixels.
  * *`Ex2ColorReduce`* - Reduce colors in the image by modifying color values in all bands the same way.
  * *`Ex3Sharpen`* - Use kernel convolution to sharpen an image: `filter2D()`.
  * *`Ex4BlendImages`* - Blend two images using weighted addition: `cvAddWeighted()`.
  * *`Ex5ROILogo`* - Paste small image into a larger one using a region of interest: `IplROI` and `cvCopy()`.
* [Chapter 3: Processing Images with Classes](src/main/scala/opencv2_cookbook/chapter03)
  * *`Ex1ColorDetector`* - Compare RGB colors to some target color, colors that are similar to the target color are assigned to white in the output image, other pixels are set to black.
  * *`Ex2ColorDetectorSimpleApplication`* - Same processing is the first example, but demonstrates simple UI.
  * *`Ex3ColorDetectorMVCApplication`* - Same processing is the first example, but demonstrates a more elaborated UI.
  * *`Ex4ConvertingColorSpaces`* - Similar to the first example, but color distance is calculated in `L*a*b*` color space. Illustrates use of `cvCvtColor` function.
* [Chapter 4: Counting the Pixels with Histograms](src/main/scala/opencv2_cookbook/chapter04)
  * *`Ex1ComputeHistogram`* - Computes histogram using utility class `Histogram1D` and prints values to the screen. 
  * *`Ex2ComputeHistogramGraph`* - Displays a graph of a histogram created using utility class `Histogram1D`.
  * *`Ex3Threshold`* - Separates pixels in an image into a foreground and background using OpenCV `cvThreshold()` method.
  * *`Ex4InvertLut`* - Creates inverted image by inverting its look-up table.
  * *`Ex5EqualizeHistogram`* - Enhances image using histogram equalization.
  * *`Ex6ContentDetectionGrayscale`* - Uses histogram of a region in an grayscale image to create 'template', looks through the whole image to detect pixels that are similar to that template. Illustrates use of method `cvCalcBackProject()`.
  * *`Ex7ContentDetectionColor`* - Uses histogram of region in an color image to create 'template', looks through the whole image to detect pixels that are similar to that template. Relies on the utility classes `ColorHistogram` and `ContentFinder`.
  * *`Ex8MeanShiftDetector`* - Uses histogram of region in an color image to create 'template', uses the mean shift algorithm to find best matching location of the 'template' in another image. Illustrates use of method `cvMeanShift()`.
  * *`Ex9ImageComparator`* - Computes image similarity measure using helper class `ImageComparator`. 
  * *`Histogram1D`* - Helper class that performs histogram and look-up table operations, correspond to part of the C++ class `Histogram1D` in the OpenCV2 Cookbook sample code. Illustrates use of OpenCV methods: `cvLUT()`, `cvEqualizeHist()`, `cvCreateHist()`, `cvCalcHist()`, `cvQueryHistValue_1D()`, `cvReleaseHist()`. 
  * *`ColorHistogram`* - Helper class that simplifies usage of method `cvCalcHist()` for color images.
  * *`ContentFinder`* - helper class for template matching using method `cvCalcBackProject()`.
  * *`ImageComparator`* - helper class that computes image similarity using `cvCompareHist()`.
* [Chapter 5: Transforming Images with Morphological Operations](src/main/scala/opencv2_cookbook/chapter05)
  * *`Ex1ErodingAndDilating`* - Morphological erosion and dilation: `cvErode()` and `cvDilate()`.
  * *`Ex2OpeningAndClosing`* - Morphological opening and closing: `cvMorphologyEx()`.
  * *`Ex3EdgesAndCorners`* - Detection of edges and corners using morphological filters.
  * *`Ex4WatershedSegmentation`* - Image segmentation using the watershed algorithm.
  * *`Ex5GrabCut`* - Separates objects from background using `grabCut()`.
  * *`MorphoFeatures`* - Equivalent of C++ class of the same name, contains methods for morphological corner detection.
  * *`WatershedSegmenter`* - Helper class for section "Segmenting images using watersheds".
* [Chapter 6: Filtering the Images](src/main/scala/opencv2_cookbook/chapter06)
  * *`Ex1LowPassFilter`* - Blur with a Gaussian filter.
  * *`Ex2MedianFilter`* - Remove noise with a median filter.
  * *`Ex3DirectionalFilters`* - Use of Sobel edge detection filter.
  * *`Ex4Laplacian`* - Edge detection using Laplacian filter.
  * *`LaplacianZC`* - Computation of Laplacian and zero-crossing, used in `Ex4Laplacian`.
* [Chapter 7: Extracting Lines, Contours, and Components](src/main/scala/opencv2_cookbook/chapter07)
  * *`Ex1CannyOperator`* - Detect contours with the Canny operator.
  * *`Ex2HoughLines`* - Detect lines using standard Hough transform approach.
  * *`Ex3HoughLineSegments`* - Detect lines segments using probabilistic Hough transform approach.
  * *`Ex4HoughCircles`* - Detect circles using Hough transform approach.
  * *`Ex5ExtractContours`* - Extract contours from a binary image using connected components.
  * *`Ex6ShapeDescriptors`* - Compute various shape descriptors: bounding box, enclosing circle, approximate polygon, convex hull, center of mass.
  * *`LineFinder`* - Helper class to detect lines segments using probabilistic Hough transform approach, used in `Ex3HoughLineSegments`.
* [Chapter 8: Detecting and Matching Interest Points](src/main/scala/opencv2_cookbook/chapter08)
  * *`Ex1HarrisCornerMap`* - Computes Harris corners strength image.
  * *`Ex2HarrisCornerDetector`* - Uses Harris Corner strength image to detect well localized corners, replacing several closely located detections (blurred) by a single one. Uses `HarrisDetector` helper class.
  * *`Ex3GoodFeaturesToTrack`* - Example of using the Good Features to Track detector.
  * *`Ex4FAST`* - Example of using the FAST detector.
  * *`Ex5SURF`* - Example of using the SURF detector.
  * *`Ex6SIFT`* - Example of using the SIFT detector.
  * *`Ex7DescribingSURF`* - Computes SURF features,  extracts their descriptors, and finds best matching descriptors between two images of the same object. 
  * *`HarrisDetector`* - Helper class for Harris Corner strength image to detection and localization. Closely located detections (blurred) are replaced by a single one.
* [Chapter 9: Estimating Projective Relations in Images](src/main/scala/opencv2_cookbook/chapter09)
  * *`Ex1FindChessboardCorners`* - Illustrates one of a camera calibration steps, detection of a chessboard pattern in a calibration board.
  * *`Ex2CalibrateCamera`* - Camera calibration example, shows how to correct geometric deformation that may be introduced by the optics. Uses the `CameraCalibrator` helper class.
  * *`Ex3ComputeFundamentalMatrix`* - Using features detected and matched between two images, compute fundamental matrix that describes projective relation between those two images.
  * *`Ex4MatchingUsingSampleConsensus`* - Illustrates use of RANSAC (RANdom SAmpling Consensus) strategy. Most of the computations are done by `RobustMatcher` helper class.
  * *`Ex5Homography`* - Another way of describing relationship between points in two images, using homography. Shows an example how two images of partial views on an object can be stitched together. Most of the computations are done by `RobustMatcher` helper class.
  * *`CameraCalibrator`* - Helper class implementing camera calibration algorithm.
  * *`RobustMatcher`* - Implements RANSAC based algorithm used by examples `Ex4MatchingUsingSampleConsensus` and `Ex5Homography`.
* [Chapter 10: Processing Video Sequences](src/main/scala/opencv2_cookbook/chapter10)
  * *`Ex1ReadVideoSequenceC`* - Reads and displays a video using `C` API.
  * *`Ex1ReadVideoSequenceJavaCV`* - Reads and displays a video using JavaCV's `OpenCVFrameGrabber` class.
  * *`Ex2ProcessVideoFrames`* - Processed frames in a video file using Canny edge detector; shows the output video on the screen. Uses helper class `VideoProcessor`.
  * *`Ex3WriteVideoSequence`* - Processed frames in a video file using Canny edge detector; writes output to a video file. Uses helper class `VideoProcessor`.
  * *`Ex4TrackingFeatures`* - Tracks moving objects in a video, marks tracked points in the video shown on the screen. Most of the implementation is in `FeatureTracker` helper class.
  * *`Ex5ForegroundSegmenter`* - Detect moving object in a video through background estimation. Background is modeled using "simple" moving average approach implemented in helper class "BGFBSEgmenter."
  * *`Ex6MOGMotionDetector`* - A more sophisticated motion detector that models background using Mixture of Gaussians approach.
  * *`BGFBSEgmenter`* - Separates "static" background from "moving" foreground by modeling background using a moving average. Used by example 'Ex5ForegroundSegmenter'.
  * *`FeatureTracker`* - Track moving features using an optical flow algorithm, used by example `Ex4TrackingFeatures`.
  * *`VideoProcessor`* - Helper class for dealing with video files, loading and applying processing to individual frames, used by examples: `Ex2ProcessVideoFrames`, `Ex3WriteVideoSequence`, `Ex4TrackingFeatures`, and `Ex5ForegroundSegmenter`.
* [Utilities](src/main/scala/opencv2_cookbook)
  * *`OpenCVUtils`* - reading and writing of image files, display of images, drawing of features on images, conversions between OpenCV image and data representations.
  * *`OpenCVImageJUtils`* - conversion between OpenCV and ImageJ image representations.

Why Scala?
----------

[Scala](http://www.scala-lang.org) was chosen since it is more expressive than Java. 
You can achieve the same result with smaller amount of code. Smaller boilerplate code makes examples easier to read and understand. 
Compiled Scala code is fast, similar to Java and C++. 
Scala supports writing of scripts, code that can be executed without explicit compiling. 
Scala also has a console, called REPL, where single lines of code can be typed in and executed on a spot. 
Both of those features make prototyping of OpenCV-based programs easier in Scala than in Java.
Last but not least, IDE support for Scala reached level of maturity allowing easy creation, modification, 
and execution of Scala code. 
In particular, the [Scala plugin](http://blog.jetbrains.com/scala/)  for [JetBrains IDEA](http://www.jetbrains.com/idea/) 
works very well. 
There is also Scala support for [Eclipse](http://scala-ide.org/index.html) and [NetBeans](https://github.com/dcaoyuan/nbscala).

| [Next: Chapter 1>](src/main/scala/opencv2_cookbook/chapter01) |