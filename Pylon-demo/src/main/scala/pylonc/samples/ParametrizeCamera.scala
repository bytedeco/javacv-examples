package pylonc.samples

import org.bytedeco.javacpp.Pylon4_C._
import org.bytedeco.javacpp._

/**
 * This sample illustrates how to read and write the different camera parameter types.
 *
 * It is based on Basler C Sample `ParametrizeCamera`.
 *
 */
object ParametrizeCamera extends App {

  // Before using any pylon methods, the pylon runtime must be initialized.
  println("Initializing Pylon SDK.")
  PylonInitialize()

  try {

    // Number of available devices.
    val numDevices = new IntPointer(1)
    // Handle for the pylon device.
    val hDev = new IntPointer(1)

    /* Enumerate all camera devices. You must call
    PylonEnumerateDevices() before creating a device! */
    check(PylonEnumerateDevices(numDevices))
    if (0 == numDevices.get()) {
      println("No devices found!")
      /* Before exiting a program, PylonTerminate() should be called to release
         all pylon related resources. */
      PylonTerminate()
      System.exit(1)
    }

    println("Found " + numDevices.get() + " devices.")

    /* Get a handle for the first device found.  */
    check(PylonCreateDeviceByIndex(0, hDev))

    /* Before using the device, it must be opened. Open it for configuring
    parameters and for grabbing images. */
    check(PylonDeviceOpen(hDev, PYLONC_ACCESS_MODE_CONTROL | PYLONC_ACCESS_MODE_STREAM))

    /* Print out the name of the camera we are using. */
    if (PylonDeviceFeatureIsReadable(hDev, "DeviceModelName")) {
      val buf = new Array[Byte](256)
      check(PylonDeviceFeatureToString(hDev, "DeviceModelName", buf, Array(buf.length)))
      println("Using camera: " + new String(buf).trim)
    }

    /* Demonstrate how to check the accessibility of a feature. */
    demonstrateAccessibilityCheck(hDev)
    println()

    /* Demonstrate how to handle integer camera parameters. */
    demonstrateIntFeature(hDev)
    println()
    demonstrateInt32Feature(hDev)
    println()

    /* Demonstrate how to handle floating point camera parameters. */
    demonstrateFloatFeature(hDev)
    println()

    /* Demonstrate how to handle boolean camera parameters. */
    demonstrateBooleanFeature(hDev)
    println()

    /* Each feature can be read as a string and also set as a string. */
    demonstrateFromStringToString(hDev)
    println()

    /* Demonstrate how to handle enumeration camera parameters. */
    demonstrateEnumFeature(hDev)
    println()

    /* Demonstrate how to execute actions. */
    demonstrateCommandFeature(hDev)

    /* Clean up. Close and release the pylon device. */
    PylonDeviceClose(hDev)
    PylonDestroyDevice(hDev)

  } finally {
    // Shut down the pylon runtime system. Don't call any pylon method after calling PylonTerminate().
    println("Terminating Pylon SDK.")
    PylonTerminate()
  }


  ///* This function demonstrates how to retrieve the error message for the last failed
  // function call. */
  //void printErrorAndExit( GENAPIC_RESULT errc )
  //{
  //  char *errMsg;
  //  size_t length;
  //
  //  /* Retrieve the error message.
  //  ... First find out how big the buffer must be, */
  //  GenApiGetLastErrorMessage( NULL, &length );
  //  errMsg = (char*) malloc( length );
  //  /* ... and retrieve the message. */
  //  GenApiGetLastErrorMessage( errMsg, &length );
  //
  //  fprintf( stderr, "%s (%#08x).\n", errMsg, errc);
  //  free( errMsg);
  //
  //  /* Retrieve the more details about the error
  //  ... First find out how big the buffer must be, */
  //  GenApiGetLastErrorDetail( NULL, &length );
  //  errMsg = (char*) malloc( length );
  //  /* ... and retrieve the message. */
  //  GenApiGetLastErrorDetail( errMsg, &length );
  //
  //  fprintf( stderr, "%s\n", errMsg);
  //  free( errMsg);
  //
  //  PylonTerminate();  /* Releases all pylon resources */
  //  pressEnterToExit();
  //
  //  exit(EXIT_FAILURE);

