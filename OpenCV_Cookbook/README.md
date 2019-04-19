OpenCV Cookbook Examples
=========================

| [Next: Chapter 1>](src/main/scala/opencv_cookbook/chapter01) |

Overview
--------

**OpenCV Cookbook Examples** illustrate use of OpenCV with [JavaCV][javacv]. The Examples started as a port of C++ code from Robert Laganière's book "[OpenCV 2 Computer Vision Application Programming Cookbook](https://www.packtpub.com/application-development/opencv-2-computer-vision-application-programming-cookbook)". Later updated for the ssecond edition of the book "[OpenCV Computer Vision Application Programming Cookbook Second Edition](https://www.packtpub.com/application-development/opencv-3-computer-vision-application-programming-cookbook)". The examples in the book use [OpenCV](http://opencv.org/) C++ API. Here they are translated to use [JavaCV][javacv](https://github.com/bytedeco/javacv) and [JavaCPP-Presets](https://github.com/bytedeco/javacpp-presets) APIs.

[OpenCV](http://opencv.org/) (Open Source Computer Vision) is a library of several hundred algorithms for computer vision and video analysis. OpenCV can be us on JVM using two approaches. First are Java [wrappers provided by OpenCV](http://docs.opencv.org/doc/tutorials/introduction/desktop_java/java_dev_intro.html). Second are are wrappers based on [JavaCPP](https://github.com/bytedeco/javacpp) (C++ wrapper engine for JVM) called [OpenCV JavaCPP Presets](https://github.com/bytedeco/javacpp-presets). There are also JavaCPP presets for other computer vision related libraries like: [FFmpeg](http://ffmpeg.org/), [libdc1394](http://damien.douxchamps.net/ieee1394/libdc1394/), [PGR FlyCapture](http://www.ptgrey.com/products/pgrflycapture/), [OpenKinect](http://openkinect.org/), [videoInput](http://muonics.net/school/spring05/videoInput/), [ARToolKitPlus](http://studierstube.icg.tugraz.at/handheld_ar/artoolkitplus.php), [flandmark](http://cmp.felk.cvut.cz/~uricamic/flandmark/), and [others](https://github.com/bytedeco/javacpp-presets). JavaCV combines libraries in JavaCPP Presets and add some additional functionality that makes them easier use on JVM.

The *OpenCV Cookbook Examples* project illustrates use of OpenCV through JavaCV and OpenCV JavaCPP Presets. Current version is updated to match the second edition of the Robert Laganière's book "[OpenCV Computer Vision Application Programming Cookbook Second Edition](https://www.packtpub.com/application-development/opencv-3-computer-vision-application-programming-cookbook)". It is intended for use with OpenCV v.4 (JavaCV v.1). 

While code in the examples is primarily written in [Scala](http://www.scala-lang.org), one of the leading JVM languages. It can be easily converted to Java and other languages running on JVM, for instance, [Groovy](http://groovy.codehaus.org/). The use of the JavaCV API is very similar in most JVM languages. Some examples are provided in Java version.


Quick Sample
------------

Here is a quick preview that compares an original C++ example with code in Scala and Java using JavaCV wrapper.

Here is the original C++ example that opens an image (without error checking), creates a window,
displays image in the window, and waits for 5 seconds before exiting.


```cpp
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>

int main() {
    // Read an image
    cv::Mat src = cv::imread("data/boldt.jpg");
    display(src, "Input")

	// Apply Laplacian filter
    cv::Mat dest;
    cv::Laplacian(src, dest, src.depth(), 1, 3, 0, BORDER_DEFAULT);
    display(dest, "Laplacian");

    // wait key for 5000 ms
    cv::waitKey(5000);

    return 1;
}

//---------------------------------------------------------------------------

void display(Mat image, char* caption) {
    // Create image window named "My Image"
    cv::namedWindow(caption);

    // Show image on window
    cv::imshow(caption, image);
}
```

The above C++ example translated to Scala using JavaCV wrapper:

```scala
import javax.swing._
import org.bytedeco.javacv._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

object MyFirstOpenCVApp extends App {

  // Read an image.
  val src = imread("data/boldt.jpg")
  display(src, "Input")

  // Apply Laplacian filter
  val dest = new Mat()
  Laplacian(src, dest, src.depth(), 1, 3, 0, BORDER_DEFAULT)
  display(dest, "Laplacian")

  //---------------------------------------------------------------------------

  /** Display `image` with given `caption`. */
  def display(image: Mat, caption: String): Unit = {
    // Create image window named "My Image."
    val canvas = new CanvasFrame(caption, 1)

    // Request closing of the application when the image window is closed.
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    // Convert from OpenCV Mat to Java Buffered image for display
    val converter = new OpenCVFrameConverter.ToMat()
    // Show image on window
    canvas.showImage(converter.convert(image))
  }
}
```

Now the same example expressed in a Java. Note that use of JavaCV API is exactly the same in Scala and Java code. The only practical difference is that in Java code is more verbose, you have to explicitly provide type for each variable, in Scala it is optional. 

```java
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.swing.*;

import static org.bytedeco.opencv.global.opencv_core.BORDER_DEFAULT;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.Laplacian;

public class MyFirstOpenCVAppInJava {

    public static void main(String[] args) {

        // Read an image.
        final Mat src = imread("data/boldt.jpg");
        display(src, "Input");

        // Apply Laplacian filter
        final Mat dest = new Mat();
        Laplacian(src, dest, src.depth(), 1, 3, 0, BORDER_DEFAULT);
        display(dest, "Laplacian");
    }

    //---------------------------------------------------------------------------

    static void display(Mat image, String caption) {
        // Create image window named "My Image".
        final CanvasFrame canvas = new CanvasFrame(caption, 1.0);

        // Request closing of the application when the image window is closed.
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Convert from OpenCV Mat to Java Buffered image for display
        final OpenCVFrameConverter converter = new OpenCVFrameConverter.ToMat();
        // Show image on window.
        canvas.showImage(converter.convert(image));
    }
}
```

OpenCV Documentation is Your Friend
-----------------------------------

If you are looking for a particular OpenCV operation, use the [OpenCV documentation](http://docs.opencv.org/). The Quick Search box is particularly helpful. The documentation contains descriptions of alternative ways how C/C++ OpenCV API can be used. 

How to use JavaCV Examples
--------------------------

The *OpenCV Cookbook Examples* project is intended as a companion to the Robert Laganière's book "[OpenCV Computer Vision Application Programming Cookbook Second Edition](https://www.packtpub.com/application-development/opencv-3-computer-vision-application-programming-cookbook)". The recommended way is to read the Cookbook and refer to JavaCV examples when in doubt how to translate the Cookbook's C++ code to JavaCV. The Cookbook provides explanation how the algorithms work. The JavaCV examples provide only very brief comments related to specifics of JavaCV API.

Simplest way to use the JavaCV examples is to browse the code located in [src/main] online. 
You can also download it to you computer either use Git or as a ZIP file.
 
With a minimal setup you can easily execute the examples on you own computer. This is one of the benefits of JavaCV - it provides all binaries needed to run OpenCV on various platforms. The setup is explained in README for [Chapter 1](src/main/scala/opencv_cookbook/chapter01).


Organization of the Example Code
--------------------------------

The code is organized into packages that correspond to chapters in the Cookbook 1st edition, 
for instance [opencv_cookbook.chapter8](src/main/scala/opencv_cookbook/chapter08). 
It is quite similar to the 2nd edition. 
Individual examples roughly correspond to sections within each chapter of the book.

[Chapter 1](src/main/scala/opencv_cookbook/chapter01) describes IDE setup to run the examples, 
gives a basic example of loading and displaying an image, 
and an example of a basic GUI example that does basic image processing.
 
 
List of Examples
----------------

* [Chapter 1: Playing with Images](src/main/scala/opencv_cookbook/chapter01)
  - *`Ex1MyFirstOpenCVApp`* - Load an image and show it in a window (CanvasFrame)
  - *`Ex2MyFirstGUIApp`* - Simple GUI application build using Scala Swing. The application has two buttons on the left "Open Image" and "Process". The opened image is displayed in the middle. When "Process" button is pressed the image is flipped upside down and its red and blue channels are swapped.
  - *`Ex3LoadAndSave`* - Reading, saving, displaying, and drawing on an image.
* [Chapter 2: Manipulating the Pixels](src/main/scala/opencv_cookbook/chapter02)
  * *`Ex1Salt`* - Set individual, randomly selected, pixels to a fixed value. Use ImageJ's ImageProcessor to access pixels.
  * *`Ex2ColorReduce`* - Reduce colors in the image by modifying color values in all bands the same way.
  * *`Ex3Sharpen`* - Use kernel convolution to sharpen an image: `filter2D()`.
  * *`Ex4BlendImages`* - Blend two images using weighted addition: `cvAddWeighted()`.
  * *`Ex5ROILogo`* - Paste small image into a larger one using a region of interest: `IplROI` and `cvCopy()`.
* [Chapter 3: Processing Images with Classes](src/main/scala/opencv_cookbook/chapter03)
  * *`Ex1ColorDetector`* - Compare RGB colors to some target color, colors that are similar to the target color are assigned to white in the output image, other pixels are set to black.
  * *`Ex2ColorDetectorSimpleApplication`* - Same processing is the first example, but demonstrates simple UI.
  * *`Ex3ColorDetectorMVCApplication`* - Same processing is the first example, but demonstrates a more elaborated UI.
  * *`Ex4ConvertingColorSpaces`* - Similar to the first example, but color distance is calculated in `L*a*b*` color space. Illustrates use of `cvtColor` function.
* [Chapter 4: Counting the Pixels with Histograms](src/main/scala/opencv_cookbook/chapter04)
  * *`Ex1ComputeHistogram`* - Computes histogram using utility class `Histogram1D` and prints values to the screen. 
  * *`Ex2ComputeHistogramGraph`* - Displays a graph of a histogram created using utility class `Histogram1D`.
  * *`Ex3Threshold`* - Separates pixels in an image into a foreground and background using OpenCV `threshold()` method.
  * *`Ex4InvertLut`* - Creates inverted image by inverting its look-up table.
  * *`Ex5EqualizeHistogram`* - Enhances image using histogram equalization.
  * *`Ex6ContentDetectionGrayscale`* - Uses histogram of a region in an grayscale image to create 'template', looks through the whole image to detect pixels that are similar to that template. Illustrates use of method `cvCalcBackProject()`.
  * *`Ex7ContentDetectionColor`* - Uses histogram of region in an color image to create 'template', looks through the whole image to detect pixels that are similar to that template. Relies on the utility classes `ColorHistogram` and `ContentFinder`.
  * *`Ex8MeanShiftDetector`* - Uses histogram of region in an color image to create 'template', uses the mean shift algorithm to find best matching location of the 'template' in another image. Illustrates use of method `cvMeanShift()`.
  * *`Ex9ImageComparator`* - Computes image similarity measure using helper class `ImageComparator`. 
  * *`Histogram1D`* - Helper class that performs histogram and look-up table operations, correspond to part of the C++ class `Histogram1D` in the OpenCV Cookbook sample code. Illustrates use of OpenCV methods: `cvLUT()`, `cvEqualizeHist()`, `cvCreateHist()`, `cvCalcHist()`, `cvQueryHistValue_1D()`, `cvReleaseHist()`. 
  * *`ColorHistogram`* - Helper class that simplifies usage of method `cvCalcHist()` for color images.
  * *`ContentFinder`* - helper class for template matching using method `cvCalcBackProject()`.
  * *`ImageComparator`* - helper class that computes image similarity using `cvCompareHist()`.
* [Chapter 5: Transforming Images with Morphological Operations](src/main/scala/opencv_cookbook/chapter05)
  * *`Ex1ErodingAndDilating`* - Morphological erosion and dilation: `cvErode()` and `cvDilate()`.
  * *`Ex2OpeningAndClosing`* - Morphological opening and closing: `cvMorphologyEx()`.
  * *`Ex3EdgesAndCorners`* - Detection of edges and corners using morphological filters.
  * *`Ex4WatershedSegmentation`* - Image segmentation using the watershed algorithm.
  * *`Ex5GrabCut`* - Separates objects from background using `grabCut()`.
  * *`MorphoFeatures`* - Equivalent of C++ class of the same name, contains methods for morphological corner detection.
  * *`WatershedSegmenter`* - Helper class for section "Segmenting images using watersheds".
* [Chapter 6: Filtering the Images](src/main/scala/opencv_cookbook/chapter06)
  * *`Ex1LowPassFilter`* - Blur with a Gaussian filter.
  * *`Ex2MedianFilter`* - Remove noise with a median filter.
  * *`Ex3DirectionalFilters`* - Use of Sobel edge detection filter.
  * *`Ex4Laplacian`* - Edge detection using Laplacian filter.
  * *`LaplacianZC`* - Computation of Laplacian and zero-crossing, used in `Ex4Laplacian`.
* [Chapter 7: Extracting Lines, Contours, and Components](src/main/scala/opencv_cookbook/chapter07)
  * *`Ex1CannyOperator`* - Detect contours with the Canny operator.
  * *`Ex2HoughLines`* - Detect lines using standard Hough transform approach.
  * *`Ex3HoughLineSegments`* - Detect lines segments using probabilistic Hough transform approach.
  * *`Ex4HoughCircles`* - Detect circles using Hough transform approach.
  * *`Ex5ExtractContours`* - Extract contours from a binary image using connected components.
  * *`Ex6ShapeDescriptors`* - Compute various shape descriptors: bounding box, enclosing circle, approximate polygon, convex hull, center of mass.
  * *`LineFinder`* - Helper class to detect lines segments using probabilistic Hough transform approach, used in `Ex3HoughLineSegments`.
* [Chapter 8: Detecting Interest Points](src/main/scala/opencv_cookbook/chapter08)
  * *`Ex1HarrisCornerMap`* - Computes Harris corners strength image.
  * *`Ex2HarrisCornerDetector`* - Uses Harris Corner strength image to detect well localized corners, replacing several closely located detections (blurred) by a single one. Uses `HarrisDetector` helper class.
  * *`Ex3GoodFeaturesToTrack`* - Example of using the Good Features to Track detector.
  * *`Ex4FAST`* - Example of using the FAST detector.
  * *`Ex5SURF`* - Example of using the SURF detector.
  * *`Ex6SIFT`* - Example of using the SIFT detector.
  * *`HarrisDetector`* - Helper class for Harris Corner strength image to detection and localization. Closely located detections (blurred) are replaced by a single one.
* [Chapter 9: Detecting Interest Points](src/main/scala/opencv_cookbook/chapter09)
  * *`Ex2TemplateMatching`* - Finds best match between a small patch from first image (template) and a second image.. 
  * *`Ex7DescribingSURF`* - Computes SURF features,  extracts their descriptors, and finds best matching descriptors between two images of the same object. 
* [Chapter 10: Estimating Projective Relations in Images](src/main/scala/opencv_cookbook/chapter10)
  * *`Ex1FindChessboardCorners`* - Illustrates one of a camera calibration steps, detection of a chessboard pattern in a calibration board.
  * *`Ex2CalibrateCamera`* - Camera calibration example, shows how to correct geometric deformation that may be introduced by the optics. Uses the `CameraCalibrator` helper class.
  * *`Ex3ComputeFundamentalMatrix`* - Using features detected and matched between two images, compute fundamental matrix that describes projective relation between those two images.
  * *`Ex4MatchingUsingSampleConsensus`* - Illustrates use of RANSAC (RANdom SAmpling Consensus) strategy. Most of the computations are done by `RobustMatcher` helper class.
  * *`Ex5Homography`* - Another way of describing relationship between points in two images, using homography. Shows an example how two images of partial views on an object can be stitched together. Most of the computations are done by `RobustMatcher` helper class.
  * *`CameraCalibrator`* - Helper class implementing camera calibration algorithm.
  * *`RobustMatcher`* - Implements RANSAC based algorithm used by examples `Ex4MatchingUsingSampleConsensus` and `Ex5Homography`.
* [Chapter 11: Processing Video Sequences](src/main/scala/opencv_cookbook/chapter11)
  * *`Ex1ReadVideoSequence`* - Reads and displays a video.
  * *`Ex2ProcessVideoFrames`* - Processed frames in a video file using Canny edge detector; shows the output video on the screen. Uses helper class `VideoProcessor`.
  * *`Ex3WriteVideoSequence`* - Processed frames in a video file using Canny edge detector; writes output to a video file. Uses helper class `VideoProcessor`.
  * *`Ex4TrackingFeatures`* - Tracks moving objects in a video, marks tracked points in the video shown on the screen. Most of the implementation is in `FeatureTracker` helper class.
  * *`Ex5ForegroundSegmenter`* - Detect moving object in a video through background estimation. Background is modeled using "simple" moving average approach implemented in helper class "BGFBSEgmenter."
  * *`Ex6MOGMotionDetector`* - A more sophisticated motion detector that models background using Mixture of Gaussians approach.
  * *`BGFBSEgmenter`* - Separates "static" background from "moving" foreground by modeling background using a moving average. Used by example 'Ex5ForegroundSegmenter'.
  * *`FeatureTracker`* - Track moving features using an optical flow algorithm, used by example `Ex4TrackingFeatures`.
  * *`VideoProcessor`* - Helper class for dealing with video files, loading and applying processing to individual frames, used by examples: `Ex2ProcessVideoFrames`, `Ex3WriteVideoSequence`, `Ex4TrackingFeatures`, and `Ex5ForegroundSegmenter`.
* [Utilities](src/main/scala/opencv_cookbook)
  * *`OpenCVUtils`* - reading and writing of image files, display of images, drawing of features on images, conversions between OpenCV image and data representations.

Why Scala?
----------

[Scala](http://www.scala-lang.org) was chosen since it is more expressive than Java. You can achieve the same result with smaller amount of code. Smaller boilerplate code makes examples easier to read and understand. Compiled Scala code is fast, similar to Java and C++. 

Unlike Java or C++, Scala supports writing of scripts - code that can be executed without explicit compiling. Scala also has a console, called REPL, where single lines of code can be typed in and executed on a spot. Both of those features make prototyping of OpenCV-based programs easier in Scala than in Java. Last but not least, IDE support for Scala reached level of maturity allowing easy creation, modification, and execution of Scala code.In particular, the [Scala plugin](http://blog.jetbrains.com/scala/)  for [JetBrains IDEA](http://www.jetbrains.com/idea/) 
works very well. There is also Scala support for [Eclipse](http://scala-ide.org/index.html) and [NetBeans](https://github.com/dcaoyuan/nbscala).

| [Next: Chapter 1>](src/main/scala/opencv_cookbook/chapter01) |

[javacv]: https://github.com/bytedeco/javacv