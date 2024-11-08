package spinnaker_c

import org.bytedeco.javacpp.*
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.spinColorProcessingAlgorithm.SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR
import org.bytedeco.spinnaker.global.Spinnaker_C.spinImageFileFormat.SPINNAKER_IMAGE_FILE_FORMAT_JPEG
import org.bytedeco.spinnaker.global.Spinnaker_C.spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR
import org.bytedeco.spinnaker.global.Spinnaker_C.spinPixelFormatEnums.PixelFormat_Mono8
import spinnaker_c.helpers.*

import java.io.File
import scala.util.Using
import scala.util.control.Breaks.{break, breakable}

/**
 *  Sequencer_C shows how to use the sequencer to grab images with
 *  various settings. It relies on information provided in the Enumeration_C,
 *  Acquisition_C, and NodeMapInfo_C examples.
 *
 *  It can also be helpful to familiarize yourself with the
 *  ImageFormatControl_C and Exposure_C examples as these provide a strong
 *  introduction to camera customization.
 *
 *  The sequencer is another very powerful tool that can be used to create and
 *  store multiple sets of customized image settings. A very useful
 *  application of the sequencer is creating high dynamic range images.
 *
 *  This example is probably the most complex and definitely the longest. As
 *  such, the configuration has been split between three functions. The first
 *  prepares the camera to set the sequences, the second sets the settings for
 *  a single sequence (it is run five times), and the third configures the
 *  camera to use the sequencer when it acquires images.
 */
object Sequencer_C {

  def main(args: Array[String]): Unit = {

    // Since this application saves images in the current folder,
    // we must ensure that we have permission to write to this folder.
    // If we do not have permission, fail right away.
    if !new File(".").canWrite then {
      System.out.println("Failed to create file in current folder.  Please check permissions.")
      return
    }

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
    }

