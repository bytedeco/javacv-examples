package spinnaker_c

import org.bytedeco.javacpp.*
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.spinColorProcessingAlgorithm.SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR
import org.bytedeco.spinnaker.global.Spinnaker_C.spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR
import spinnaker_c.helpers.*

import scala.util.boundary
import scala.util.boundary.break

/**
 * `SaveToAvi_C` shows how to create a video from a vector of images.
 * It relies on information provided in the `Enumeration_C` , `Acquisition_C`, and
 * `NodeMapInfo_C` examples.
 *
 * This example introduces the `SpinVideo` class, which is used to quickly and
 * easily create various types of video files. It demonstrates the creation of
 * three types: `uncompressed`, `MJPG`, and `H264`.
 *
 * Code based on C version, EnumerationEvents_C, from Spinnaker SDK by FLIR.
 */
object SaveToAvi_C {

  // These macros helps with C-strings and number of frames in a video.
  private val NUM_IMAGES = 60

  // Use the following "enum" and global constant to select the type of video
  // file to be created and saved.
  enum FileType:
    case Uncompressed
    case MJPG
    case H264
  val chosenFileType: FileType = FileType.Uncompressed

  /** This function prepares, saves, and cleans up an video from a vector of images. */
  def saveArrayToVideo(
    hNodeMap: spinNodeMapHandle,
    hNodeMapTLDevice: spinNodeMapHandle,
    hImages: Array[spinImage]
  ): spinError = {

    var err = spinError.SPINNAKER_ERR_SUCCESS

    printf("\n\n*** CREATING VIDEO ***\n\n")

    //
    // Get the current frame rate; acquisition frame rate recorded in hertz
    //
    // *** NOTES ***
    // The video frame rate can be set to anything; however, in order to
    // have videos play in real-time, the acquisition frame rate can be
    // retrieved from the camera.
    //
    val hAcquisitionFrameRate = new spinNodeHandle()
    val acquisitionFrameRate  = new DoublePointer(1)

    err = spinNodeMapGetNode(hNodeMap, toBytePointer("AcquisitionFrameRate"), hAcquisitionFrameRate)
    if (printOnError(err, "Unable to retrieve frame rate (node retrieval)."))
      return err

    if (isReadable(hAcquisitionFrameRate, "AcquisitionFrameRate")) {
      err = spinFloatGetValue(hAcquisitionFrameRate, acquisitionFrameRate)
      if (printOnError(err, "Unable to retrieve frame rate (value retrieval)."))
        return err
    } else {
      err = spinError.SPINNAKER_ERR_ACCESS_DENIED
      printError(err, "Unable to read frame rate.\n")
      return err
    }

    val frameRateToSet = acquisitionFrameRate

    printf("Frame rate to be set to %f\n", frameRateToSet.get)

    // Retrieve device serial number for filename
    val deviceSerialNumberOpt = nodeGetStringValueOpt(hNodeMapTLDevice, "DeviceSerialNumber")

    //
    // Create a unique filename
    //
    // *** NOTES ***
    // This example creates filenames according to the type of video
    // being created. Notice that '.avi' does not need to be appended to the
    // name of the file. This is because the SpinVideo object takes care
    // of the file extension automatically.
    //
    val snStr    = deviceSerialNumberOpt.map(_ + "-").getOrElse("")
    val filename = s"SaveToAvi-C-$snStr$chosenFileType"

    //
    // Select option and open video file type
    //
    // *** NOTES ***
    // Depending on the file type, a number of settings need to be set in
    // an object called an option. An uncompressed option only needs to
    // have the video frame rate set whereas videos with MJPG or H264
    // compressions need to have more values set.
    //
    // Once the desired option object is configured, open the video file
    // with the option in order to create the video file.
    //
    // *** LATER ***
    // Once all images have been added, it is important to close the file -
    // this is similar to many other standard file streams.
    //
    val video = new spinVideo()

    chosenFileType match {
      case FileType.Uncompressed =>
        val option = new spinAVIOption()

        option.frameRate(frameRateToSet.get().toFloat)

        val hWidth = new spinNodeHandle()
        err = spinNodeMapGetNode(hNodeMap, toBytePointer("Width"), hWidth)
        if (printOnError(err, "Unable to retrieve width (node retrieval). Aborting with error.")) return err

        val width = new LongPointer(1)
        if (isReadable(hWidth, "Width")) {

          err = spinIntegerGetValue(hWidth, width)
          if (printOnError(err, "Unable to retrieve width (value retrieval). Aborting with error.")) return err
        } else {
          err = spinError.SPINNAKER_ERR_ACCESS_DENIED
          printError(err, "Unable to read width. Aborting with error.")
          return err
        }

        option.width(width.get().toInt)

        val hHeight = new spinNodeHandle()
        err = spinNodeMapGetNode(hNodeMap, toBytePointer("Height"), hHeight)
        if (printOnError(err, "Unable to retrieve height (node retrieval). Aborting with error.")) return err

        val height = new LongPointer(1)
        if (isReadable(hHeight, "Height")) {

          err = spinIntegerGetValue(hHeight, height)
          if (printOnError(err, "Unable to retrieve height (value retrieval). Aborting with error.")) return err
        } else {
          err = spinError.SPINNAKER_ERR_ACCESS_DENIED
          printError(err, "Unable to read height. Aborting with error.")
          return err
        }

        option.height(height.get().toInt)

        err = spinVideoOpenUncompressed(video, toBytePointer(filename), option)
        if (printOnError(err, "Unable to open uncompressed video file."))
          return err

      case FileType.MJPG =>
        val option = new spinMJPGOption()

        option.frameRate(frameRateToSet.get().toFloat)
        option.quality(75)

        val hWidth = new spinNodeHandle()
        err = spinNodeMapGetNode(hNodeMap, toBytePointer("Width"), hWidth)
        if (printOnError(err, "Unable to retrieve width (node retrieval). Aborting with error.")) return err

        val width = new LongPointer(1)
        if (isReadable(hWidth, "Width")) {

          err = spinIntegerGetValue(hWidth, width)
          if (printOnError(err, "Unable to retrieve width (value retrieval). Aborting with error.")) return err
        } else {
          err = spinError.SPINNAKER_ERR_ACCESS_DENIED
          printError(err, "Unable to read width. Aborting with error.")
          return err
        }

        option.width(width.get().toInt)

        val hHeight = new spinNodeHandle()
        err = spinNodeMapGetNode(hNodeMap, toBytePointer("Height"), hHeight)
        if (printOnError(err, "Unable to retrieve height (node retrieval). Aborting with error.")) return err

        val height = new LongPointer(1)
        if (isReadable(hHeight, "Height")) {

          err = spinIntegerGetValue(hHeight, height)
          if (printOnError(err, "Unable to retrieve height (value retrieval). Aborting with error.")) return err
        } else {
          err = spinError.SPINNAKER_ERR_ACCESS_DENIED
          printError(err, "Unable to read height. Aborting with error.")
          return err
        }

        option.height(height.get().toInt)

        err = spinVideoOpenMJPG(video, toBytePointer(filename), option)
        if (printOnError(err, "Unable to open MJPG video file."))
          return err

      case FileType.H264 =>
        val option = new spinH264Option()

        option.frameRate(frameRateToSet.get.toFloat)
        option.bitrate(1_000_000)

        val hWidth = new spinNodeHandle()
        val width  = new LongPointer(1)

        err = spinNodeMapGetNode(hNodeMap, toBytePointer("Width"), hWidth)
        if (printOnError(err, "Unable to retrieve width (node retrieval)."))
          return err

        if (isAvailableAndReadable(hWidth, "Width")) {
          err = spinIntegerGetValue(hWidth, width)
          if (printOnError(err, "Unable to retrieve width (value retrieval)."))
            return err
        } else {
          err = spinError.SPINNAKER_ERR_ACCESS_DENIED
          printError(err, "Unable to read width")
          return err
        }

        option.width(width.get.toInt)

        val hHeight = new spinNodeHandle()
        val height  = new LongPointer(1)

        err = spinNodeMapGetNode(hNodeMap, toBytePointer("Height"), hHeight)
        if (printOnError(err, "Unable to retrieve height (node retrieval)."))
          return err

        if (isAvailableAndReadable(hHeight, "Height")) {
          err = spinIntegerGetValue(hHeight, height)
          if (printOnError(err, "Unable to retrieve height (value retrieval)."))
            return err
        } else {
          err = spinError.SPINNAKER_ERR_ACCESS_DENIED
          printError(err, "Unable to read height")
          return err
        }

        option.height(height.get.toInt)

        err = spinVideoOpenH264(video, toBytePointer(filename), option)
        if (printOnError(err, "Unable to open H264 video file."))
          return err
    }

    // Set maximum video file size to 2GB. A new video file is generated when 2GB
    // limit is reached. Setting maximum file size to 0 indicates no limit.
    val k_videoFileSize = 2048

    err = spinVideoSetMaximumFileSize(video, k_videoFileSize)
    if (printOnError(err, "Unable to set maximum file size."))
      return err

    //
    // Construct and save video
    //
    // *** NOTES ***
    // Although the video file has been opened, images must be individually
    // appended in order to construct the video.
    //
    printf("Appending %d images to video file: %s.avi...\n\n", hImages.length, filename)

    boundary:
      for (imageCnt <- hImages.indices) do
        err = spinVideoAppend(video, hImages(imageCnt))
        if printOnError(err, "Unable to append image.") then break(Option(err))
        printf("\tAppended image %d...\n", imageCnt)
      None
    match
      case Some(err) => return err
      case None      => // OK
    printf("\nVideo saved at %s.avi\n\n", filename)

    //
    // Close video file
    //
    // *** NOTES ***
    // Once all images have been appended, it is important to close the
    // video file. Notice that once an video file has been closed, no more
    // images can be added.
    //
    err = spinVideoClose(video)
    if (printOnError(err, "Unable to close video file."))
      return err

    // Destroy images
    for (imageCnt <- hImages.indices) {
      err = spinImageDestroy(hImages(imageCnt))
      printOnError(err, "Unable to destroy image %d. Non-fatal error.")
    }

    err
  }

