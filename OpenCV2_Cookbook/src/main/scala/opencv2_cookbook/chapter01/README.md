
Chapter 1: Playing with Images
==============================

| [<Previous: Introduction](/OpenCV2_Cookbook) |  [Next: Chapter 2>](/OpenCV2_Cookbook/src/main/scala/opencv2_cookbook/chapter02) |

There are only two things needed to start working with OpenCV from a Java platform:
 
* JDK - Java Development Kit
* Build tool like SBT, Gradle, Maven or similar.

Installation of OpenCV is not required, all needed Java and native binaries can be automatically downloaded by the build tool.

Here we will focus on [SBT](http://www.scala-sbt.org/) as a build tool (description of Gradle or other setup may be added in the future, if you are willing to contribute setup and description it will be added here)

One of the important features of SBT, compared for instance to Maven, is the ability to easy run example code from command line. You can also use SBT to interactively execute commands with classpath initialized to your project classpath. This is a very good way to experiment with JavaCV and other projects.

If you prefer to use IDE, you can also open the SBT project directly in [IntelliJ IDEA](https://www.jetbrains.com/idea/) and [NetBeans](https://netbeans.org/) using their Scala plugins. If you prefer to use Eclipse, you can use SBT to generate Eclipse project configuration using commend `eclipse`.

Assuming that you already have JDK installed, the only thing necessary to run examples is to [install SBT](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.htm). SBT will download JavaCV and OpenCV binaries, so do not need to install them.

### Getting the OpenCV Cookbook Examples for JavaCV ###

You can get the Cookbook Examples sources either by cloning the [GitHub repository](https://github.com/bytedeco/javacv-examples) or by downloading then as a ZIP archive clicking on the [Download ZIP](https://github.com/bytedeco/javacv-examples/archive/master.zip) button on the [GitHub page](https://github.com/bytedeco/javacv-examples).

The examples are in the sub-directory [OpenCV2_Cookbook](/OpenCV2_Cookbook). 


### Running Examples from Command Prompt using SBT ###


1. Open command prompt or a terminal.
2. Change directory to the location of `OpenCV2_Cookbook`
3. Type `sbt` to start `SBT`
4. If this is the first time you run SBT it may take a while to download all needed dependencies. Once everything is ready you should see prompt: `sbt:opencv2_cookbook>`
5. Type `run` and you will see a long list of available examples:

```
Multiple main classes detected, select one to run:

 [1] opencv2_cookbook.chapter01.Ex1MyFirstOpenCVApp
 [2] opencv2_cookbook.chapter01.Ex2MyFirstGUIApp
 [3] opencv2_cookbook.chapter01.Ex2MyFirstGUIAppJava

...

Enter number:
```

Type `1` and you should see example `opencv2_cookbook.chapter01.Ex1MyFirstOpenCVApp` running

![Ex1MyFirstOpenCVApp](http://bytedeco.org/javacv-examples/images/OpenCV2_Cookbook/Ch1_Ex1MyFirstOpenCVApp.png)

### Using IntelliJ IDEA ###

Make sure that you have Scala plugin installed, it provides support for SBT too. Scala is one of the standard plugins available in  the Community (free) and Ultimate editions of IDEA.

1. Select `File` > `Open`. 
2. Navigate to `OpenCV2_Cookbook` directory
3. Select `build.sbt` and click `OK`
4. You will see "Import Project from SBT", you can select "Download sources and docs" and "Download SBT sources and docs" 



Loading, Displaying, and Saving Images with JavaCV
--------------------------------------------------

A simple example of loading and displaying an image using JavaCV is in class `opencv2_cookbook.chapter01.Ex1MyFirstOpenCVApp`:

``` scala
import javax.swing.JFrame._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacv.CanvasFrame

object Ex1MyFirstOpenCVApp extends App {

  // Read an image
  val image = imread("data/boldt.jpg")
  if (image.empty()) {
    // error handling
    // no image has been created...
    // possibly display an error message
    // and quit the application
    println("Error reading image...")
    System.exit(0)
  }

  // Create image window named "My Image".
  //
  // Note that you need to indicate to CanvasFrame not to apply gamma correction,
  // by setting gamma to 1, otherwise the image will not look correct.
  val canvas = new CanvasFrame("My Image", 1)

  // Request closing of the application when the image window is closed
  canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)

  // Show image on window
  canvas.showImage(image)
}
```

It is based on the example on page 18 of the cookbook. Note how `CanvasFrame` from JavaCV API is used to display the image.

Loading Images
--------------

JavaCV methods for loading images and saving images are based on based on OpenCV. They are in the class `com.googlecode.javacv.cpp.opencv_highgui`. Note that class is named starting with small letter to look similar to the C++ module `opencv_highgui` where OpenCV method for reading and writing images are located.

OpenCV has a couple of classes to represent images. Some are ore obsolete, like `IplImage` and `CvMat`. We will use the currently recommended `Mat`.

`Mat` images are loaded using method [`imread`](http://docs.opencv.org/modules/highgui/doc/reading_and_writing_images_and_video.html?highlight=imread#Mat%20imread%28const%20string&%20filename,%20int%20flags%29), it takes one or two parameters. The first one is a file name, the second is a conversion parameter, frequently used to load color images area gray scale.
Here are some examples.

``` scala
import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacpp.opencv_highgui._

val image1: Mat = imread("data/boldt.jpg")
val image2: Mat = imread("data/boldt.jpg", CV_LOAD_IMAGE_COLOR)
val image3: Mat = imread("data/boldt.jpg", CV_LOAD_IMAGE_GRAYSCALE)
```
The default value for the conversion parameter is `CV_LOAD_IMAGE_COLOR`.

If image cannot be loaded both `imread` will return `null`. You may want to wrap a call to `imread` in a method that throws an exception if an image cannot be loaded.

``` scala
def load(file: File, flags: Int = CV_LOAD_IMAGE_GRAYSCALE): Mat = {
    // Verify file
    if (!file.exists()) {
        throw new FileNotFoundException("Image file does not exist: " +
                                        file.getAbsolutePath)
    }
    // Read input image
    val image = imread(file.getAbsolutePath, flags)
    if (image == null) {
        throw new IOException("Couldn't load image: " + file.getAbsolutePath)
    }
    // Return loaded image
    image
}
```

Saving Images
-------------

The method `imwrite(filename, image)` saves the image to the specified file.
The image format is chosen based on the filename extension.

``` scala
 imwrite("my_image.png", image1)
```

Displaying Images
-----------------

The easy way to display image using JavaCV is to use `CanvasFrame` as in the example [`Ex1MyFirstOpenCVApp`](/OpenCV2_Cookbook/src/main/scala/opencv2_cookbook/chapter01/Ex1MyFirstOpenCVApp.scala). It shows the image in a new window.

JavaCV adds to  `Mat` a method `getBufferedImage` to convert OpenCV data to Java's `java.awt.image.BufferedImage` that can be displayed using standard Java approach.
You can see example of that in `Ex2MyFirstGUIApp`, see next section.


Creating a GUI application using Scala Swing
--------------------------------------------

It is not an intention of this module to describe how to create GUI applications in Scala or Java. However for the sake of completeness an equivalent of a Qt GUI application presented in the Cookbook is provided. As the original, it is a simple frame with two buttons "Open Image" and "Process" on the left. Loaded image is displayed in the middle of the frame.
The example is using Scala Swing framework, which may be interesting in its own right.
The framework enables writing a more concise Swing code, in this particular example Java core requires about 30% more characters to achieve the same result.

**Note:** The new GUI framework for building UIs is JavaFX. Working in scale you may consider [ScalaFX](http://scalafx.org) over Scala Swing. 

You can find code for the `Ex2MyFirstGUIApp` example [here](/OpenCV2_Cookbook/src/main/scala/opencv2_cookbook/chapter01/Ex2MyFirstGUIApp.scala).
For the sake of comparison an equivalent Java code is also [Ex2MyFirstGUIAppJava.java](/OpenCV2_Cookbook/src/main/java/opencv2_cookbook/chapter01/Ex2MyFirstGUIAppJava.java).

![Ex2MyFirstGUIApp](http://bytedeco.org/javacv-examples/images/OpenCV2_Cookbook/Ch1_Ex2MyFirstGUIApp.png)

| [<Previous: Introduction](/OpenCV2_Cookbook) | **Chapter 1: Playing with Images** | [Next: Chapter 2>](/OpenCV2_Cookbook/src/main/scala/opencv2_cookbook/chapter02) |
