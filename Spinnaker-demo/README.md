Spinnaker-demo
================

Example of using [JVM wrapper](https://github.com/bytedeco/javacpp-presets/tree/master/flycapture) for Point
Grey [Spinnaker SDK](https://www.ptgrey.com/spinnaker-sdk).

Please address queries and questions to [JavaCPP discussion group](http://groups.google.com/group/javacpp-project).


Examples
-------

### C API examples in Java

* `Acquisition_C` - how to enumerate cameras, start acquisition, and grab images.
* `Enumeration_C` - how to enumerate interfaces and cameras.
* `Sequencer_C` - shows how to use the sequencer to grab images with various settings.
* `Utils` - helper functions that are reused by multiple examples. In original Spinnaker C examples corresponding C code
  is duplicated in each example. This reduces verbosity of the original C examples. Treat `Utils` is another source of
  example code.

To reduce verbosity of the original C examples some repeating code was moved to `spinnaker_c4j.Utils`

### C API examples in Scala

* `Acquisition_C` - shows how to acquire images.
* `Enumeration_C` - how to enumerate interfaces and cameras.
* `Enumeration_C_QuickSpin` - shows how to enumerate- interfaces and cameras using the QuickSpin API.
* `EnumerationEvents_C` - handling system and interface events, like camera disconnect.
* `ImageControl_C_QuickSpin` - shows how to apply custom image settings to the camera using the QuickSpin API.
* `NodeMapInfo_C` - shows how to retrieve node map information.
* `SaveToAvi_C` - shows how to create a video from a vector of images.
* `Sequencer_C` - shows how to use the sequencer to grab images with various settings.
* `Trigger_C` - shows how to trigger the camera.
* `helpers` - helper functions that are reused by multiple examples. In original Spinnaker C examples corresponding C
  code is duplicated in each example. This reduces verbosity of the original C examples. Treat `helpers` is another
  source of example code.

Implementation Notes
--------------------

### Dealing with C pointers and memory C allocation

Some parts of Spinnaker usage require explicit dealing with deallocating memory and resources
that are specific to Spinnaker API, like releasing camera handles.
Many routine usages of C pointers like wrappers `LongPointer` may refer to Java garbage collector to clean associated
memory.
In the examples, we are using more direct style of "closing" those pointers when no longer in use.
In JavaCPP implementation a `Pointer` implements `AutoCloseable`.
In Scala code, we use `scala.util.Using` to perform automatic resource management. Rather than simply having

```scala 3
val hDeviceSerialNumber = new spinNodeHandle()
val deviceSerialNumber = new BytePointer(MAX_BUFF_LEN)
val lenDeviceSerialNumber = new SizeTPointer(1)
.
..
```

you will see "Using" blocks like this

```scala 3
Using.Manager { use =>
  val hDeviceSerialNumber = use(new spinNodeHandle())
  val deviceSerialNumber = use(new BytePointer(MAX_BUFF_LEN))
  val lenDeviceSerialNumber = use(new SizeTPointer(1))
  .
..
}.get
```

At the end of each block, the pointers surrounded by `use` will be automatically closed.

### Error Handling

Method calls in the original C API return error values.
Dealing with those error codes is quite verbose and repetitive in the original C code. Here is an original example:

```c
err = spinSystemGetInstance(&hSystem);
if (err != SPINNAKER_ERR_SUCCESS)
{
    spinErrorGetLastMessage(lastErrorMessage, &lenLastErrorMessage);
    printf("Error: %s [%d]\n\n", lastErrorMessage, err);
    return err;
}
```

In our examples, we use simple helper methods to handle the errors, so the above will look like this:

```scala 3
exitOnError(spinSystemGetInstance(hSystem), "Unable to retrieve system instance.")
```

Another typical situation is when an the original C example method returns an error from the SDK method call

```c
if (err != SPINNAKER_ERR_SUCCESS)
{
    spinErrorGetLastMessage(lastErrorMessage, &lenLastErrorMessage);
    printf("Error: %s [%d]\n\n", lastErrorMessage, err);
    return err;
}
```

In our example's implementation we use a helper method `check` that will of that error code returned from SDK
indicates error and throws an exception containing detail error information. Resulting in simpler example code:

```scala 3
check(spinCameraInit(hCam), "Unable to initialize camera.")
```

### Helper methods

Many interactions with Spinnaker C API require a large amount of boilerplate code.
The Examples are simplified, compared to the original C version, by putting frequently used code in a helper methods.
You will find those helper methods in package `spinnaker_c.helpers`.

For instance a direct call to `spinEnumerationEntryGetIntValue` to get long value:

```scala 3
val value: Long = Using.Manager { use =>
  val enumEntryIntValue = use(new LongPointer(1)).put(0)
  check(
    spinEnumerationEntryGetIntValue(enumEntry, enumEntryIntValue),
    "Failed to retrieve enumeration entry int value"
  )
  enumEntryIntValue.get
}.get

```

Using a helper method:

```scala 3
val value: Long = enumerationEntryGetIntValue(enumEntry)
```

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

   SBT will download all necessary dependencies, including OpenCV and Spinnaker, and run the selected example code. 