    println("\nDone!\n")
  }

  /**
   * This function acts very similarly to the RunSingleCamera() functions of other
   * examples, except that the values for the sequences are also calculated here;
   * please see NodeMapInfo example for additional information on the steps in
   * this function.
   */
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

      // Configure sequencer to be ready to set sequences
      configureSequencerPartOne(hNodeMap)

      //
      // Set sequences
      //
      // *** NOTES ***
      // In the following section, the sequencer values are calculated. This
      // section does not appear in the configuration, as the values
      // calculated are somewhat arbitrary: width and height are both set to
      // 25% of their maximum values, incrementing by 10%; exposure time is
      // set to its minimum, also incrementing by 10% of its maximum; and gain
      // is set to its minimum, incrementing by 2% of its maximum.
      //
      val k_numSequences = 5

      // Retrieve maximum width; width recorded in pixels// Retrieve maximum width; width recorded in pixels
      val widthMax = nodeGetMaxLong(hNodeMap, "Width")

      // Retrieve maximum height; height recorded in pixels// Retrieve maximum height; height recorded in pixels
      val heightMax = nodeGetMaxLong(hNodeMap, "Height")

      // Retrieve maximum exposure time; exposure time recorded in microseconds// Retrieve maximum exposure time; exposure time recorded in microseconds
      val exposureTimeMax = math.min(nodeGetMaxDouble(hNodeMap, "ExposureTime"), 2_000_000d)

      val exposureTimeMin = nodeGetMinDouble(hNodeMap, "ExposureTime")

      // Retrieve maximum and minimum gain; gain recorded in decibels// Retrieve maximum and minimum gain; gain recorded in decibels
      val gainMax = nodeGetMaxDouble(hNodeMap, "Gain")
      val gainMin = nodeGetMinDouble(hNodeMap, "Gain")

      // Set individual sequences// Set individual sequences
      var widthToSet        = widthMax / 4
      var heightToSet       = heightMax / 4
      var exposureTimeToSet = exposureTimeMin
      var gainToSet         = gainMin

      for (sequenceNumber <- 0 until k_numSequences) {
        setSingleState(hNodeMap, sequenceNumber, widthToSet, heightToSet, exposureTimeToSet, gainToSet)
        widthToSet += widthMax / 10
        heightToSet += heightMax / 10
        exposureTimeToSet += exposureTimeMax / 10.0
        gainToSet += gainMax / 50.0
      }

      // Calculate appropriate acquisition grab timeout window based on exposure time// Calculate appropriate acquisition grab timeout window based on exposure time
      // Note: exposureTimeToSet is in microseconds and needs to be converted to milliseconds// Note: exposureTimeToSet is in microseconds and needs to be converted to milliseconds
      val timeout = ((exposureTimeToSet / 1000) + 1000).toInt

      // Configure sequencer to acquire images// Configure sequencer to acquire images
      configureSequencerPartTwo(hNodeMap)

      // Acquire images// Acquire images
      acquireImages(hCam, hNodeMap, hNodeMapTLDevice, timeout)

      // Reset sequencer// Reset sequencer
      resetSequencer(hNodeMap)

    finally
      // Deinitialize camera
      check(spinCameraDeInit(hCam), "Unable to deinitialize camera.")
  }.get

  /**
   * This function prepares the sequencer to accept custom configurations by
   * ensuring sequencer mode is off (this is a requirement to the enabling of
   * sequencer configuration mode), disabling automatic gain and exposure, and
   * turning sequencer configuration mode on.
   */
  private def configureSequencerPartOne(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>
    println("\n\n*** SEQUENCER CONFIGURATION ***\n\n")

    //
    // Ensure sequencer is off for configuration
    //
    // *** NOTES ***
    // In order to set a new sequencer configuration, sequencer mode must
    // be disabled and sequencer configuration mode must be enabled. In
    // order to manually disable sequencer mode, the sequencer configuration
    // must be valid; otherwise, we know that sequencer mode is off, but an
    // exception will be raised when we try to manually disable it.
    //
    // Therefore, in order to ensure that sequencer mode is off, we first
    // check whether the current sequencer configuration is valid. If it
    // isn't, then we know that sequencer mode is off and we can move on;
    // however, if it is, then we know it is safe to manually disable
    // sequencer mode.
    //
    // Also note that sequencer configuration mode needs to be off in order
    // to manually disable sequencer mode. It should be off by default, so
    // the example skips checking this.
    //

    // Validate sequencer configuration
    val hSequencerConfigurationValid = nodeMapGetNode(hNodeMap, "SequencerConfigurationValid")
    checkIsReadable(hSequencerConfigurationValid, "SequencerConfigurationValid")

    val hSequencerConfigurationValidCurrent = use(enumerationGetCurrentEntry(hSequencerConfigurationValid))
    checkIsReadable(hSequencerConfigurationValidCurrent, "hSequencerConfigurationValidCurrent")

    val hSequencerConfigurationValidYes = use(enumerationGetEntryByName(hSequencerConfigurationValid, "Yes"))
    checkIsReadable(hSequencerConfigurationValidYes, "hSequencerConfigurationValidYes")

    // If valid, disable sequencer mode; otherwise, do nothing
    if hSequencerConfigurationValidCurrent.getPointer == hSequencerConfigurationValidYes.getPointer then
      setEnumerationNodeValue(hNodeMap, "SequencerMode", "Off")

    System.out.print("Sequencer mode disabled...\n")
    //
    // Turn off automatic exposure mode
    //
    // *** NOTES ***
    // Automatic exposure prevents the manual configuration of exposure
    // times and needs to be turned off for this example.
    //
    // *** LATER ***
    // If exposure time is not being manually set for a specific reason, it
    // is best to let the camera take care of exposure time automatically.
    //
    setEnumerationNodeValue(hNodeMap, "ExposureAuto", "Off")
    printf("Automatic exposure disabled...\n")

    //
    // Turn off automatic gain
    //
    // *** NOTES ***
    // Automatic gain prevents the manual configuration of gain and needs to
    // be turned off for this example.
    //
    // *** LATER ***
    // If gain is not being manually set for a specific reason, it is best
    // to let the camera take care of gain automatically.
    //
    setEnumerationNodeValue(hNodeMap, "GainAuto", "Off")
    printf("Automatic gain disabled...\n")

    //
    // Turn configuration mode on
    //
    // *** NOTES ***
    // Once sequencer mode is off, enabling sequencer configuration mode
    // allows for the setting of individual sequences.
    //
    // *** LATER ***
    // Before sequencer mode is turned back on, sequencer configuration
    // mode must be turned off.
    //
    setEnumerationNodeValue(hNodeMap, "SequencerConfigurationMode", "On")
    printf("Sequencer configuration mode enabled...\n\n")
  }.get

  /**
   * This function sets a single state. It sets the sequence number, applies
   * custom settings, selects the trigger type and next state number, and saves
   * the state. The custom values that are applied are all calculated in the
   * function that calls this one, RunSingleCamera().
   */
  private def setSingleState(
    hNodeMap: spinNodeMapHandle,
    sequenceNumber: Int,
    widthToSet: Long,
    heightToSet: Long,
    exposureTimeToSet: Double,
    gainToSet: Double
  ): Unit = Using.Manager { use =>
    //
    // Select the sequence number
    //
    // *** NOTES ***
    // Select the index of the state to be set.
    //
    // *** LATER ***
    // The next state - i.e. the state to be linked to -
    // also needs to be set before saving the current state.
    //
    integerSetValue(hNodeMap, "SequencerSetSelector", sequenceNumber)
    printf("Customizing sequence %d...\n", sequenceNumber)

    //
    // Set desired settings for the current state
    //
    // *** NOTES ***
    // Width, height, exposure time, and gain are set in this example. If
    // the sequencer isn't working properly, it may be important to ensure
    // that each feature is enabled on the sequencer. Features are enabled
    // by default, so this is not explored in this example.
    //
    // Changing the height and width for the sequencer is not available
    // for all camera models.
    //
    // Set width; width recorded in pixels
    val hWidth = use(nodeMapGetNode(hNodeMap, "Width"))
    if (isReadable(hWidth, "hWidth") && isWritable(hWidth, "hWidth")) {
      val widthInc = use(new LongPointer(1)).put(0)
      check(spinIntegerGetInc(hWidth, widthInc), "Unable to set width")
      val widthToSet1 =
        if widthToSet % widthInc.get != 0 then
          (widthToSet / widthInc.get) * widthInc.get
        else
          widthToSet
      integerSetValue(hNodeMap, "Width", widthToSet1)
      printf("\tWidth set to %d...\n", widthToSet1.toInt)
    } else
      printf("\tUnable to get or set width; width for sequencer not readable/writable on all camera models...\n")

    // Set height; height recorded in pixels
    val hHeight = use(nodeMapGetNode(hNodeMap, "Height"))
    if (isReadable(hHeight, "hHeight") && isWritable(hHeight, "hHeight")) {
      val heightInc = use(new LongPointer(1)).put(0)
      check(spinIntegerGetInc(hHeight, heightInc), "Unable to set height")
      val heightToSet1 =
        if heightToSet % heightInc.get != 0 then
          (heightToSet / heightInc.get) * heightInc.get
        else
          heightToSet
      integerSetValue(hNodeMap, "Height", heightToSet1)
      printf("\tHeight set to %d...\n", heightToSet1.toInt)
    } else
      printf("\tUnable to get or set height; height for sequencer not readable/writable on all camera models...\n")

    // Set exposure time; exposure time recorded in microseconds
    floatSetValue(hNodeMap, "ExposureTime", exposureTimeToSet)
    printf("\tExposure time set to %f...\n", exposureTimeToSet)

    // Set gain; gain recorded in decibels
    floatSetValue(hNodeMap, "Gain", gainToSet)
    printf("\tGain set to %f...\n", gainToSet)

    //
    // Set the trigger type for the current sequence
    //
    // *** NOTES ***
    // It is a requirement of every state to have its trigger source set.
    // The trigger source refers to the moment when the sequencer changes
    // from one state to the next.
    //
    setEnumerationNodeValue(hNodeMap, "SequencerTriggerSource", "FrameStart")
    printf("\tTrigger source set to start of frame...\n")

    //
    // Set the next state in the sequence
    //
    // *** NOTES ***
    // When setting the next state in the sequence, ensure it does not
    // exceed the maximum and that the states loop appropriately.
    //
    val finalSequenceIndex = 4
    val nextSequence       = if (sequenceNumber != finalSequenceIndex) sequenceNumber + 1 else 0
    integerSetValue(hNodeMap, "SequencerSetNext", nextSequence)
    printf("\tNext sequence set to %d...\n", nextSequence)

    //
    // Save current state
    //
    // *** NOTES ***
    // Once all appropriate settings have been configured, make sure to
    // save the state to the sequence. Notice that these settings will be
    // lost when the camera is power-cycled.
    //
    val hSequencerSetSave = use(nodeMapGetNode(hNodeMap, "SequencerSetSave"))
    checkIsWritable(hSequencerSetSave, "hSequencerSetSave")
    check(spinCommandExecute(hSequencerSetSave), "Unable to save sequence.")
    printf("\tSequence %d saved...\n\n", sequenceNumber)
  }.get

  /**
   * Now that the states have all been set, this function readies the camera
   * to use the sequencer during image acquisition.
   */
  private def configureSequencerPartTwo(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>
    //
    // Turn configuration mode off
    //
    // *** NOTES ***
    // Once all desired states have been set, turn sequencer
    // configuration mode off in order to turn sequencer mode on.
    //
    setEnumerationNodeValue(hNodeMap, "SequencerConfigurationMode", "Off")
    printf("Sequencer configuration mode disabled...\n")

    //
    // Turn sequencer mode on
    //
    // *** NOTES ***
    // Once sequencer mode is turned on, the camera will begin using the
    // saved states in the order that they were set.
    //
    // *** LATER ***
    // Once all images have been captured, disable the sequencer in order
    // to restore the camera to its initial state.
    //
    setEnumerationNodeValue(hNodeMap, "SequencerMode", "On")
    printf("Sequencer mode enabled...\n")

    //
    // Validate sequencer settings
    //
    // *** NOTES ***
    // Once all states have been set, it is a good idea to
    // validate them. Although this node cannot ensure that the states
    // have been set up correctly, it does ensure that the states have
    // been set up in such a way that the camera can function.
    //
    val hSequencerConfigurationValid = use(nodeMapGetNode(hNodeMap, "SequencerConfigurationValid"))
    checkIsReadable(hSequencerConfigurationValid, "hSequencerConfigurationValid")

    val hSequencerConfigurationValidCurrent = use(enumerationGetCurrentEntry(hSequencerConfigurationValid))
    val hSequencerConfigurationValidYes     = use(enumerationGetEntryByName(hSequencerConfigurationValid, "Yes"))

    val sequencerConfigurationValidCurrent = enumerationEntryGetIntValue(hSequencerConfigurationValidCurrent)
    val sequencerConfigurationValidYes     = enumerationEntryGetIntValue(hSequencerConfigurationValidYes)

    if (sequencerConfigurationValidCurrent != sequencerConfigurationValidYes) {
      printf("Sequencer configuration not valid.")
      throw new SpinnakerSDKException("Sequencer configuration not valid.", spinError.SPINNAKER_ERR_ACCESS_DENIED)
    }
    printf("Sequencer configuration valid...\n\n")
  }.get

  //
  // This function acquires and saves 10 images from a device; please see
  // Acquisition_C example for more in-depth comments on the acquisition of
  // images.
  private def acquireImages(
    hCam: spinCamera,
    hNodeMap: spinNodeMapHandle,
    hNodeMapTLDevice: spinNodeMapHandle,
    timeout: Int
  ): Unit = Using.Manager { use =>
    printf("\n*** IMAGE ACQUISITION ***\n\n")

    // Set acquisition mode to continuous
    setEnumerationNodeValue(hNodeMap, "AcquisitionMode", "Continuous")
    printf("Acquisition mode set to continuous...\n")

    // Begin acquiring images
    check(spinCameraBeginAcquisition(hCam), "Unable to begin image acquisition.")
    printf("Acquiring images...\n")

    // Retrieve device serial number for filename
    val deviceSerialNumber =
      nodeGetStringValueOpt(hNodeMapTLDevice, "DeviceSerialNumber")
        .map(_.trim)
        .getOrElse("")
    println(s"Device serial number retrieved as $deviceSerialNumber...")

    //
    // Create Image Processor context for post processing images
    //
    val hImageProcessor = use(new spinImageProcessor)
    printOnError(spinImageProcessorCreate(hImageProcessor), "Unable to create image processor. Non-fatal error.")
    //
    // Set default image processor color processing method
    //
    // *** NOTES ***
    // By default, if no specific color processing algorithm is set, the image
    // processor will default to NEAREST_NEIGHBOR method.
    //
    printOnError(
      spinImageProcessorSetColorProcessing(hImageProcessor, SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR),
      "Unable to set image processor color processing method. Non-fatal error."
    )
    // Retrieve, convert, and save images
    val k_numImages = 10
    for (imageCnt <- 0 until k_numImages) breakable {
      // Retrieve next received image
      val hResultImage = use(new spinImage())
      if printOnError(
          spinCameraGetNextImageEx(hCam, timeout, hResultImage),
          "Unable to get next image. Non-fatal error."
        )
      then break

      // Ensure image completion
      val isIncomplete = use(new BytePointer(1))
      var hasFailed    = false
      if (
        printOnError(
          spinImageIsIncomplete(hResultImage, isIncomplete),
          "Unable to determine image completion. Non-fatal error."
        )
      ) hasFailed = true
      // Check image for completion
      if (isIncomplete.getBool) {
        val imageStatus = use(new IntPointer(1L)).put(SPINNAKER_IMAGE_STATUS_NO_ERROR.value)
        if (
          !printOnError(
            spinImageGetStatus(hResultImage, imageStatus),
            "Unable to retrieve image status. Non-fatal error. " + findImageStatusNameByValue(imageStatus.get)
          )
        )
          println("Image incomplete with image status " + findImageStatusNameByValue(imageStatus.get) + "...")
        hasFailed = true
      }

      // Release incomplete or failed image
      if (hasFailed) {
        printOnError(spinImageRelease(hResultImage), "Unable to release image. Non-fatal error")
        break
      }

      // Retrieve image width
      val width = use(new SizeTPointer(1))
      if (printOnError(spinImageGetWidth(hResultImage, width), "spinImageGetWidth()"))
        println("width  = unknown")
      else
        println("width  = " + width.get)

      // Retrieve image height
      val height = use(new SizeTPointer(1))
      if (printOnError(spinImageGetHeight(hResultImage, height), "spinImageGetHeight()"))
        println("height = unknown")
      else
        println("height = " + height.get)

      // Convert image to mono 8
      val hConvertedImage = use(new spinImage())
      if printOnError(spinImageCreateEmpty(hConvertedImage), "Unable to create image. Non-fatal error.") then
        hasFailed = true
      if (
        printOnError(
          spinImageProcessorConvert(hImageProcessor, hResultImage, hConvertedImage, PixelFormat_Mono8),
          "\"Unable to convert image. Non-fatal error."
        )
      ) hasFailed = true

      // Create unique file name
      val filename =
        if deviceSerialNumber.isBlank then
          "Sequencer-C-" + imageCnt + ".jpg"
        else
          "Sequencer-C-" + deviceSerialNumber + "-" + imageCnt + ".jpg"

      // Save image
      if (
        !printOnError(
          spinImageSave(hConvertedImage, use(new BytePointer(filename)), SPINNAKER_IMAGE_FILE_FORMAT_JPEG),
          "Unable to save image. Non-fatal error."
        )
      )
        println("Image saved at " + filename + "\n")

      // Destroy converted image
      printOnError(spinImageDestroy(hConvertedImage), "Unable to destroy image. Non-fatal error")

      // Release image
      printOnError(spinImageRelease(hResultImage), "Unable to release image. Non-fatal error ")
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
    printOnError(spinCameraEndAcquisition(hCam), "Unable to end acquisition. Non-fatal error.")
  }.get

  /**
   * This function restores the camera to its default state by turning sequencer
   * mode off and re-enabling automatic exposure and gain.
   */
  private def resetSequencer(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>
    //
    // Turn sequencer mode back off
    //
    // *** NOTES ***
    // The sequencer is turned off in order to return the camera to its default
    // state.
    //
    setEnumerationNodeValue(hNodeMap, "SequencerMode", "Off")
    printf("Sequencer mode disabled...\n")

    //
    // Turn automatic exposure back on
    //
    // *** NOTES ***
    // Automatic exposure is turned on in order to return the camera to its
    // default state.
    //
    setEnumerationNodeValue(hNodeMap, "ExposureAuto", "Continuous")
    printf("Automatic exposure enabled...\n")

    //
    // Turn automatic gain back on
    //
    // *** NOTES ***
    // Automatic gain is turned on in order to return the camera to its
    // default state.
    //
    setEnumerationNodeValue(hNodeMap, "GainAuto", "Continuous")
    printf("Automatic gain enabled...\n\n")
  }.get
}
