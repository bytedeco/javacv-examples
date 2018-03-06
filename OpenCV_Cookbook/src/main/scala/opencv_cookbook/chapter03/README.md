Chapter 3: Processing Images with Classes
=========================================

*JavaCV versions of C++ examples from Chapter 3 of the book "OpenCV Computer Vision Applications Programming Cookbook" titled "Processing Images with Classes"*

| [<Previous: Chapter 2](/OpenCV_Cookbook/src/main/scala/opencv_cookbook/chapter02) | [Next: Chapter 4>](/OpenCV_Cookbook/src/main/scala/opencv_cookbook/chapter04) |


Chapter 3 discusses some of software design patterns:
 * Strategy design pattern
 * Singleton design pattern
 * Controller
 * Model-View-Controller pattern
 
The last section additionally overviews conversion between color spaces.  

For a more thorough coverage of software design patters refer to, for instance, a book by Eric Freeman, Elisabeth Freeman, Kathy Sierra and Bert Bates, *Head First Design Patterns*.

Here we cover only examples related to color operations.

Color Detector
-------------

The key image processing example considered in Chapter 3 is color detection. Colors that are similar to some target color are assigned to white in the output image, other pixels are set to black. To achieve this you need to iterate over all pixels in the image and check their color. We use JavaCPP's Indexer. Color is represented by three numbers. We will use version of Indexer's `get` method that allows reading an array of values, here 3 values. To help deal with color operations we use a class `ColorRGB`. Following code iterates though pixels in the image and tests is a color is within a specified distance from a target color:  

``` scala
// Indexer for input image
val srcI = image.createIndexer().asInstanceOf[UByteIndexer]

// Create output image and itx indexer
val dest = new Mat(image.rows, image.cols, org.bytedeco.javacpp.opencv_core.CV_8U)
val destI = dest.createIndexer().asInstanceOf[UByteIndexer]

// Iterate through pixels and check if their distance from the target color is
// withing the distance threshold, if it is set `dest` to 255.
val brg = new Array[Byte](3)
for (y <- 0 until image.rows) {
  for (x <- 0 until image.cols) {
    srcI.get(y, x, brg)
    val c = ColorRGB(brg)
    val t = if (distance(c) < colorDistanceThreshold) (255 & 0xFF).toByte else 0.toByte
    destI.put(y, x, t)
  }
}    
```

The distance is computed similar way as in the Cookbook, current color is compared to the target color using the city block distance:
``` scala
private def distance(color: ColorRGB): Double = {
  abs(targetColor.red - color.red) + 
    abs(targetColor.green - color.green) + 
    abs(targetColor.blue - color.blue)
}    
```

Example 1
---------

The first example, [Ex1ColorDetector](Ex1ColorDetector.scala), corresponds to section "Using the Strategy Pattern in algorithm design". It uses [ColorDetector](ColorDetector.scala) described above. Here is an example of an input and an output image: 