  /**
   * This function acquires and saves 10 images from a device;
   * please see `Acquisition_C` example for more in-depth comments on the acquisition of images.
   */
  def acquireImages(
    hCam: spinCamera,
    hNodeMap: spinNodeMapHandle,
    hNodeMapTLDevice: spinNodeMapHandle,
    hImages: Array[spinImage]
  ): spinError = {
    printf("\n*** IMAGE ACQUISITION ***\n\n")

    var err = spinError.SPINNAKER_ERR_SUCCESS

    // Set acquisition mode to continuous
    val hAcquisitionMode = new spinNodeHandle()
    err = spinNodeMapGetNode(hNodeMap, new BytePointer("AcquisitionMode"), hAcquisitionMode)
    if (printOnError(err, "Unable to set acquisition mode to continuous (node retrieval)."))
      return err

    // Retrieve entry node from enumeration node
    val hAcquisitionModeContinuous = new spinNodeHandle()
    if (isReadable(hAcquisitionMode, "AcquisitionMode")) {
      err = spinEnumerationGetEntryByName(hAcquisitionMode, new BytePointer("Continuous"), hAcquisitionModeContinuous)
      if (printOnError(err, "Unable to set acquisition mode to continuous (entry 'continuous' retrieval)."))
        return err
    } else {
      printRetrieveNodeFailure("entry", "AcquisitionMode")
      return spinError.SPINNAKER_ERR_ACCESS_DENIED
    }

    // Retrieve integer from entry node
    val acquisitionModeContinuous = new LongPointer(1)
    if (isReadable(hAcquisitionModeContinuous, "AcquisitionModeContinuous")) {
      err = spinEnumerationEntryGetIntValue(hAcquisitionModeContinuous, acquisitionModeContinuous)
      if (printOnError(err, "Unable to set acquisition mode to continuous (entry int value retrieval)."))
        return err
    } else {
      printRetrieveNodeFailure("entry", "AcquisitionMode 'Continuous'")
      return spinError.SPINNAKER_ERR_ACCESS_DENIED
    }

    // Set integer as new value of enumeration node
    if (isWritable(hAcquisitionMode, "AcquisitionMode")) {
      err = spinEnumerationSetIntValue(hAcquisitionMode, acquisitionModeContinuous.get)
      if (printOnError(err, "Unable to set acquisition mode to continuous (entry int value setting)."))
        return err
    } else {
      printRetrieveNodeFailure("entry", "AcquisitionMode")
      return spinError.SPINNAKER_ERR_ACCESS_DENIED
    }

    // Begin acquiring images
    err = spinCameraBeginAcquisition(hCam)
    if (printOnError(err, "Unable to begin image acquisition."))
      return err

    printf("Acquiring images...\n")

    // Retrieve device serial number for filename
    nodeGetStringValueOpt(hNodeMapTLDevice, "DeviceSerialNumber") match {
      case Some(sn) => println(s"Device serial number retrieved as $sn...")
      case None     => printRetrieveNodeFailure("node", "DeviceSerialNumber")
    }

    //
    // Create Image Processor context for post processing images
    //
    val hImageProcessor = new spinImageProcessor()
    err = spinImageProcessorCreate(hImageProcessor)
    printOnError(err, "Unable to create image processor. Non-fatal error.")

    //
    // Set default image processor color processing method
    //
    // *** NOTES ***
    // By default, if no specific color processing algorithm is set, the image
    // processor will default to NEAREST_NEIGHBOR method.
    //
    err = spinImageProcessorSetColorProcessing(hImageProcessor, SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR)
    printOnError(err, "Unable to set image processor color processing method. Non-fatal error.")

    // Retrieve, convert, and save images
    for (imageCnt <- hImages.indices) {

      // Retrieve next received image
      val hResultImage = new spinImage()

      err = spinCameraGetNextImageEx(hCam, 1000, hResultImage)
      var continueToNextIteration = false
      if (printOnError(err, "Unable to get next image. Non-fatal error."))
        continueToNextIteration = true

      if (!continueToNextIteration) {

        // Ensure image completion
        val isIncomplete = new BytePointer(1)
        var hasFailed    = false
        err = spinImageIsIncomplete(hResultImage, isIncomplete)
        if (printOnError(err, "Unable to determine image completion. Non-fatal error."))
          hasFailed = true

        // Check image for completion
        if (isIncomplete.getBool) {
          val imageStatus = new IntPointer(1L).put(SPINNAKER_IMAGE_STATUS_NO_ERROR.value)
          err = spinImageGetStatus(hResultImage, imageStatus)
          if (
            printOnError(
              err,
              "Unable to retrieve image status. Non-fatal error. " + findImageStatusNameByValue(imageStatus.get)
            )
          )
            println("Image incomplete with image status " + findImageStatusNameByValue(imageStatus.get) + "...")

          hasFailed = true

          err = spinImageRelease(hResultImage)
          printOnError(err, "Unable to release image. Non-fatal error.")
        }

        // Release incomplete or failed image
        if (hasFailed) {

          err = spinImageRelease(hResultImage)
          printOnError(err, "Unable to release image. Non-fatal error.")

        } else {

          // Print image information
          print("Grabbed image " + imageCnt)

          // Retrieve image width
          val width = new SizeTPointer(1)
          err = spinImageGetWidth(hResultImage, width)
          if (printOnError(err, "spinImageGetWidth()"))
            print(", width  = unknown")
          else
            print(", width  = " + width.get)

          // Retrieve image height
          val height = new SizeTPointer(1)
          err = spinImageGetHeight(hResultImage, height)
          if (printOnError(err, "spinImageGetHeight()"))
            println(", height = unknown")
          else
            println(", height = " + height.get)

          // Convert image to mono 8
          val hConvertedImage = new spinImage()
          err = spinImageCreateEmpty(hConvertedImage)
          if (printOnError(err, "Unable to create image. Non-fatal error."))
            hasFailed = true

          err = spinImageProcessorConvert(
            hImageProcessor,
            hResultImage,
            hConvertedImage,
            spinPixelFormatEnums.PixelFormat_Mono8
          )
          if (printOnError(err, "Unable to convert image. Non-fatal error."))
            hasFailed = true

          if (!hasFailed) {
            hImages(imageCnt) = hConvertedImage
          }

          // Release image
          err = spinImageRelease(hResultImage)
          printOnError(err, "Unable to release image. Non-fatal error.")
        }
      }
    }

    //
    // Destroy Image Processor context
    //
    // *** NOTES ***
    // Image processor context needs to be destroyed after all image processing
    // are complete to avoid memory leaks.
    //
    err = spinImageProcessorDestroy(hImageProcessor)
    printOnError(err, "Unable to destroy image processor. Non-fatal error.")

    // End Acquisition
    err = spinCameraEndAcquisition(hCam)
    printOnError(err, "Unable to end acquisition.")

    err
  }

