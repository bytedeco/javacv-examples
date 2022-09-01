LibRaw Demo
===========


Example of using [JavaCPP Wrapper](https://github.com/bytedeco/javacpp-presets/tree/master/libraw) for
the [LibRaw](https://www.libraw.org/) library.

Please address queries and questions to [JavaCPP discussion group](http://groups.google.com/group/javacpp-project).

Examples
--------

### Java

#### LibRawDemo4J

Basic example of using reading raw image, processing, and writing processed image

### Scala

#### RawProcessor and LibRawDemo

Example of a higher level wrapper for the LibRaw raw processor and example of its use.

#### DemosaicDemo

Example of performing only democaicing of a Bayer pattern, when an image is saved in a non-proprietary format. This could be a raw image captured with the [Spinnaker Wrapper](https://github.com/bytedeco/javacpp-presets/tree/master/spinnaker)

Sample Data
-----------

Images that are used by the demos

Build script
------------

* `build.sbt` - the main SBT configuration file.
* `project/build.properties` - version of SBT to use.

How to build and run using SBT
------------------------------

1. Install [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

2. Install [SBT](http://www.scala-sbt.org/)

3. To run examples, change to directory containing `LibRaw-demo`. You can run examples by typing on the command line:

   ```
    %> sbt run
   ```

   SBT will download all needed dependencies and run the selected example code. 

You can also run a specific example providing its full name (with the package), for instance:

```
%> sbt "runMain libraw.examples.LibRawDemo4J"
```