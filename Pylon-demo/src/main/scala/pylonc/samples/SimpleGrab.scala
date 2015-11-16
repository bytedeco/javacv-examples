package pylonc.samples

import org.bytedeco.javacpp.Pylon5_C._
import org.bytedeco.javacpp.{BoolPointer, BytePointer, IntPointer}

/**
 * This sample illustrates how to use the `PylonDeviceGrabSingleFrame()` convenience
 * method for grabbing images in a loop. `PylonDeviceGrabSingleFrame()` grabs one
 * single frame in single frame mode.
 *
 * Grabbing in single frame acquisition mode is the easiest way to grab images. Note: in single frame
 * mode the maximum frame rate of the camera can't be achieved. The full frame
 * rate can be achieved by setting the camera to the continuous frame acquisition
 * mode and by grabbing in overlapped mode, i.e., image acquisition is done in parallel
 * with image processing. This is illustrated in the `OverlappedGrab` sample program.
 *
 * This sample is based on Basler C Sample `SimpleGrab`.
 *
 */
object SimpleGrab extends App {


  // Before using any pylon methods, the pylon runtime must be initialized.
  println("Initializing Pylon SDK.")
  PylonInitialize()

  // Number of available devices.
  val numDevices  = new IntPointer(1)
  // Handle for the pylon device.
  val hDev        = new IntPointer(1)
  // Number of images to grab.
  val numGrabs    = 10
  // Size of an image frame in bytes.
  val payloadSize = new IntPointer(1)


  try {

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

    /* Set the pixel format to Mono8, where gray values will be output as 8 bit values for each pixel. */
    /* ... Check first to see if the device supports the Mono8 format. */
    if (!PylonDeviceFeatureIsAvailable(hDev, "EnumEntry_PixelFormat_Mono8")) {
      /* Feature is not available. */
      println("Device doesn't support the Mono8 pixel format.")
      PylonTerminate()
      System.exit(1)
    }
    /* ... Set the pixel format to Mono8. */
    check(PylonDeviceFeatureFromString(hDev, "PixelFormat", "Mono8"))

    /* Disable acquisition start trigger if available */
    if (PylonDeviceFeatureIsAvailable(hDev, "EnumEntry_TriggerSelector_AcquisitionStart")) {
      check(PylonDeviceFeatureFromString(hDev, "TriggerSelector", "AcquisitionStart"))
      check(PylonDeviceFeatureFromString(hDev, "TriggerMode", "Off"))
    }

    /* Disable frame burst start trigger if available. */
    if (PylonDeviceFeatureIsAvailable(hDev, "EnumEntry_TriggerSelector_FrameBurstStart")) {
      check(PylonDeviceFeatureFromString(hDev, "TriggerSelector", "FrameBurstStart"))
      check(PylonDeviceFeatureFromString(hDev, "TriggerMode", "Off"))
    }

    /* Disable frame start trigger if available */
    if (PylonDeviceFeatureIsAvailable(hDev, "EnumEntry_TriggerSelector_FrameStart")) {
      check(PylonDeviceFeatureFromString(hDev, "TriggerSelector", "FrameStart"))
      check(PylonDeviceFeatureFromString(hDev, "TriggerMode", "Off"))
    }

    /* For GigE cameras, we recommend increasing the packet size for better
       performance. If the network adapter supports jumbo frames, set the packet
       size to a value > 1500, e.g., to 8192. In this sample, we only set the packet size
       to 1500. */
    /* ... Check first to see if the GigE camera packet size parameter is supported
        and if it is writable. */
    if (PylonDeviceFeatureIsWritable(hDev, "GevSCPSPacketSize")) {
      /* ... The device supports the packet size feature. Set a value. */
      check(PylonDeviceSetIntegerFeature(hDev, "GevSCPSPacketSize", 1500))
    }

    /* Determine the required size of the grab buffer. */
    check(PylonDeviceGetIntegerFeatureInt32(hDev, "PayloadSize", payloadSize))

    /* Allocate memory for grabbing. */
    val imgBuf = new BytePointer(payloadSize.get())

    /* Grab some images in a loop. */
    for (i <- 0 until numGrabs) {
      val grabResult = new PylonGrabResult_t()
      val bufferReady = new BoolPointer(1)

      /* Grab one single frame from stream channel 0. The
      camera is set to single frame acquisition mode.
      Wait up to 500 ms for the image to be grabbed. */
      val res = PylonDeviceGrabSingleFrame(hDev, 0, imgBuf, payloadSize.get, grabResult, bufferReady, 500)
      if ( /* GENAPI_E_OK */ 0 == res && !bufferReady.get) {
        /* Timeout occurred. */
        println(s"Frame ${i + 1}: timeout")
      }
      check(res)

      val status = grabResult.Status

      /* Check to see if the image was grabbed successfully. */
      if (status == Grabbed) {
        /* Success. Perform image processing. */
        val (min, max) = getMinMax(imgBuf, grabResult.SizeX, grabResult.SizeY)
        println(s"Grabbed frame ${i + 1}. Min. gray value = $min, Max. gray value = $max")

        //        /* Display image */
        //        check(PylonImageWindowDisplayImageGrabResult(0, grabResult))

      } else if (status == Failed) {
        printf(s"Frame %d wasn't grabbed successfully.  Error code = 0x%08X\n", i + 1, grabResult.ErrorCode)
        val length = new IntPointer(1)
        GenApiGetLastErrorMessage(null, length)
        val errorMessage = new BytePointer(length.get())
        GenApiGetLastErrorMessage(errorMessage, length)
        println("Pylon error: " + errorMessage.getString)

      }
    }

    /* Clean up. Close and release the pylon device. */

    check(PylonDeviceClose(hDev))
    check(PylonDestroyDevice(hDev))

  } finally {
    // Shut down the pylon runtime system. Don't call any pylon method after calling PylonTerminate().
    println("Terminating Pylon SDK.")
    PylonTerminate()
  }


  //--------------------------------------------------------------------------
  // Methods
  //

  /**
   * @throws Exception when return code is different than `GENAPI_E_OK`.
   */
  def check(errorCode: Long): Unit = {
    if (errorCode != 0 /* GENAPI_E_OK */ ) {

      val length = new IntPointer(1)
      GenApiGetLastErrorMessage(null, length)
      val errorMessage = new BytePointer(length.get())
      GenApiGetLastErrorMessage(errorMessage, length)

      throw new Exception("Pylon error: " + errorMessage.getString)
    }
  }

  /* Simple "image processing" function returning the minimum and maximum gray
     value of an 8 bit gray value image. */
  def getMinMax(pImg: BytePointer, width: Int, height: Int): (Int, Int) = {
    var min: Int = 255
    var max: Int = 0
    for (i <- 0 until width * height) {
      val v = pImg.get(i) & 0xFF
      if (v > max) max = v
      if (v < min) min = v
    }
    (min, max)
  }

}