  /**
   * This function acts as the body of the example;
   * please see `NodeMapInfo_C` example for more in-depth comments on setting up cameras.
   */
  def runSingleCamera(hCam: spinCamera): spinError = {
    var err = spinError.SPINNAKER_ERR_SUCCESS

    // Retrieve TL device nodemap and print device information
    val hNodeMapTLDevice = new spinNodeMapHandle()

    err = spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice)
    if (err.intern() != spinError.SPINNAKER_ERR_SUCCESS) {
      printError(err, "Unable to retrieve TL device nodemap. Non-fatal error...\n")
    } else {
      err = printDeviceInfo(hNodeMapTLDevice)
    }

    // Initialize camera
    err = spinCameraInit(hCam)
    if (printOnError(err, "Unable to initialize camera."))
      return err

    // Retrieve GenICam nodemap
    val hNodeMap = new spinNodeMapHandle
    err = spinCameraGetNodeMap(hCam, hNodeMap)
    if (printOnError(err, "Unable to retrieve GenICam nodemap."))
      return err

    // Acquire images
    val hImages = new Array[spinImage](NUM_IMAGES)
    err = acquireImages(hCam, hNodeMap, hNodeMapTLDevice, hImages)
    if (printOnError(err, "acquireImages"))
      return err

    err = saveArrayToVideo(hNodeMap, hNodeMapTLDevice, hImages)
    if (printOnError(err, "saveArrayToVideo"))
      return err

