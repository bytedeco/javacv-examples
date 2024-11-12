package spinnaker_c

import org.bytedeco.javacpp.{BytePointer, IntPointer, LongPointer, SizeTPointer}
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.spinColorProcessingAlgorithm.SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR
import org.bytedeco.spinnaker.global.Spinnaker_C.spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR
import spinnaker_c.helpers.*

import scala.util.Using
import scala.util.control.Breaks.{break, breakable}

/**
 * ImageFormatControl_C_Quickspin.c shows how to apply custom image
 * settings to the camera using the QuickSpin API. QuickSpin is a subset of
 * the Spinnaker library that allows for simpler node access and control.
 *
 * This example demonstrates customizing offsets X and Y, width and height,
 * and the pixel format. Ensuring custom values fall within an acceptable
 * range is also touched on. Retrieving and setting node values using
 * QuickSpin is the only portion of the example that differs from
 * ImageFormatControl_C.
 *
 * A much wider range of topics is covered in the full Spinnaker examples than
 * in the QuickSpin ones. There are only enough QuickSpin examples to
 * demonstrate node access and to get started with the API; please see full
 * Spinnaker examples for further or specific knowledge on a topic.
 */
object ImageControl_C_QuickSpin {

  private val MAX_BUFF_LEN = 256

  /**
   * Example entry point; please see Enumeration_C example for more in-depth
   * comments on preparing and cleaning up the system.
   */
  def main(args: Array[String]): Unit = {
    Using.Manager { use =>

      // Retrieve singleton reference to system object
      val hSystem = use(new spinSystem())
      exitOnError(spinSystemGetInstance(hSystem), "Unable to retrieve system instance.")

      try {
        // Print out current library version
        printLibraryVersion(hSystem)

        // Retrieve list of cameras from the system
        val hCameraList = use(new spinCameraList())
        exitOnError(spinCameraListCreateEmpty(hCameraList), "Unable to create camera list.")

        try {

          exitOnError(spinSystemGetCameras(hSystem, hCameraList), "Unable to retrieve camera list.")

          // Retrieve number of cameras
          val numCameras = use(new SizeTPointer(1))
          exitOnError(spinCameraListGetSize(hCameraList, numCameras), "Unable to retrieve number of cameras.")
          println("Number of cameras detected: " + numCameras.get + "\n")

          // Run example on each camera
          for i <- 0 until numCameras.get.toInt do {
            println(s"Running example for camera $i...")

            // Select camera
            val hCamera = use(new spinCamera())
            exitOnError(spinCameraListGet(hCameraList, i, hCamera), s"Unable to retrieve camera $i from list.")

            try
              runSingleCamera(hCamera)
            finally
              // Release camera
              printOnError(spinCameraRelease(hCamera), "Error releasing camera.")
              println(s"Camera $i example complete...\n")
          }

        } finally
          // Clear and destroy camera list before releasing system
          exitOnError(spinCameraListClear(hCameraList), "Unable to clear camera list.")
          exitOnError(spinCameraListDestroy(hCameraList), "Unable to destroy camera list.")

      } finally
        // Release system
        exitOnError(spinSystemReleaseInstance(hSystem), "Unable to release system instance.")
    }.get
  }

