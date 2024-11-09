package spinnaker_c

import org.bytedeco.javacpp.{BytePointer, IntPointer, LongPointer, SizeTPointer}
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR
import spinnaker_c.helpers.*

import java.io.File
import scala.util.Using
import scala.util.control.Breaks.{break, breakable}

/**
 * Acquisition_C.c shows how to acquire images. It relies on
 * information provided in the Enumeration_C example. Following this, check
 * out the NodeMapInfo_C example if you haven't already. It explores
 * retrieving information from various node types.
 *
 * This example touches on the preparation and cleanup of a camera just
 * before and just after the acquisition of images. Image retrieval and
 * conversion, grabbing image data, and saving images are all covered.
 *
 * Once comfortable with Acquisition_C and NodeMapInfo_C, we suggest checking
 * out AcquisitionMultipleCamera_C, NodeMapCallback_C, or SaveToAvi_C.
 * AcquisitionMultipleCamera_C demonstrates simultaneously acquiring images
 * from a number of cameras, NodeMapCallback_C acts as a good introduction to
 * programming with callbacks and events, and SaveToAvi_C exhibits video
 * creation.
 */
object Acquisition_C {
  private val MAX_BUFF_LEN = 256

  // Use the following enum to select the stream mode
  enum StreamMode(val name: String) {

    /** Teledyne Gige Vision stream mode is the default stream mode for spinview which is supported on Windows */
    case STREAM_MODE_TELEDYNE_GIGE_VISION extends StreamMode("TeledyneGigeVision")

    /** Light Weight Filter driver is our legacy driver which is supported on Windows */
    case STREAM_MODE_PGRLWF extends StreamMode("LWF")

    /** Socket is supported for MacOS and Linux, and uses native OS network sockets instead of a filter driver */
    case STREAM_MODE_SOCKET extends StreamMode("Socket")
  }

  private val chosenStreamMode = StreamMode.STREAM_MODE_TELEDYNE_GIGE_VISION

  def main(args: Array[String]): Unit = {

    // Since this application saves images in the current folder,
    // we must ensure that we have permission to write to this folder.
    // If we do not have permission, fail right away.
    if !new File(".").canWrite then {
      System.out.println("Failed to create file in current folder.  Please check permissions.")
      return
    }

    Using.Manager { use =>
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
      } catch {
        case ex: Throwable =>
          ex.printStackTrace()
      } finally
        // Release system
        exitOnError(spinSystemReleaseInstance(hSystem), "Unable to release system instance.")
    }