    // Deinitialize camera
    err = spinCameraDeInit(hCam)
    if (printOnError(err, "Unable to deinitialize camera.")) return err

    err
  }

  /**
   * Example entry point; please see `Enumeration_C` example for more in-depth comments on preparing and cleaning
   * up the system.
   */
  def main(args: Array[String]): Unit = {

    var err = spinError.SPINNAKER_ERR_SUCCESS

    // Retrieve singleton reference to system object
    val hSystem = new spinSystem()
    err = spinSystemGetInstance(hSystem)
    exitOnError(err, "Unable to retrieve system instance.")

    // Print out current library version
    val hLibraryVersion = new spinLibraryVersion()
    spinSystemGetLibraryVersion(hSystem, hLibraryVersion)
    printf(
      "Spinnaker library version: %d.%d.%d.%d\n\n%n",
      hLibraryVersion.major(),
      hLibraryVersion.minor(),
      hLibraryVersion.`type`(),
      hLibraryVersion.build()
    )

    // Retrieve list of cameras from the system
    val hCameraList = new spinCameraList()
    err = spinCameraListCreateEmpty(hCameraList)
    exitOnError(err, "Unable to create camera list.")

    err = spinSystemGetCameras(hSystem, hCameraList)
    exitOnError(err, "Unable to retrieve camera list.")

    // Retrieve number of cameras
    val numCameras = new SizeTPointer(1)
    err = spinCameraListGetSize(hCameraList, numCameras)
    exitOnError(err, "Unable to retrieve number of cameras.")
    println("Number of cameras detected: " + numCameras.get + "\n")

    // Finish if there are no cameras
    var errReturn = spinError.SPINNAKER_ERR_SUCCESS.value
    if (numCameras.get > 0) {

      // Run example on each camera
      for (i <- 0 until numCameras.get.toInt) {
        printf("\nRunning example for camera %d...\n", i)

        // Select camera
        val hCamera = new spinCamera()

        err = spinCameraListGet(hCameraList, i, hCamera)
        if (err.intern() != spinError.SPINNAKER_ERR_SUCCESS) {
          printError(err, "Unable to retrieve camera from list. Aborting with error %d...\n")
          errReturn = err.value
        } else {
          // Run example
          err = runSingleCamera(hCamera)
          if (err.intern() != spinError.SPINNAKER_ERR_SUCCESS) {
            errReturn = err.value
          }
        }

        // Release camera
        err = spinCameraRelease(hCamera)
        if (err.intern() != spinError.SPINNAKER_ERR_SUCCESS) {
          errReturn = err.value
        }

        printf("Camera %d example complete...\n\n", i)
      }
    } else {
      println("Not enough cameras!\n")
      errReturn = -1
    }

    // Clear and destroy camera list before releasing system
    err = spinCameraListClear(hCameraList)
    exitOnError(err, "Unable to clear camera list.")

    err = spinCameraListDestroy(hCameraList)
    exitOnError(err, "Unable to destroy camera list.")

    // Release system
    err = spinSystemReleaseInstance(hSystem)
    exitOnError(err, "Unable to release system instance.")

    println("\nDone!\n")
    System.exit(errReturn)
  }

}