  /**
   * This function acts as the body of the example; please see NodeMapInfo_C
   * example for more in-depth comments on setting up cameras.
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  def runSingleCamera(hCam: spinCamera): Unit = Using.Manager { use =>
    // Retrieve nodemap
    val hNodeMapTLDevice = use(new spinNodeMapHandle())
    check(spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice), "Unable to retrieve TL device nodemap .")

    check(printDeviceInfo(hNodeMapTLDevice), "")

    // Initialize camera
    check(spinCameraInit(hCam), "Unable to initialize camera.")

    try
      // Pre-fetch TL device nodes
      val qsD = use(new quickSpinTLDevice())

      check(quickSpinTLDeviceInit(hCam, qsD), "Unable to pre-fetch TL device nodes.")

      // Pre-fetch GenICam nodes
      val qs = use(new quickSpin())

      check(quickSpinInit(hCam, qs), "Unable to pre-fetch GenICam nodes.")

      // Configure custom image settings
      configureCustomImageSettings(qs)

      // Acquire images
      acquireImages(hCam, qs, qsD)
    finally
      // Deinitialize camera
      check(spinCameraDeInit(hCam), "Unable to deinitialize camera.")
  }.get

  /**
   * This function configures a number of settings on the camera including
   * offsets X and Y, width, height, and pixel format. These settings must be
   * applied before spinCameraBeginAcquisition() is called; otherwise, those
   * nodes would be read only. Also, it is important to note that settings are
   * applied immediately. This means if you plan to reduce the width and move
   * the x offset accordingly, you need to apply such changes in the appropriate
   * order.
   */
  private def configureCustomImageSettings(qs: quickSpin): Unit = Using.Manager { use =>
    println("\n\n*** CONFIGURING CUSTOM IMAGE SETTINGS ***\n")

    //
    // Apply mono 8 pixel format
    //
    // *** NOTES ***
    // In QuickSpin, enumeration nodes are as easy to set as other node types.
    // This is because enum values representing each entry node are added to
    // the API.
    //
    // It is important to note that there are two sets of functions that might
    // produce erroneous results if they were to be mixed up. The first two
    // functions, spinEnumerationSetIntValue() and
    // spinEnumerationEntryGetIntValue(), use the integer values stored on each
    // individual cameras. The second two, spinEnumerationSetEnumValue() and
    // spinEnumerationEntryGetEnumValue(), use enum values defined in the
    // Spinnaker library. The int and enum values will most likely be
    // different from another.
    //
    check(
      spinEnumerationSetEnumValue(qs.PixelFormat, spinPixelFormatEnums.PixelFormat_Mono8.value),
      "Unable to set pixel format."
    )

    println("Pixel format set to 'mono8'...")

    //
    // Apply minimum to offset X
    //
    // *** NOTES ***
    // Numeric nodes have both a minimum and maximum. A minimum is retrieved
    // with the method GetMin(). Sometimes it can be important to check
    // minimums to ensure that your desired value is within range.
    //
    // Notice that the node type is explicitly expressed in the name of the
    // second and third functions. Although node types are not expressed in
    // node handles, knowing the node type is important to interacting with
    // a node in any meaningful way.
    //
    val offsetXMin = use(new LongPointer(1)).put(0)
    check(spinIntegerGetMin(qs.OffsetX, offsetXMin), "Unable to get offset x.")
    check(spinIntegerSetValue(qs.OffsetX, offsetXMin.get), "Unable to set offset x.")
    println(s"Offset X set to ${offsetXMin.get()}...")

    //
    // Apply minimum to offset Y
    //
    // *** NOTES ***
    // It is often desirable to check the increment as well. The increment
    // is a number of which a desired value must be a multiple. Certain
    // nodes, such as those corresponding to offsets X and Y, have an
    // increment of 1, which basically means that any value within range
    // is appropriate. The increment is retrieved with the method
    // spinIntegerGetInc().
    //
    // The offsets both hold integer values. As such, if a double were input
    // as an argument or if a string function were used, problems would
    // occur.
    //

    val offsetYMin = use(new LongPointer(1))
    check(spinIntegerGetMax(qs.OffsetY, offsetYMin), "Unable to get offset y.")

    check(spinIntegerSetValue(qs.OffsetY, offsetYMin.get), "Unable to set offset y.")

    println(s"Offset Y set to ${offsetYMin.get()}...")

    //
    // Set maximum width
    //
    // *** NOTES ***
    // Other nodes, such as those corresponding to image width and height,
    // might have an increment other than 1. In these cases, it can be
    // important to check that the desired value is a multiple of the
    // increment.
    //
    // This is often the case for width and height nodes. However, because
    // these nodes are being set to their maximums, there is no real reason
    // to check against the increment.
    //
    val widthToSet = use(new LongPointer(1))
    check(spinIntegerGetMax(qs.Width, widthToSet), "Unable to get offset width.")
    check(spinIntegerSetValue(qs.Width, widthToSet.get), "Unable to set offset width.")
    println(s"Width set to ${widthToSet.get}...")

    //
    // Set maximum height
    //
    // *** NOTES ***
    // A maximum is retrieved with the method spinIntegerGetMax(). A node's
    // minimum and maximum should always be multiples of the increment.
    //
    val heightToSet = use(new LongPointer(1))
    check(spinIntegerGetMax(qs.Height, heightToSet), "Unable to get offset height.")
    check(spinIntegerSetValue(qs.Height, heightToSet.get), "Unable to set offset height.")
    println(s"Height set to ${heightToSet.get}...")
  }.get

  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  def acquireImages(hCam: spinCamera, qs: quickSpin, qsD: quickSpinTLDevice): Unit =
    Using.Manager { use =>
      println("\n*** IMAGE ACQUISITION ***\n")

      // Set acquisition mode to continuous
      check(
        spinEnumerationSetEnumValue(qs.AcquisitionMode, spinAcquisitionModeEnums.AcquisitionMode_Continuous.value),
        "Unable to set acquisition mode to continuous (entry int value setting)."
      )

      println("Acquisition mode set to continuous...")

      // Begin acquiring images
      check(spinCameraBeginAcquisition(hCam), "Unable to begin image acquisition")

      println("Acquiring images...")

      // Retrieve device serial number for filename
      val deviceSerialNumber    = use(new BytePointer(MAX_BUFF_LEN))
      val lenDeviceSerialNumber = use(new SizeTPointer(1)).put(MAX_BUFF_LEN)

      if spinStringGetValue(qsD.DeviceSerialNumber, deviceSerialNumber, lenDeviceSerialNumber) != spinError.SPINNAKER_ERR_SUCCESS
      then
        deviceSerialNumber.putString("")
        lenDeviceSerialNumber.put(0)
      else
        println("Device serial number retrieved as " + deviceSerialNumber.getString.trim + "...")

      //
      // Create Image Processor context for post-processing images
      //
      val hImageProcessor = use(new spinImageProcessor())
      check(spinImageProcessorCreate(hImageProcessor), "Unable to create image processor. Non-fatal error.")

      //
      // Set default image processor color processing method
      //
      // *** NOTES ***
      // By default, if no specific color processing algorithm is set, the image
      // processor will default to NEAREST_NEIGHBOR method.
      //
      check(
        spinImageProcessorSetColorProcessing(hImageProcessor, SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR),
        "Unable to set image processor color processing method. Non-fatal error."
      )

      val k_numImages = 10
      for (imageCnt <- 0 until k_numImages) do
        breakable {

          // Retrieve next received image
          val hResultImage = use(new spinImage()) // NULL

          val err1 = spinCameraGetNextImageEx(hCam, 1000, hResultImage)
          if printOnError(err1, "Unable to get next image. Non-fatal error.") then
            break

          // Ensure image completion
          val isIncomplete = use(new BytePointer(1))
          var hasFailed    = false
          val err2         = spinImageIsIncomplete(hResultImage, isIncomplete)
          if printOnError(err2, "Unable to determine image completion. Non-fatal error.") then
            hasFailed = true

          // Check image for completion
          if isIncomplete.getBool then {
            val imageStatus = use(new IntPointer(1L)).put(SPINNAKER_IMAGE_STATUS_NO_ERROR.value)
            val err3        = spinImageGetStatus(hResultImage, imageStatus)
            if !printOnError(
                err3,
                "Unable to retrieve image status. Non-fatal error. " + findImageStatusNameByValue(imageStatus.get)
              )
            then
              println("Image incomplete with image status " + findImageStatusNameByValue(imageStatus.get) + "...")
            hasFailed = true
          }
          // Release incomplete or failed image
          if hasFailed then {
            val err4 = spinImageRelease(hResultImage)
            printOnError(err4, "Unable to release image. Non-fatal error.")
            break
          }

          // Print image information
          println("Grabbed image " + imageCnt)
          // Retrieve image width
          val width = use(new SizeTPointer(1))
          val err5  = spinImageGetWidth(hResultImage, width)
          if printOnError(err5, "spinImageGetWidth()") then
            println("width  = unknown")
          else
            println("width  = " + width.get)
          // Retrieve image height
          val height = use(new SizeTPointer(1))
          val err6   = spinImageGetHeight(hResultImage, height)
          if printOnError(err6, "spinImageGetHeight()") then
            println("height = unknown")
          else
            println("height = " + height.get)

          // Convert image to mono 8
          //
          // *** NOTES ***
          // Images not gotten from a camera directly must be created and
          // destroyed. This includes any image copies, conversions, or
          // otherwise. Basically, if the image was gotten, it should be
          // released, if it was created, it needs to be destroyed.
          //
          // Images can be converted between pixel formats by using the
          // appropriate enumeration value. Unlike the original image, the
          // converted one does not need to be released as it does not affect the
          // camera buffer.
          //
          // Optionally, the color processing algorithm can also be set using
          // the alternate spinImageConvertEx() function.
          //
          // *** LATER ***
          // The converted image was created, so it must be destroyed to avoid
          // memory leaks.
          //
          val hConvertedImage = use(new spinImage()) // NULL

          val err7 = spinImageCreateEmpty(hConvertedImage)
          if printOnError(err7, "Unable to create image. Non-fatal error.") then
            hasFailed = true
          val err8 = spinImageProcessorConvert(
            hImageProcessor,
            hResultImage,
            hConvertedImage,
            spinPixelFormatEnums.PixelFormat_Mono8
          )
          printOnError(err8, "Unable to convert image. Non-fatal error.")

          // Save image
          if !hasFailed then {
            // Create a unique filename
            val filename =
              if lenDeviceSerialNumber.get == 0 then
                "Acquisition-C-" + imageCnt + ".jpg"
              else
                "Acquisition-C-" + deviceSerialNumber.getString.trim + "-" + imageCnt + ".jpg"
            //
            // Save image
            //
            // *** NOTES ***
            // The standard practice of the examples is to use device serial
            // numbers to keep images of one device from overwriting those of
            // another.
            //
            val err9 = spinImageSave(
              hConvertedImage,
              use(new BytePointer(filename)),
              spinImageFileFormat.SPINNAKER_IMAGE_FILE_FORMAT_JPEG
            )
            if !printOnError(err9, "Unable to save image. Non-fatal error.") then
              println("Image saved at " + filename + "\n")
          }

          //
          // Destroy converted image
          printOnError(spinImageDestroy(hConvertedImage), "Unable to destroy image. Non-fatal error.")
          // Release image from camera
          printOnError(spinImageRelease(hResultImage), "Unable to release image. Non-fatal error.")
        }

      // Destroy Image Processor context
      printOnError(spinImageProcessorDestroy(hImageProcessor), "Unable to destroy image processor. Non-fatal error.")

      // End acquisition
      check(spinCameraEndAcquisition(hCam), "Unable to end acquisition.")
    }.get

}