![boldt.jpg](http://bytedeco.org/javacv-examples/images/OpenCV_Cookbook/boldt.jpg)
![Ch3_Ex1_output.png](http://bytedeco.org/javacv-examples/images/OpenCV_Cookbook/Ch3_Ex1_output.png)

Example 2 - Simple UI
---------------------

The second example, [Ex2ColorDetectorSimpleApplication](Ex2ColorDetectorSimpleApplication.scala), is a simple UI for loading an image and detection of colors using the same [ColorDetector](ColorDetector.scala) as above. The example corresponds to sections "Using a Controller to communicate with processing modules" and "Using the Singleton design pattern" in Chapter 3. In Scala singletons can be easily created using keyword `object` in place of keyword `class`. For instance class ColorDetectorController
``` scala
class ColorDetectorController {
    private val colorDetector = new ColorDetector()
    ...
}
```
can be easily converted to a singleton, Scala makes sure that there is only a single instance of a particular `object`
``` scala
object ColorDetectorController {
    private val colorDetector = new ColorDetector()
    ...
}
```
![Ch3_Ex2.png](http://bytedeco.org/javacv-examples/images/OpenCV_Cookbook/Ch3_Ex2.png)

Example 3 - Model-View-Controller
---------------------------------

The third example, [Ex3ColorDetectorMVCApplication](Ex3ColorDetectorMVCApplication.scala), corresponds to section "Using a Model-View-Controller architecture to design an application". 

![Ch3_Ex3.png](http://bytedeco.org/javacv-examples/images/OpenCV_Cookbook/Ch3_Ex3.png)

Example 4 - Converting Color Spaces
-----------------------------------

The last example, [Ex4ConvertingColorSpaces](Ex4ConvertingColorSpaces.scala), corresponds to last section in Chapter 3: "Converting color spaces". 

Conversion between color spaces can be done with a single call 

``` scala
cvtColor(rgbImage, labImage, COLOR_BGR2Lab)
```

The simplicity can be deceiving. Function `cvtColor` expects that the output image is of the same type as input. The an RGB color is typically represented by three 8-bit unsigned integers, that is, each band, red, green, and blue, takes integer values in the range from 0 to 255. However, a color in CIE `L*a*b*` color space is typically represented by floating point numbers. `L*` is in range 0 to 100, `a*` and `b*` are in range -100 to 100. Since `cvtColor` expects input and output image to be of the same type, things get a bit complicated. If the input is a regular RGB color image, unsigned 8-bit bit per channel, `cvtColor` scales and shifts `L*`, `a*`, and `b*` values to fit within unsigned 8-bit bit range 0-255: `L <- L*255/100`,  `a <- a + 128`, `b <- b + 128`. One of the premises of `L*a*b*` is that color differences there are "linear" with human perception. Note that when converting to `L*a*b*` unsigned 8-bit bit, the `L*` was scaled differently than `a*` and `b*`, you need to keep this in mind when making color distance calculations. To avoid all those issues you can first convert RGB image to use floating point channels than convert to `L*a*b*` using `cvtColor`. in this case `L*a*b*` are not scaled and are within their normal range.

Since example in the OpenCV Cookbook used 8-bit per channel color image, the JavaCV example does the same, but compensates for that in distance calculations by scaling back the `L*` channel to be comparable with `a*` and `b*`. 

One additional thing to keep in mind is that the OpenCV stores RGB channels in reverse order, that is, they are actually stored as BGR. Notice that code snippet above is using flag `COLOR_BGR2Lab`. The `L*a*b*` images are stored in normal order. Unlike the Example 1 that used [ColorDetector](ColorDetector.scala), this example is is using [ColorDetectorLab](ColorDetectorLab.scala). Now, before iterating over image, the input is first converted to `L*a*b*`

``` scala
// Convert input from RGB to L*a*b* color space
// Note that since destination image uses 8 bit unsigned integers, original L*a*b* values
// are converted to fit 0-255 range
//       L <- L*255/100
//       a <- a + 128
//       b <- b + 128
val labImage = new Mat()
cvtColor(rgbImage, labImage, COLOR_BGR2Lab)
```

The channel order and scaling of `L*` is dealt with in the distance calculations:

``` scala
private def distance(color: Triple): Double = {
  // When converting to 8-bit representation L* is scaled, a* and b* are only shifted.
  // To make the distance calculations more proportional we scale here L* difference back.
  abs(_targetLab.lAsUInt8 - color.l) / 255d * 100d +
    abs(_targetLab.aAsUInt8 - color.a) +
    abs(_targetLab.bAsUInt8 - color.b)
}    
``` 

Since distances in the `L*a*b*` color space are different than in RGB color space, the `L*a*b*` Color Detector's results a different than in the first example.

![Ch3_Ex4Lab_output.png](http://bytedeco.org/javacv-examples/images/OpenCV_Cookbook/Ch3_Ex4Lab_output.png)

| [<Previous: Chapter 2](/OpenCV_Cookbook/src/main/scala/opencv_cookbook/chapter02) | [Next: Chapter 4>](/OpenCV_Cookbook/src/main/scala/opencv_cookbook/chapter04) |