    println("\nDone!\n")
  }

  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  def runSingleCamera(hCam: spinCamera): Unit = Using.Manager { use =>

    // Retrieve TL device nodemap and print device information
    val hNodeMapTLDevice = use(new spinNodeMapHandle())
    check(spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice), "Unable to retrieve TL device nodemap .")

    check(printDeviceInfo(hNodeMapTLDevice), "")

    // Initialize camera
    check(spinCameraInit(hCam), "Unable to initialize camera.")

    try
      // Retrieve GenICam nodemap
      val hNodeMap = use(new spinNodeMapHandle)
      check(spinCameraGetNodeMap(hCam, hNodeMap), "Unable to retrieve GenICam nodemap.")

      // Set stream mode
      setStreamMode(hCam)

      // Acquire images
      acquireImages(hCam, hNodeMap, hNodeMapTLDevice)
    finally
      // Deinitialize camera
      check(spinCameraDeInit(hCam), "Unable to deinitialize camera.")
  }.get

  private def setStreamMode(hCam: spinCamera): Unit = Using.Manager { use =>
    breakable {
      // *** NOTES ***
      // Enumeration nodes are slightly more complicated to set than other
      // nodes. This is because setting an enumeration node requires working
      // with two nodes instead of the usual one.
      //
      // As such, there are a number of steps to setting an enumeration node:
      // retrieve the enumeration node from the nodemap, retrieve the desired
      // entry node from the enumeration node, retrieve the integer value
      // from the entry node, and set the new value of the enumeration node
      // with the integer value from the entry node.
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

      //    spinError err = SPINNAKER_ERR_SUCCESS;
      //    char lastErrorMessage[MAX_BUFF_LEN];
      //    size_t lenLastErrorMessage = MAX_BUFF_LEN;
      //    size_t displayNameLength = MAX_BUFF_LEN;
      //
      val hNodeMapStreamDevice = use(new spinNodeMapHandle())
      check(spinCameraGetTLStreamNodeMap(hCam, hNodeMapStreamDevice), "spinCameraGetTLStreamNodeMap failed")

      // The node "StreamMode" is only available for GEV cameras.
      val hStreamMode = use(new spinNodeHandle())
      val err         = spinNodeMapGetNode(hNodeMapStreamDevice, use(new BytePointer("StreamMode")), hStreamMode)
      if err != spinError.SPINNAKER_ERR_SUCCESS then
        println("Cannot get 'StreamMode' node, ignoring")
        break

      // Skip setting stream mode if the node is inaccessible.
      if hStreamMode == null || isReadable(hStreamMode, "StreamMode") || isWritable(hStreamMode, "StreamMode") then
        println("Cannot access 'StreamMode' node, ignoring")
        break

      // Retrieve the desired entry node value from the entry node
      val streamModeCustom = enumerationEntryGetIntValue(hStreamMode, chosenStreamMode.name)

      // Set integer as new value for enumeration node
      check(spinEnumerationSetIntValue(hStreamMode, streamModeCustom), "spinEnumerationSetIntValue failed")

      // Print our result
      val hCurrentEntryNode = use(new spinNodeHandle())
      check(spinEnumerationGetCurrentEntry(hStreamMode, hCurrentEntryNode), "spinEnumerationGetCurrentEntry failed")

      val currentEntrySymbolic       = use(new BytePointer(MAX_BUFF_LEN))
      val currentEntrySymbolicLength = use(new SizeTPointer(1)).put(MAX_BUFF_LEN)
      check(
        spinEnumerationEntryGetSymbolic(hCurrentEntryNode, currentEntrySymbolic, currentEntrySymbolicLength),
        "spinEnumerationEntryGetSymbolic failed"
      )

      println(s"Stream Mode set to ${currentEntrySymbolic.getString}")
    }
  }.get

  def enumerationEntryGetIntValue(hEnumNode: spinNodeHandle, enumEntryName: String): Long = Using.Manager { use =>
    val hEnumEntry = use(new spinNodeHandle())
    check(
      spinEnumerationGetEntryByName(hEnumNode, use(new BytePointer(enumEntryName)), hEnumEntry),
      "spinEnumerationGetEntryByName failed"
    )

    // Retrieve the desired entry node value from the entry node
    val streamModeCustom = use(new LongPointer(1)).put(0)
    check(
      spinEnumerationEntryGetIntValue(hEnumEntry, streamModeCustom),
      "spinEnumerationEntryGetIntValue failed"
    )
    streamModeCustom.get()
  }.get

  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  def acquireImages(hCam: spinCamera, hNodeMap: spinNodeMapHandle, hNodeMapTLDevice: spinNodeMapHandle): Unit =
    Using.Manager { use =>
      println("\n*** IMAGE ACQUISITION ***\n")

      //
      // Set acquisition mode to continuous
      //
      // *** NOTES ***
      // Because the example acquires and saves 10 images, setting acquisition
      // mode to continuous lets the example finish. If set to single frame
      // or multiframe (at a lower number of images), the example would just
      // hang. This would happen because the example has been written to acquire
      // 10 images while the camera would have been programmed to retrieve
      // less than that.
      //
      // Setting the value of an enumeration node is slightly more complicated
      // than other node types, and especially so in C. It can roughly be broken
      // down into four steps: first, the enumeration node is retrieved from the
      // nodemap; second, the entry node is retrieved from the enumeration node;
      // third, an integer is retrieved from the entry node; and finally, the
      // integer is set as the new value of the enumeration node.
      ////
      // It is important to note that there are two sets of functions that might
      // produce erroneous results if they were to be mixed up. The first two
      // functions, spinEnumerationSetIntValue() and
      // spinEnumerationEntryGetIntValue(), use the integer values stored on each
      // individual cameras. The second two, spinEnumerationSetEnumValue() and
      // spinEnumerationEntryGetEnumValue(), use enum values defined in the
      // Spinnaker library. The int and enum values will most likely be
      // different from another.
      //

      // Retrieve enumeration node from nodemap
      setEnumerationNodeValue(hNodeMap, "AcquisitionMode", "Continuous")
      println("Acquisition mode set to continuous...")

      //
      // Begin acquiring images
      //
      // *** NOTES ***
      // What happens when the camera begins acquiring images depends on the
      // acquisition mode. Single frame captures only a single image, multi
      // frame catures a set number of images, and continuous captures a
      // continuous stream of images. Because the example calls for the retrieval
      // of 10 images, continuous mode has been set.
      //
      // *** LATER ***
      // Image acquisition must be ended when no more images are needed.
      //
      check(spinCameraBeginAcquisition(hCam), "Unable to begin image acquisition.")
      System.out.println("Acquiring images...")

      //
      // Retrieve device serial number for filename
      //
      // *** NOTES ***
      // The device serial number is retrieved in order to keep cameras from
      // overwriting one another. Grabbing image IDs could also accomplish this.
      //
      val deviceSerialNumber = nodeGetStringValueOpt(hNodeMapTLDevice, "DeviceSerialNumber").getOrElse("")
      printf("Device serial number retrieved as %s...\n", deviceSerialNumber);
      printf("\n");

      //
      // Create Image Processor context for post-processing images
      //
      val hImageProcessor = use(new spinImageProcessor())
      printOnError(spinImageProcessorCreate(hImageProcessor), "Unable to create image processor. Non-fatal error.")

      //
      // Set default image processor color processing method
      //
      // *** NOTES ***
      // By default, if no specific color processing algorithm is set, the image
      // processor will default to NEAREST_NEIGHBOR method.
      //
      printOnError(
        spinImageProcessorSetColorProcessing(
          hImageProcessor,
          spinColorProcessingAlgorithm.SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR
        ),
        "Unable to set image processor color processing method. Non-fatal error."
      )

      // Retrieve, convert, and save images// Retrieve, convert, and save images
      val k_numImages = 10
      for (imageCnt <- 0 until k_numImages) do {
        breakable {
          //
          // Retrieve next received image
          //
          // *** NOTES ***
          // Capturing an image houses images on the camera buffer. Trying to
          // capture an image that does not exist will hang the camera.
          //
          // *** LATER ***
          // Once an image from the buffer is saved and/or no longer needed, the
          // image must be released in orer to keep the buffer from filling up.
          //
          val hResultImage = use(new spinImage()) // NULL;

          val err1 = spinCameraGetNextImageEx(hCam, 1000, hResultImage)
          if printOnError(err1, "Unable to get next image. Non-fatal error.") then
            break

          //
          // Ensure image completion
          //
          // *** NOTES ***
          // Images can easily be checked for completion. This should be done
          // whenever a complete image is expected or required. Further, check
          // image status for a little more insight into why an image is
          // incomplete.
          //
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
          //
          // Print image information; height and width recorded in pixels
          //
          // *** NOTES ***
          // Images have quite a bit of available metadata including things such
          // as CRC, image status, and offset values, to name a few.
          //
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

          //
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
          val hConvertedImage = use(new spinImage()) // NULL;

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
          if !hasFailed then {
            // Create a unique filename
            val filename =
              if deviceSerialNumber.isEmpty then
                "Acquisition-C-" + imageCnt + ".jpg"
              else
                "Acquisition-C-" + deviceSerialNumber + "-" + imageCnt + ".jpg"
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
          //
          // *** NOTES ***
          // Images that are created must be destroyed in order to avoid memory
          // leaks.
          //
          printOnError(spinImageDestroy(hConvertedImage), "Unable to destroy image. Non-fatal error.")
          //
          // Release image from camera
          //
          // *** NOTES ***
          // Images retrieved directly from the camera (i.e. non-converted
          // images) need to be released in order to keep from filling the
          // buffer.
          //
          printOnError(spinImageRelease(hResultImage), "Unable to release image. Non-fatal error.")
        }
      }

      //
      // Destroy Image Processor context
      //
      // *** NOTES ***
      // Image processor context needs to be destroyed after all image processing
      // are complete to avoid memory leaks.
      //
      printOnError(spinImageProcessorDestroy(hImageProcessor), "Unable to destroy image processor. Non-fatal error.")

      //
      // End acquisition
      //
      // *** NOTES ***
      // Ending acquisition appropriately helps ensure that devices clean up
      // properly and do not need to be power-cycled to maintain integrity.
      //
      check(spinCameraEndAcquisition(hCam), "Unable to end acquisition.")
    }

}