  //--------------------------------------------------------------------------
  // Methods
  //

  def lastPylonError(): String = {
    val length = new IntPointer(1)
    GenApiGetLastErrorMessage(null, length)
    val errorMessage = new BytePointer(length.get())
    GenApiGetLastErrorMessage(errorMessage, length)
    errorMessage.getString
  }

  /**
   * @throws Exception when return code is different than `GENAPI_E_OK`.
   */
  def check(errorCode: Long): Unit = {
    if (errorCode != 0 /* GENAPI_E_OK */ ) {

      val length = new IntPointer(1)
      GenApiGetLastErrorMessage(null, length)
      val errorMessage = new BytePointer(length.get())
      GenApiGetLastErrorMessage(errorMessage, length)

      throw new Exception("Pylon error: " + lastPylonError())
    }
  }

  /* This function demonstrates how to check the presence, readability, and writability
     of a feature. */
  def demonstrateAccessibilityCheck(hDev: IntPointer) {

    def implementedStr(v: Boolean) = if (v) "is" else "isn't"

    // Output of the check functions
    var v: Boolean = false

    // Check to see if a feature is implemented at all.
    v = PylonDeviceFeatureIsImplemented(hDev, "Width")
    println(s"The 'Width' feature ${implementedStr(v)} implemented")
    v = PylonDeviceFeatureIsImplemented(hDev, "MyCustomFeature")
    println(s"The 'MyCustomFeature' feature ${implementedStr(v)} implemented")


    // Although a feature is implemented by the device, it might not be available
    //   with the device in its current state. Check to see if the feature is currently
    //   available. The PylonDeviceFeatureIsAvailable sets val to 0 if either the feature
    //   is not implemented or if the feature is not currently available.
    v = PylonDeviceFeatureIsAvailable(hDev, "BinningVertical")
    println(s"The 'BinningVertical' feature ${implementedStr(v)} available")

    // If a feature is available, it could be read-only, write-only, or both
    //   readable and writable. Use the PylonDeviceFeatureIsReadable() and the
    //   PylonDeviceFeatureIsWritable() functions(). It is safe to call these functions
    //   for features that are currently not available or not implemented by the device.
    //   A feature that is not available or not implemented is neither readable nor writable.
    //   The readability and writability of a feature can change depending on the current
    //   state of the device. For example, the Width parameter might not be writable when
    //   the camera is acquiring images.
    v = PylonDeviceFeatureIsReadable(hDev, "Width")
    println(s"The 'Width' feature ${implementedStr(v)} readable")
    v = PylonDeviceFeatureIsImplemented(hDev, "MyCustomFeature")
    println(s"The 'MyCustomFeature' feature ${implementedStr(v)} readable")

    v = PylonDeviceFeatureIsWritable(hDev, "Width")
    println(s"The 'Width' feature ${implementedStr(v)} writable")

    println()
  }


