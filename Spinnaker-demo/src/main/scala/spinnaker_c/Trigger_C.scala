package spinnaker_c

import org.bytedeco.javacpp.{BytePointer, IntPointer, LongPointer, SizeTPointer}
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.spinColorProcessingAlgorithm.SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR
import org.bytedeco.spinnaker.global.Spinnaker_C.spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR
import spinnaker_c.Trigger_C.TriggerType.{Hardware, Software}
import spinnaker_c.helpers.*

import java.io.File
import scala.util.Using
import scala.util.control.Breaks.{break, breakable}

/**
 * Trigger_C.c shows how to trigger the camera. It relies on
 * information provided in the Enumeration_C, Acquisition_C, and NodeMapInfo_C
 * examples.
 *
 * It can also be helpful to familiarize yourself with the
 * ImageFormatControl_C and Exposure_C examples as these provide a strong
 * introduction to camera customization.
 *
 * This example shows the process of configuring, using, and cleaning up a
 * camera for use with both a software and a hardware trigger.
 */
object Trigger_C {

  private val chosenTrigger = TriggerType.Software
  private val MAX_BUFF_LEN  = 256

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
            val hCamera = use(new spinCamera)
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
    }
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

      // Configure trigger
      configureTrigger(hNodeMap)

      // Acquire images
      acquireImages(hCam, hNodeMap, hNodeMapTLDevice)

      // Reset trigger
      resetTrigger(hNodeMap)
    finally
      // Deinitialize camera
      check(spinCameraDeInit(hCam), "Unable to deinitialize camera.")
  }

  /**
   * Configures the camera to use a trigger.
   * First, trigger mode is set to off in order to select the trigger source.
   * Trigger mode is then enabled,
   * which has the camera capture only a single image upon the execution of the chosen trigger.
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  private def configureTrigger(hNodeMap: spinNodeMapHandle): Unit = {

    println("\n\n*** TRIGGER CONFIGURATION ***\n");

    println("Note that if the application / user software triggers faster than frame time, " +
      "the trigger may be dropped skipped by the camera.")
    println(
      "If several frames are needed per trigger, a more reliable alternative for such case, " +
        "is to use the multi-frame mode.\n"
    )

    println(s"$chosenTrigger trigger chosen...\n")

    Using.Manager { use =>

      //
      // Ensure trigger mode off
      //
      // *** NOTES ***
      // The trigger must be disabled in order to configure whether the source
      // is software or hardware.
      //
      val hTriggerMode = use(new spinNodeHandle())
      //    spinNodeHandle hTriggerModeOff = NULL;
      //    int64_t triggerModeOff = 0;
      //
      check(
        spinNodeMapGetNode(hNodeMap, use(new BytePointer("TriggerMode")), hTriggerMode),
        "Unable to disable trigger mode"
      )

      // check if readable
      checkIsReadable(hTriggerMode, "TriggerMode")

      // fetch entry
      val hTriggerModeOff = use(new spinNodeHandle())
      check(
        spinEnumerationGetEntryByName(hTriggerMode, use(new BytePointer("Off")), hTriggerModeOff),
        "Unable to disable trigger mode"
      )

      // check if readable
      checkIsReadable(hTriggerModeOff, "TriggerModeOff")

      val triggerModeOff = use(new LongPointer(1L))

      check(spinEnumerationEntryGetIntValue(hTriggerModeOff, triggerModeOff), "Unable to disable trigger mode.")

      // turn trigger mode off
      checkIsWritable(hTriggerMode, "TriggerMode")

      check(spinEnumerationSetIntValue(hTriggerMode, triggerModeOff.get), "Unable to disable trigger mode")

      println("Trigger mode disabled...");

      //
      // Set TriggerSelector to FrameStart
      //
      // *** NOTES ***
      // For this example, the trigger selector should be set to frame start.
      // This is the default for most cameras.
      //

      // Retrieve enumeration node from nodemap
      val hTriggerSelector = use(new spinNodeHandle())
      check(
        spinNodeMapGetNode(hNodeMap, use(new BytePointer("TriggerSelector")), hTriggerSelector),
        "Unable to choose trigger selector"
      )

      // check if readable
      checkIsReadable(hTriggerSelector, "TriggerSelector")

      // Retrieve entry node from enumeration node to set selector
      val hTriggerSelectorChoice = use(new spinNodeHandle())

      check(
        spinEnumerationGetEntryByName(hTriggerSelector, use(new BytePointer("FrameStart")), hTriggerSelectorChoice),
        "Unable to choose trigger selector"
      )

      checkIsReadable(hTriggerSelectorChoice, "TriggerSelectorChoice")

      // Retrieve integer value from entry node
      val triggerSelectorChoice = use(new LongPointer(1));
      check(
        spinEnumerationEntryGetIntValue(hTriggerSelectorChoice, triggerSelectorChoice),
        "Unable to choose trigger selector"
      )

      // set trigger source choice
      checkIsWritable(hTriggerSelector, "TriggerSelector")

      check(
        spinEnumerationSetIntValue(hTriggerSelector, triggerSelectorChoice.get),
        "Unable to choose trigger selector"
      )

      println("Trigger selector set to frame start...")

      //
      // Choose trigger source
      //
      // *** NOTES ***
      // The trigger source must be set to hardware or software while trigger
      // mode is off.
      //

      // Retrieve enumeration node from nodemap
      val hTriggerSource = new spinNodeHandle()
      check(
        spinNodeMapGetNode(hNodeMap, use(new BytePointer("TriggerSource")), hTriggerSource),
        "Unable to choose trigger source."
      )

      // check if readable
      checkIsReadable(hTriggerSource, "TriggerSource")

      val hTriggerSourceChoice = use(new spinNodeHandle())
      chosenTrigger match
        case Software =>
          // Retrieve entry node from enumeration node to set software
          check(
            spinEnumerationGetEntryByName(hTriggerSource, use(new BytePointer("Software")), hTriggerSourceChoice),
            "Unable to choose trigger source"
          )
        case Hardware =>
          // Retrieve entry node from enumeration node to set hardware ('Line0')
          check(
            spinEnumerationGetEntryByName(hTriggerSource, use(new BytePointer("Line0")), hTriggerSourceChoice),
            "Unable to choose trigger source"
          )

      checkIsReadable(hTriggerSourceChoice, "TriggerSourceChoice")

      // Retrieve integer value from entry node
      val triggerSourceChoice = use(new LongPointer(1))
      check(
        spinEnumerationEntryGetIntValue(hTriggerSourceChoice, triggerSourceChoice),
        "Unable to choose trigger source."
      )

      // set trigger source choice
      checkIsWritable(hTriggerSource, "TriggerSource")

      check(spinEnumerationSetIntValue(hTriggerSource, triggerSourceChoice.get), "Unable to choose trigger source.")

      println(s"Trigger source set to $chosenTrigger...\n");

      //
      // Turn trigger mode on
      //
      // *** LATER ***
      // Once the appropriate trigger source has been set, turn trigger mode
      // in order to retrieve images using the trigger.
      //

      //
      // NOTE: Blackfly and Flea3 GEV cameras need 1 second delay after trigger mode is
      // turned on
      val hTriggerModeOn = new spinNodeHandle()
      check(
        spinEnumerationGetEntryByName(hTriggerMode, use(new BytePointer("On")), hTriggerModeOn),
        "Unable to enable trigger mode."
      )

      checkIsReadable(hTriggerModeOn, "TriggerModeOn")

      val triggerModeOn = use(new LongPointer(1))
      check(spinEnumerationEntryGetIntValue(hTriggerModeOn, triggerModeOn), "Unable to enable trigger mode.")

      checkIsWritable(hTriggerMode, "TriggerMode")

      check(spinEnumerationSetIntValue(hTriggerMode, triggerModeOn.get), "Unable to enable trigger mode.")

      println("Trigger mode enabled...\n");
    }
  }

  /**
   * Returns the camera to a normal state by turning off trigger mode.
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  private def resetTrigger(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>
    //
    // Turn trigger mode back off
    //
    // *** NOTES ***
    // Once all images have been captured, it is important to turn trigger
    // mode back off to restore the camera to a clean state.
    //

    //    int64_t  = 0;
    //
    val hTriggerMode = use(new spinNodeHandle())
    check(
      spinNodeMapGetNode(hNodeMap, use(new BytePointer("TriggerMode")), hTriggerMode),
      "Unable to disable trigger mode."
    )

    checkIsReadable(hTriggerMode, "TriggerMode")

    val hTriggerModeOff = use(new spinNodeHandle())
    check(
      spinEnumerationGetEntryByName(hTriggerMode, use(new BytePointer("Off")), hTriggerModeOff),
      "Unable to disable trigger mode."
    )

    checkIsReadable(hTriggerModeOff, "TriggerModeOff")

    val triggerModeOff = use(new LongPointer(1))
    check(spinEnumerationEntryGetIntValue(hTriggerModeOff, triggerModeOff), "Unable to disable trigger mode.")

    checkIsWritable(hTriggerMode, "TriggerMode")

    check(spinEnumerationSetIntValue(hTriggerMode, triggerModeOff.get), "Unable to disable trigger mode.")
    println("Trigger mode disabled...\n");
  }

  /**
   * Acquires and saves 10 images from a device;
   * please see Acquisition_C example for more in-depth comments on the acquisition of images.
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  def acquireImages(hCam: spinCamera, hNodeMap: spinNodeMapHandle, hNodeMapTLDevice: spinNodeMapHandle): Unit =
    Using.Manager { use =>
      println("\n*** IMAGE ACQUISITION ***\n")

      // Set acquisition mode to continuous
      val hAcquisitionMode = use(new spinNodeHandle()) // Empty handle, equivalent to NULL in C

      check(
        spinNodeMapGetNode(hNodeMap, use(new BytePointer("AcquisitionMode")), hAcquisitionMode),
        "Unable to set acquisition mode to continuous (node retrieval)."
      )

      // Retrieve entry node from enumeration node
      val hAcquisitionModeContinuous = use(new spinNodeHandle()) // Empty handle, equivalent to NULL in C

      checkIsReadable(hAcquisitionMode, "AcquisitionMode")

      check(
        spinEnumerationGetEntryByName(
          hAcquisitionMode,
          use(new BytePointer("Continuous")),
          hAcquisitionModeContinuous
        ),
        "Unable to set acquisition mode to continuous (entry 'continuous' retrieval)."
      )

      // Retrieve integer from entry node
      val acquisitionModeContinuous = use(new LongPointer(1))
      checkIsReadable(hAcquisitionModeContinuous, "AcquisitionModeContinuous")

      check(
        spinEnumerationEntryGetIntValue(hAcquisitionModeContinuous, acquisitionModeContinuous),
        "Unable to set acquisition mode to continuous (entry int value retrieval)."
      )

      // Set integer as new value of enumeration node
      checkIsWritable(hAcquisitionMode, "AcquisitionMode")

      check(
        spinEnumerationSetIntValue(hAcquisitionMode, acquisitionModeContinuous.get),
        "Unable to set acquisition mode to continuous (entry int value setting)."
      )

      println("Acquisition mode set to continuous...")

      // Begin acquiring images
      check(spinCameraBeginAcquisition(hCam), "Unable to begin image acquisition.")

      System.out.println("Acquiring images...")

      // Retrieve device serial number for filename
      val hDeviceSerialNumber = use(new spinNodeHandle()) // NULL;

      val deviceSerialNumber    = use(new BytePointer(MAX_BUFF_LEN))
      val lenDeviceSerialNumber = use(new SizeTPointer(1))
      lenDeviceSerialNumber.put(MAX_BUFF_LEN)
      val err1 = spinNodeMapGetNode(hNodeMapTLDevice, new BytePointer("DeviceSerialNumber"), hDeviceSerialNumber)
      if printOnError(err1, "") then {
        deviceSerialNumber.putString("")
        lenDeviceSerialNumber.put(0)
      } else {
        if isReadable(hDeviceSerialNumber, "DeviceSerialNumber") then {
          val err2 = spinStringGetValue(hDeviceSerialNumber, deviceSerialNumber, lenDeviceSerialNumber)
          if printOnError(err2, "") then {
            deviceSerialNumber.putString("")
            lenDeviceSerialNumber.put(0)
          }
        } else {
          deviceSerialNumber.putString("")
          lenDeviceSerialNumber.put(0)
          printRetrieveNodeFailure("node", "DeviceSerialNumber")
        }
        println("Device serial number retrieved as " + deviceSerialNumber.getString.trim + "...")
      }
      println()

      // Retrieve, convert, and save images

      //
      // Create Image Processor context for post processing images
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

          // Retrieve next image by trigger
          grabNextImageByTrigger(hNodeMap)

          // Retrieve next received image
          val hResultImage = new spinImage // NULL;

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

          // Print image information; height and width recorded in pixels
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
          val hConvertedImage = new spinImage // NULL;

          val err7 = spinImageCreateEmpty(hConvertedImage)
          if printOnError(err7, "Unable to create image. Non-fatal error.") then
            hasFailed = true
          val err8 = spinImageProcessorConvert(
            hImageProcessor,
            hResultImage,
            hConvertedImage,
            spinPixelFormatEnums.PixelFormat_Mono8
          )
          printOnError(err8, "\"Unable to convert image. Non-fatal error.")
          if !hasFailed then {
            // Create a unique filename
            val filename =
              if lenDeviceSerialNumber.get == 0 then
                "Acquisition-C-" + imageCnt + ".jpg"
              else
                "Acquisition-C-" + deviceSerialNumber.getString.trim + "-" + imageCnt + ".jpg"

            // Save image
            val err9 = spinImageSave(
              hConvertedImage,
              new BytePointer(filename),
              spinImageFileFormat.SPINNAKER_IMAGE_FILE_FORMAT_JPEG
            )
            if !printOnError(err9, "Unable to save image. Non-fatal error.") then
              println("Image saved at " + filename + "\n")
          }

          // Destroy converted image
          printOnError(spinImageDestroy(hConvertedImage), "Unable to destroy image. Non-fatal error.")
          // Release image from camera
          printOnError(spinImageRelease(hResultImage), "Unable to release image. Non-fatal error.")

        }

      //
      // Destroy Image Processor context
      //
      // *** NOTES ***
      // Image processor context needs to be destroyed after all image processing
      // are complete to avoid memory leaks.
      //
      printOnError(spinImageProcessorDestroy(hImageProcessor), "Unable to destroy image processor. Non-fatal error.")

      // End acquisition
      check(spinCameraEndAcquisition(hCam), "Unable to end acquisition.")
    }

  /**
   * Retrieves a single image using the trigger.
   * In this example, only a single image is captured and made available for acquisition - as such,
   * attempting to acquire two images for a single trigger execution would cause the example to hang.
   * This is different from other examples,
   * whereby a constant stream of images are being captured and made available for image acquisition.
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  private def grabNextImageByTrigger(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>
    //
    // Use trigger to capture image
    //
    // *** NOTES ***
    // The software trigger only feigns being executed by the Enter key;
    // what might not be immediately apparent is that there is no
    // continuous stream of images being captured; in other examples that
    // acquire images, the camera captures a continuous stream of images.
    // When an image is then retrieved, it is plucked from the stream;
    // there are many more images captured than retrieved. However, while
    // trigger mode is activated, there is only a single image captured at
    // the time that the trigger is activated.
    //

    chosenTrigger match
      case Software =>
        // Get user input
        println("Press the Enter key to initiate software trigger...");
        System.in.read()

        // Execute software trigger
        val hTriggerSoftware = new spinNodeHandle()

        check(
          spinNodeMapGetNode(hNodeMap, use(new BytePointer("TriggerSoftware")), hTriggerSoftware),
          "Unable to execute software trigger"
        )

        check(spinCommandExecute(hTriggerSoftware), "Unable to execute software trigger.")

      // NOTE: Blackfly and Flea3 GEV cameras need 2 second delay after software trigger
      case Hardware =>
        // Execute hardware trigger
        println("Use the hardware to trigger image acquisition.");
  }

  enum TriggerType:
    case Software, Hardware
}
