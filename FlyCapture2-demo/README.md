FlyCapture2-demo
================

Example of using [JVM wrapper](https://github.com/bytedeco/javacpp-presets/tree/master/flycapture) for 
Point Grey [FlyCapture SDK](http://ww2.ptgrey.com/sdk/flycap).

Please address queries and questions to [JavaCPP discussion group](http://groups.google.com/group/javacpp-project).


Content
-------

Demo files:

* C++ API examples in `src/main/scala/flycapture/examples/cpp`:
    * `BusEventEx` - register for Bus Events such as Camera Arrival/Removal and Bus Resets.
    * `CameraPropertyInfoEx` - prints out property information from attached cameras.
    * `ExtendedShutterEx` - demonstrates how to enable and calculate extended integration times.
    * `FlyCapture2Test` - for each attached cameras, print info and capture a couple of images.
    * `GrabCallbackEx` - demonstrates how to set up an asynchronous image callback.
    * `SoftwareTriggerEx` - demonstrates use of basic asynchronous trigger capabilities. 
* C API examples in `src/main/scala/flycapture/examples/c` :
    * `FlyCapture2Test_C` - for each attached cameras, print info and capture a couple of images.
* Simple GUI application for showing live view and controlling camera settings is in directory `src/main/scala/flycapture/examples/cpp/snap`

![Sample GUI application](http://bytedeco.org/javacv-examples/images/FlyCapture_SnapApp.png)
    
Build script:

* `build.sbt` - the main SBT configuration file.
* `project/build.properties` - version of SBT to use.
* `project/plugins.sbt` - plugins used for creation of Eclipse projects.



How to build and run using SBT
------------------------------

1. Install [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

2. Install [SBT](http://www.scala-sbt.org/)

3. Run the example: change o directory containing this example and use SBT to
   build and run the examples:

   ```
    %> sbt run
   ```

   It will download needed dependencies, including OpenCV and flycapture, and run 
   the example code. 