  /* This function demonstrates how to handle integer camera parameters. */
  def demonstrateIntFeature(hDev: IntPointer): Unit = {
    /* Name of the feature used in this sample: AOI Width */
    val featureName = "Width"
    /* Properties of the feature */
    val value = new LongPointer(1)
    val min = new LongPointer(1)
    val max = new LongPointer(1)
    val incr = new LongPointer(1)


    if (PylonDeviceFeatureIsReadable(hDev, featureName)) {
      /*
        Query the current value, the allowed value range, and the increment of the feature.
        For some integer features, you are not allowed to set every value within the
        value range. For example, for some cameras the Width parameter must be a multiple
        of 2. These constraints are expressed by the increment value. Valid values
        follow the rule: val >= min && val <= max && val == min + n * inc. */
      check(PylonDeviceGetIntegerFeatureMin(hDev, featureName, min)) /* Get the minimum value. */
      check(PylonDeviceGetIntegerFeatureMax(hDev, featureName, max)) /* Get the maximum value. */
      check(PylonDeviceGetIntegerFeatureInc(hDev, featureName, incr)) /* Get the increment value. */
      check(PylonDeviceGetIntegerFeature(hDev, featureName, value)) /* Get the current value. */

      println(s"$featureName: min= ${min.get}  max= ${max.get}  incr=${incr.get}  Value=${value.get}")

      if (PylonDeviceFeatureIsWritable(hDev, featureName)) {
        /* Set the Width half-way between minimum and maximum. */
        check(PylonDeviceSetIntegerFeature(hDev, featureName, min.get + (max.get - min.get) / incr.get / 2 * incr.get))
      }
      else {
        println(s"The $featureName feature is not writable.")
      }
    }
    else
      println(s"The $featureName feature is not readable.")
  }


  /* The integer functions illustrated above take 64 bit integers as output parameters. There are variants
     of the integer functions that accept 32 bit integers instead. The Get.... functions return
     an error when the value returned by the device doesn't fit into a 32 bit integer. */
  def demonstrateInt32Feature(hDev: IntPointer): Unit = {
    /* Name of the feature used in this sample: AOI height */
    val featureName = "Height"
    /* Properties of the feature */
    val value = new IntPointer(1)
    val min = new IntPointer(1)
    val max = new IntPointer(1)
    val incr = new IntPointer(1)

    if (PylonDeviceFeatureIsReadable(hDev, featureName)) {
      /*
         Query the current value, the allowed value range, and the increment of the feature.
         For some integer features, you are not allowed to set every value within the
         value range. For example, for some cameras the Width parameter must be a multiple
         of 2. These constraints are expressed by the increment value. Valid values
         follow the rule: val >= min && val <= max && val == min + n * inc. */
      check(PylonDeviceGetIntegerFeatureMinInt32(hDev, featureName, min)) /* Get the minimum value. */
      check(PylonDeviceGetIntegerFeatureMaxInt32(hDev, featureName, max)) /* Get the maximum value. */
      check(PylonDeviceGetIntegerFeatureIncInt32(hDev, featureName, incr)) /* Get the increment value. */
      check(PylonDeviceGetIntegerFeatureInt32(hDev, featureName, value)) /* Get the current value. */

      printf("%s: min= %d  max= %d  incr=%d  Value=%d\n", featureName, min.get, max.get, incr.get, value.get)

      if (PylonDeviceFeatureIsWritable(hDev, featureName)) {
        /* Set the value to half its maximum  */
        check(PylonDeviceSetIntegerFeatureInt32(hDev, featureName, min.get + (max.get - min.get) / incr.get / 2 * incr.get))

      } else
        printf("The %s feature is not writable.\n", featureName)
    } else
      printf("The %s feature is not readable.\n", featureName)
  }


  /* Some features are floating point features. This function illustrates how to set and get floating
     point parameters. */
  def demonstrateFloatFeature(hDev: IntPointer): Unit = {
    /* The name of the feature used */
    val featureName = "Gamma"
    /* Value range and current value */
    val min = new DoublePointer(1)
    val max = new DoublePointer(1)
    val value = new DoublePointer(1)

    if (PylonDeviceFeatureIsReadable(hDev, featureName)) {
      /* Query the value range and the current value. */
      check(PylonDeviceGetFloatFeatureMin(hDev, featureName, min))

      check(PylonDeviceGetFloatFeatureMax(hDev, featureName, max))

      check(PylonDeviceGetFloatFeature(hDev, featureName, value))


      printf("%s: min = %4.2f, max = %4.2f, value = %4.2f\n", featureName, min.get, max.get, value.get)

      /* Set the value to half its maximum. */
      val isWritable = PylonDeviceFeatureIsWritable(hDev, featureName)
      if (isWritable) {
        value.put(0.5 * (min.get + max.get))
        printf("Setting %s to %4.2f\n", featureName, value.get)
        check(PylonDeviceSetFloatFeature(hDev, featureName, value.get))

      } else
        printf("The %s feature is not writable.\n", featureName)
    } else
      printf("The %s feature is not readable.\n", featureName)
  }


