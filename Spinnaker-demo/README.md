Spinnaker-demo
================

Example of using [JVM wrapper](https://github.com/bytedeco/javacpp-presets/tree/master/flycapture) for 
Point Grey [Spinnaker SDK](https://www.ptgrey.com/spinnaker-sdk).

Please address queries and questions to [JavaCPP discussion group](http://groups.google.com/group/javacpp-project).


Examples
-------

* C API examples:
    * `Acquisition_C` is an example how to enumerate cameras, start acquisition, and grab images.
    * `EnumerationEvents_C` example of handling system and interface events, like camera disconnect. 
    * `Sequencer_C` shows how to use the sequencer to grab images with various settings.


Build script
------------

* `build.sbt` - the main SBT configuration file.
* `project/build.properties` - version of SBT to use.
* `project/plugins.sbt` - plugins used for creation of Eclipse projects.


How to build and run using SBT
------------------------------

1. Install [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

2. Install [SBT](http://www.scala-sbt.org/)

3. To run examples, change to directory containing `Spinnaker-demo`. You can run examples by typing on the command line:
 
   ```
    %> sbt run
   ```
   
   SBT will download all needed dependencies, including OpenCV and spinnaker, and run the selected example code. 
