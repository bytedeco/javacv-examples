flandmark-demo
==============

Example of using [JVM wrapper](https://github.com/bytedeco/javacpp-presets/tree/master/flandmark) for 
[flandmark](https://github.com/uricamic/flandmark/blob/master/examples/example1.cpp) library.

Please address queries and questions to [JavaCPP discussion group](http://groups.google.com/group/javacpp-project).


Content
-------

Demo files:

* `src/main/java/flandmark/Example1.java` - Java version of flandmark's 
  [example1](https://github.com/uricamic/flandmark/blob/master/examples/example1.cpp)
* `face.jpg`, `flandmark_model.dat`, `haarcascade_frontalface_alt.xml` - sample data, 
  located in runtime directory per flandmark requirements
  
Face landmarks detected by example1:

![Face landmarks detected by example1](http://bytedeco.org/javacv-examples/images/flandmark-example1-output.png)
  
Build script files:

* `build.sbt` - the main SBT configuration file.
* `project/build.properties` - version of SBT to use.
* `project/plugins.sbt` - plugins used for creation of Eclipse projects.



How to build and run using SBT
------------------------------

1. Install [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

2. Install [SBT](http://www.scala-sbt.org/)

3. Run the example: change o directory containing this example and use SBT to
   build and run the example:

   ```
    %> sbt run
   ```

   It will download needed dependencies, including OpenCV, JavaCV, and flandmark, and run 
   the example code. 