  /* Some features are boolean features that can be switched on and off.
     This function illustrates how to access boolean features. */
  def demonstrateBooleanFeature(hDev: IntPointer): Unit = {
    /* The name of the feature */
    val featureName = "GammaEnable"
    /* The value of the feature */
    val value = new BoolPointer(1)

    /* Check to see if the feature is writable. */
    val isWritable = PylonDeviceFeatureIsWritable(hDev, featureName)

    if (isWritable) {
      /* Retrieve the current state of the feature. */
      check(PylonDeviceGetBooleanFeature(hDev, featureName, value))

      printf("The %s features is %s\n", featureName, if (value.get) "on" else "off")

      /* Set a new value. */
      value.put(value.get) /* New value */
      printf("Switching the %s feature %s\n", featureName, if (value.get) "on" else "off")
      check(PylonDeviceSetBooleanFeature(hDev, featureName, value.get))
    } else
      printf("The %s feature isn't writable\n", featureName)
  }


  /*
    Regardless of the parameter's type, any parameter value can be retrieved as a string. Each parameter
    can be set by passing in a string correspondingly. This function illustrates how to set and get the
    Width parameter as string. As demonstrated above, the Width parameter is of the integer type.
    */
  def demonstrateFromStringToString(hDev: IntPointer): Unit = {
    /* The name of the feature */
    val featureName = "Width"

    val len = new IntPointer(1)

    /* Get the value of a feature as a string. Normally getting the value consits of 3 steps:
       1.) Determine the required buffer size.
       2.) Allocate the buffer.
       3.) Retrieve the value. */
    /* ... Get the required buffer size. The size is queried by
           passing a NULL pointer as a pointer to the buffer. */
    check(PylonDeviceFeatureToString(hDev, featureName, null, len))

    /* ... Len is set to the required buffer size (terminating zero included).
           Allocate the memory and retrieve the string. */
    val buf = new Array[Byte](len.get)
    check(PylonDeviceFeatureToString(hDev, featureName, buf, Array(buf.length)))

    printf("%s: %s\n", featureName, new String(buf))

    /* You are not necessarily required to query the buffer size in advance. If the buffer is
       big enough, passing in a buffer and a pointer to its length will work.
       When the buffer is too small, an error is returned. */

    /* Passing in a buffer that is too small */
    val smallBuf = new Array[Byte](1)
    val res = PylonDeviceFeatureToString(hDev, featureName, smallBuf, Array(smallBuf.length))
    if (res == 0xC2000003 /* GENAPI_E_INSUFFICIENT_BUFFER */ ) {
      /* The buffer was too small. The required size is indicated by len. */
      printf("Buffer is too small for the value of '%s'. The required buffer size is %d\n", featureName, len.get)
      println(lastPylonError())
    } else {
      println("Got: " + new String(smallBuf))
      /* Unexpected return value */
      check(res)
    }
    println()

    /* Passing in a buffer with sufficient size. */
    val properBuf = new Array[Byte](32)
    check(PylonDeviceFeatureToString(hDev, featureName, properBuf, Array(properBuf.length)))



    /* A feature can be set as a string using the PylonDeviceFeatureFromString() function.
       If the content of a string can not be converted to the type of the feature, an
       error is returned. */
    val res1 = PylonDeviceFeatureFromString(hDev, featureName, "fourty-two") /* Can not be converted to an integer */
    if (res1 != 0L) {
      println(lastPylonError())
    }
  }


