FlyCapture2-demo
================

Example of using [JVM wrapper](https://github.com/bytedeco/javacpp-presets/tree/master/flycapture) for 
Point Grey [FlyCapture SDK](https://www.ptgrey.com/flycapture-sdk).

Please address queries and questions to [JavaCPP discussion group](http://groups.google.com/group/javacpp-project).


Content
-------

There are two example projects
* `examples` contains command line examples
* `example_ui` an example of GUI application using [ScalaFX](http://www.scalafx.org/) toolkit.

### `examples`

Command line examples illustrate use of the C++ and C API:

* C++ API examples in `src/main/scala/flycapture/examples/cpp`:
    * `BusEventEx` - register for Bus Events such as Camera Arrival/Removal and Bus Resets.
    * `CameraPropertyInfoEx` - prints out property information from attached cameras.
    * `ExtendedShutterEx` - demonstrates how to enable and calculate extended integration times.
    * `FlyCapture2Test` - for each attached cameras, print info and capture a couple of images.
    * `GrabCallbackEx` - demonstrates how to set up an asynchronous image callback.
    * `SoftwareTriggerEx` - demonstrates use of basic asynchronous trigger capabilities. 
* C API examples in `src/main/scala/flycapture/examples/c` :
    * `FlyCapture2Test_C` - for each attached cameras, print info and capture a couple of images.
    
### `example_ui`

The `example_ui` project is a simple GUI application for showing live view and controlling camera settings is in directory `src/main/scala/flycapture/examples/cpp/snap`

![Sample GUI application](http://bytedeco.org/javacv-examples/images/FlyCapture_SnapApp.png)
    
### `check_macro`

Project `check_macro` contains some convenience scala macros for handling produced by FlyCapture SDK. Error codes are converted to exceptions. 

### Build script

* `build.sbt` - the main SBT configuration file.
* `project/build.properties` - version of SBT to use.
* `project/plugins.sbt` - plugins used for creation of Eclipse projects.



How to build and run using SBT
------------------------------

1. Install [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

2. Install [SBT](http://www.scala-sbt.org/)

3. To run examples, change to directory containing `flycapture-demo`. You can run UI example by typing on the command line:
 
   ```
    %> sbt example_ui/run
   ```
   
   To run command line examples type on the command line:
   ```
    %> sbt examples/run
   ```

   SBT will download all needed dependencies, including OpenCV and flycapture, and run the selected example code. 