  /* There are camera features that behave like enumerations. These features can take a value from a fixed
     set of possible values. One example is the pixel format feature. This function illustrates how to deal with
     enumeration features.

  */
  def demonstrateEnumFeature(hDev: IntPointer): Unit = {
    /* The current value of the feature */
    val value = new Array[Byte](64)

    /* The allowed values for an enumeration feature are represented as strings. Use the
    PylonDeviceFeatureFromString() and PylonDeviceFeatureToString() methods for setting and getting
    the value of an enumeration feature. */


    /* Get the current value of the enumeration feature. */
    check(PylonDeviceFeatureToString(hDev, "PixelFormat", value, Array(value.length)))

    printf("PixelFormat: %s\n", new String(value))

    /*
      For an enumeration feature, the pylon Viewer's "Feature Documentation" window lists the the
      names of the possible values. Some of the values might not be supported by the device.
      To check if a certain "SomeValue" value for a "SomeFeature" feature can be set, call the
      PylonDeviceFeatureIsAvailable() function with "EnumEntry_SomeFeature_SomeValue" as an argument.
    */
    /* Check to see if the Mono8 pixel format can be set. */
    val supportsMono8 = PylonDeviceFeatureIsAvailable(hDev, "EnumEntry_PixelFormat_Mono8")
    printf("Mono8 %s a supported value for the PixelFormat feature\n", if (supportsMono8) "is" else "isn't")

    /* Check to see if the YUV422Packed pixel format can be set. */
    val supportsYUV422Packed = PylonDeviceFeatureIsAvailable(hDev, "EnumEntry_PixelFormat_YUV422Packed")
    printf("YUV422Packed %s a supported value for the PixelFormat feature\n", if (supportsYUV422Packed) "is" else "isn't")

    /* Check to see if the Mono16 pixel format can be set. */
    val supportsMono16 = PylonDeviceFeatureIsAvailable(hDev, "EnumEntry_PixelFormat_Mono16")
    printf("Mono16 %s a supported value for the PixelFormat feature\n", if (supportsMono16) "is" else "isn't")


    /* Before writing a value, we recommend checking to see if the enumeration feature is
       currently writable. */
    val isWritable = PylonDeviceFeatureIsWritable(hDev, "PixelFormat")
    if (isWritable) {
      /* The PixelFormat feature is writable, set it to one of the supported values. */
      if (supportsMono16) {
        printf("Setting PixelFormat to Mono16\n")
        check(PylonDeviceFeatureFromString(hDev, "PixelFormat", "Mono16"))

      }
      else if (supportsYUV422Packed) {
        printf("Setting PixelFormat to YUV422Packed\n")
        check(PylonDeviceFeatureFromString(hDev, "PixelFormat", "YUV422Packed"))

      }
      else if (supportsMono8) {
        printf("Setting PixelFormat to Mono8\n")
        check(PylonDeviceFeatureFromString(hDev, "PixelFormat", "Mono8"))

      }

      /* Reset the PixelFormat feature to its previous value. */
      PylonDeviceFeatureFromString(hDev, "PixelFormat", new String(value))
    }

  }


  /* There are camera features, such as starting image acquisition, that represent a command.
     This function that loads the factory settings, illustrates how to execute a command feature.  */
  def demonstrateCommandFeature(hDev: IntPointer): Unit = {
    /* Before executing the user set load command, the user set selector must be
       set to the default set. Since we are focusing on the command feature,
       we skip the recommended steps for checking the availability of the user set
       related features and values. */

    /* Choose the default configuration set (with one of the factory setups chosen). */
    check(PylonDeviceFeatureFromString(hDev, "UserSetSelector", "Default"))


    /* Execute the user set load command. */
    printf("Loading the default set.\n")
    check(PylonDeviceExecuteCommandFeature(hDev, "UserSetLoad"))

  }


}
