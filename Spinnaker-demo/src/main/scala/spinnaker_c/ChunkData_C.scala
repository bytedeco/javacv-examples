package spinnaker_c

import org.bytedeco.javacpp.*
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import spinnaker_c.helpers.*

import java.io.File
import scala.util.Using
import scala.util.control.Breaks.{break, breakable}

/**
 *  ChunkData_C.c shows how to get chunk data on an image, either from
 *  the nodemap or from the image itself. It relies on information provided in
 *  the Enumeration_C, Acquisition_C, and NodeMapInfo_C examples.
 *
 *  It can also be helpful to familiarize yourself with the ImageFormatControl_C
 *  and Exposure_C examples. As they are somewhat shorter and simpler, either
 *  provides a strong introduction to camera customization.
 *
 *  Chunk data provides information on various traits of an image. This includes
 *  identifiers such as frame ID, properties such as black level, and more. This
 *  information can be acquired from either the nodemap or the image itself.
 *
 *  It may be preferable to grab chunk data from each individual image, as it
 *  can be hard to verify whether data is coming from the correct image when
 *  using the nodemap. This is because chunk data retrieved from the nodemap is
 *  only valid for the current image; when spinCameraGetNextImage() or
 *  spinCameraGetNextImageEx() is called, chunk data will be updated to that of
 *  the new current image.
 */
object ChunkData_C {

  private enum ChunkDataType:
    case Image, NodeMap

  private val chosenChunkData = ChunkDataType.Image

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

      // Configure chunk data
      configureChunkData(hNodeMap)

      // Acquire images
      acquireImages(hCam, hNodeMap, hNodeMapTLDevice)

      // Disable chunck data
      disableChunkData(hNodeMap)
    finally
      // Deinitialize camera
      check(spinCameraDeInit(hCam), "Unable to deinitialize camera.")

  }.get

  /**
   * This function configures the camera to add chunk data to each image. It does
   * this by enabling each type of chunk data after enabling chunk data mode.
   * When chunk data mode is turned on, the data is made available in both the nodemap
   * and each image.
   */
  private def configureChunkData(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>
    printf("\n\n*** CONFIGURING CHUNK DATA ***\n\n")

    //
    // Activate chunk mode
    //
    // *** NOTES ***
    // Once enabled, chunk data will be available at the end of hte payload of
    // every image captured until it is disabled. Chunk data can also be
    // retrieved from the nodemap.
    //
    booleanSetValue(hNodeMap, "ChunkModeActive", true)
    printf("Chunk mode activated...\n")

    //
    // Enable all types of chunk data
    //
    // *** NOTES ***
    // Enabling chunk data requires working with nodes: "ChunkSelector" is an
    // enumeration selector node and "ChunkEnable" is a boolean. It requires
    // retrieving the selector node (which is of enumeration node type),
    // selecting the entry of the chunk data to be enabled, retrieving the
    // corresponding boolean, and setting it to true.
    //
    // In this example, all chunk data is enabled, so these steps are performed
    // in a loop. Once this is complete, chunk mode still needs to be activated.
    //
    // Retrieve selector node, check if available and readable and writable
    val hChunkSelector = nodeMapGetNode(hNodeMap, "ChunkSelector")
    checkIsReadable(hChunkSelector, "ChunkSelector")

    val hNumEntries = use(new SizeTPointer(1)).put(0)
    check(spinEnumerationGetNumEntries(hChunkSelector, hNumEntries), "Unable to retrieve number of entries.")
    printf("Enabling entries...\n")

    for i <- 0 until hNumEntries.get.toInt do
      breakable {
        // Retrieve entry node
        val hEntry = use(new spinNodeHandle())
        {
          val err = spinEnumerationGetEntryByIndex(hChunkSelector, i, hEntry)
          if isError(err) then
            printf("\tUnable to enable chunk entry (error %d)...\n\n", err.value)
            break
        }

        // Check if readable, retrieve entry name
        val entryName = {
          val pEntryName   = use(new BytePointer(MAX_BUFF_LEN))
          val lenEntryName = use(new SizeTPointer(1)).put(MAX_BUFF_LEN)
          if isReadable(hEntry, "ChunkEntry") then {
            val err = spinNodeGetDisplayName(hEntry, pEntryName, lenEntryName)
            if isSuccess(err) then
              pEntryName.getString.trim
            else
              printf("\t%d: unable to retrieve chunk entry display name (error %d)...\n", i, err.value);
              "???"
          } else
            break
        }

        // Retrieve enum entry integer value
        val value = {
          val hValue = use(new LongPointer(1)).put(0)
          val err    = spinEnumerationEntryGetIntValue(hEntry, hValue)
          if isError(err) then
            printf("\t%s: unable to get chunk entry value (error %d)...\n", entryName, err.value)
            break
          hValue.get()
        }

        // Set integer value
        if isWritable(hChunkSelector, "ChunkSelector") then {
          val err = spinEnumerationSetIntValue(hChunkSelector, value)
          if isError(err) then
            printf("\t%s: unable to set chunk entry value (error %d)...\n", entryName, err.value)
            break
        } else
          printf("\t%s: unable to write to chunk entry value...\n", entryName)
        ;

        // Retrieve corresponding chunk enable node
        val hChunkEnable = use(new spinNodeHandle())
        {
          val err = spinNodeMapGetNode(hNodeMap, use(new BytePointer("ChunkEnable")), hChunkEnable)
          if isError(err) then
            printf("\t%s: unable to get entry from nodemap (error %d)...\n", entryName, err.value)
            break
        }

        // Retrieve chunk enable value and set to true if necessary
        val isEnabled = use(new BytePointer(1)).put(False)
        if isReadable(hChunkEnable, "ChunkEnable") then {
          val err = spinBooleanGetValue(hChunkEnable, isEnabled)
          if isError(err) then {
            printf("\t%s: unable to get chunk entry boolean value (error %d)...\n", entryName, err.value)
            break
          }
        } else
          printf("\t%s: not writable\n", entryName)
          break

        // Consider the case in which chunk data is enabled but not writable
        if !isEnabled.getBool || !isWritable(hChunkEnable, "ChunkEnable") then {
          // Set chunk enable value to true
          val err = spinBooleanSetValue(hChunkEnable, True)
          if isError(err) then
            printf("\t%s: unable to set chunk entry boolean value (error %d)...\n", entryName, err.value)
            break
        }

        printf("\t%s: enabled\n", entryName)
      }
  }.get

  /**
   * This function displays a select amount of chunk data from the image. Unlike
   * accessing chunk data via the nodemap, there is no way to loop through all
   * available data.
   */
  private def displayChunkDataFromImage(hImage: spinImage): Unit = {
    printf("Print chunk data from image...\n")

    //
    // Retrieve exposure time; exposure time recorded in microseconds
    //
    // *** NOTES ***
    // Floating point numbers are returned as a double
    //
    val exposureTime = imageChunkDataGetFloatValue(hImage, "ChunkExposureTime")
    printf("\tExposure time: %f\n", exposureTime)

    //
    // Retrieve compression ratio
    //
    // *** NOTES ***
    // Floating point numbers are returned as a double
    //
    val compressionRatio = imageChunkDataGetFloatValue(hImage, "ChunkCompressionRatio")
    printf("\tCompression ratio: %f\n", compressionRatio)

    //
    // Retrieve frame ID
    //
    // *** NOTES ***
    // Integers are returned as an int64_t.
    //
    val frameID = imageChunkDataGetIntValue(hImage, "ChunkFrameID")
    printf("\tFrame ID: %d\n", frameID)

    // Retrieve gain; gain recorded in decibels
    val gain = imageChunkDataGetFloatValue(hImage, "ChunkGain")
    printf("\tGain: %f\n", gain)

    // Retrieve height; height recorded in pixels
    val height = imageChunkDataGetIntValue(hImage, "ChunkHeight")
    printf("\tHeight: %d\n", height)

    // Retrieve offset X; offset X recorded in pixels
    val offsetX = imageChunkDataGetIntValue(hImage, "ChunkOffsetX")
    printf("\tOffset X: %d\n", offsetX)

    // Retrieve offset Y; offset Y recorded in pixels
    val offsetY = imageChunkDataGetIntValue(hImage, "ChunkOffsetY")
    printf("\tOffset Y: %d\n", offsetY)

    // Retrieve width; width recorded in pixels
    val width = imageChunkDataGetIntValue(hImage, "ChunkWidth")
    printf("\tWidth: %d\n", width)

    // Retrieve black level; black level recorded as a percentage
    val blackLevel = imageChunkDataGetFloatValue(hImage, "ChunkBlackLevel")
    printf("\tBlack level: %f\n", blackLevel)
  }

  /**
   * This function displays all available chunk data by looping through the chunk
   * data category node on the nodemap.
   */
  private def displayChunkDataFromNodeMap(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>

    //
    // Retrieve chunk data information nodes
    //
    // *** NOTES ***
    // As well as being written into the payload of the image, chunk data is
    // accessible on the GenICam nodemap. Insofar as chunk data is
    // enabled, it is available from both sources.
    //
    val hChunkDataControl = use(nodeMapGetNode(hNodeMap, "ChunkDataControl"))
    checkIsReadable(hChunkDataControl, "ChunkDataControl")

    val numFeatures = {
      val pNumFeatures = use(new SizeTPointer(1))
      check(spinCategoryGetNumFeatures(hChunkDataControl, pNumFeatures), "Unable to retrieve number of nodes")
      pNumFeatures.get
    }
    printf("Printing chunk data from nodemap...\n")

    for i <- 0 until numFeatures.toInt do
      breakable {
        val hFeatureNode = use(new spinNodeHandle())

        // Retrieve node
        if isReadable(hChunkDataControl, "ChunkDataControl") then {
          val err = spinCategoryGetFeatureByIndex(hChunkDataControl, i, hFeatureNode)
          if isError(err) then {
            printf("Unable to retrieve node (error %d)...\n\n", err.value)
            break
          }
        } else
          printf("Unable to retrieve node...\n\n")
          break

        // Retrieve node name
        val featureName = {
          val pFeatureName   = use(BytePointer(MAX_BUFF_LEN))
          val lenFeatureName = use(new SizeTPointer()).put(MAX_BUFF_LEN)
          val err            = spinNodeGetName(hFeatureNode, pFeatureName, lenFeatureName)
          if isError(err) then "Unknown name" else pFeatureName.getString.trim
        }

        val featureType = use(new IntPointer(1L))
        if isReadable(hFeatureNode, featureName) then {
          val err = spinNodeGetType(hFeatureNode, featureType)
          if isError(err) then {
            printf("Unable to retrieve node type. Non-fatal error %d...\n\n", err.value)
            break
          }
        } else
          printf("Unable to retrieve node type. Non-fatal error...\n\n")
          break

        featureType.get match
          case spinNodeType.IntegerNode.value =>
            // Print integer node type value
            val featureValue = use(new LongPointer(1)).put(0)
            spinIntegerGetValue(hFeatureNode, featureValue)
            printf("\t%s: %d\n", featureName, featureValue.get().toInt)

          case spinNodeType.FloatNode.value =>
            // Print float node type value
            val featureValue = use(new DoublePointer(1)).put(0)
            spinFloatGetValue(hFeatureNode, featureValue)
            printf("\t%s: %f\n", featureName, featureValue.get())

          case spinNodeType.BooleanNode.value =>
            //
            // Print boolean node type value
            //
            // *** NOTES ***
            // Boolean information is manipulated to output the more-easily
            // identifiable 'true' and 'false' as opposed to '1' and '0'.
            //
            val featureValue = use(new BytePointer(1L)).put(False)
            spinBooleanGetValue(hFeatureNode, featureValue)
            if featureValue.getBool then
              printf("\t%s: true\n", featureName)
            else
              printf("\t%s: false\n", featureName)
      }
  }.get

  /**
   * This function acquires and saves 10 images from a device; please see
   *  Acquisition_C example for more in-depth comments on the acquisition of
   *  images.
   */
  def acquireImages(hCam: spinCamera, hNodeMap: spinNodeMapHandle, hNodeMapTLDevice: spinNodeMapHandle): Unit =
    Using.Manager { use =>

      printf("\n*** IMAGE ACQUISITION ***\n\n")

      // Set acquisition mode to continuous
      setEnumerationNodeValue(hNodeMap, "AcquisitionMode", "Continuous")
      printf("Acquisition mode set to continuous...\n")

      // Begin acquiring images
      check(spinCameraBeginAcquisition(hCam), "Unable to begin image acquisition")
      printf("Acquiring images...\n")

      // Retrieve device serial number for filename
      val deviceSerialNumber = nodeGetStringValueOpt(hNodeMapTLDevice, "DeviceSerialNumber").getOrElse("")
      printf("Device serial number retrieved as %s...\n", deviceSerialNumber)
      printf("\n")

      // Retrieve, convert, and save images

      //
      // Create Image Processor context for post processing images
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
          // Retrieve next received image
          val hResultImage = use(new spinImage()) // NULL;

          val err1 = spinCameraGetNextImageEx(hCam, 1000, hResultImage)
          if printOnError(err1, "Unable to get next image. Non-fatal error.") then
            break

          // Ensure image completion
          val isIncomplete = use(new BytePointer(1))
          var hasFailed    = false
          val err2         = spinImageIsIncomplete(hResultImage, isIncomplete)
          if printOnError(err2, "Unable to determine image completion. Non-fatal error.") then
            hasFailed = true

          if isIncomplete.getBool then {
            val imageStatus = use(new IntPointer(1L)).put(spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR.value)
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
          val width = use(new SizeTPointer(1)).put(0)
          val err5  = spinImageGetWidth(hResultImage, width)
          printOnError(err5, "Unable to retrieve image width.")

          val height = use(new SizeTPointer(1)).put(0)
          val err6   = spinImageGetHeight(hResultImage, height)
          printOnError(err6, "Unable to retrieve image height.")

          printf("Grabbed image %d, width = %d, height = %d\n", imageCnt, width.get(), height.get())

          // Convert image to mono 8
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

          // Display chunk data
          chosenChunkData match
            case ChunkDataType.Image =>
              displayChunkDataFromImage(hResultImage);
            case ChunkDataType.NodeMap =>
              displayChunkDataFromNodeMap(hNodeMap);
          printf("\n")

          // Destroy converted image
          printOnError(spinImageDestroy(hConvertedImage), "Unable to destroy image. Non-fatal error.")

          // Release image
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

      // End Acquisition
      check(spinCameraEndAcquisition(hCam), "Unable to end acquisition.")
    }.get

  /** This function disables each type of chunk data before disabling chunk data mode. */
  private def disableChunkData(hNodeMap: spinNodeMapHandle): Unit = Using.Manager { use =>

    val hChunkSelector = nodeMapGetNode(hNodeMap, "ChunkSelector")
    checkIsReadable(hChunkSelector, "ChunkSelector")

    val hNumEntries = use(new SizeTPointer(1)).put(0)
    check(spinEnumerationGetNumEntries(hChunkSelector, hNumEntries), "Unable to retrieve number of entries.")
    printf("Disabling entries...\n")

    for i <- 0 until hNumEntries.get.toInt do {
      breakable {
        // Retrieve entry node
        val hEntry = use(new spinNodeHandle())
        {
          val err = spinEnumerationGetEntryByIndex(hChunkSelector, i, hEntry)
          if isError(err) then
            printf("\tUnable to enable chunk entry (error %d)...\n\n", err.value)
            break
        }

        // Retrieve entry name
        // Check if readable, retrieve entry name
        val entryName = {
          val pEntryName = use(new BytePointer(MAX_BUFF_LEN))
          if isReadable(hEntry, "ChunkEntry") then {
            val lenEntryName = use(new SizeTPointer(1)).put(MAX_BUFF_LEN)
            val err          = spinNodeGetDisplayName(hEntry, pEntryName, lenEntryName)
            if isSuccess(err) then
              pEntryName.getString.trim
            else
              printf("\t%d: unable to retrieve chunk entry display name (error %d)...\n", i, err.value)
              "???"
          } else
            break
        }

        // Retrieve enum entry integer value
        val value = {
          val hValue = use(new LongPointer(1)).put(0)
          val err    = spinEnumerationEntryGetIntValue(hEntry, hValue)
          if isError(err) then
            printf("\t%s: unable to get chunk entry value (error %d)...\n", entryName, err.value)
            break
          hValue.get()
        }

        // Set integer value
        if isWritable(hChunkSelector, "ChunkSelector") then {
          val err = spinEnumerationSetIntValue(hChunkSelector, value)
          if isError(err) then
            printf("\t%s: unable to set chunk entry value (error %d)...\n", entryName, err.value)
            break
        } else
          printf("\t%s: unable to write to chunk entry value...\n", entryName)

          // Retrieve corresponding chunk enable node
          val hChunkEnable = use(new spinNodeHandle())
          {
            val err = spinNodeMapGetNode(hNodeMap, use(new BytePointer("ChunkEnable")), hChunkEnable)
            if isError(err) then
              printf("\t%s: unable to get entry from nodemap (error %d)...\n", entryName, err.value)
              break
          }

          // Retrieve chunk enable value and set to false if necessary
          val isEnabled = use(new BytePointer(1)).put(False)
          if isReadable(hChunkEnable, "ChunkEnable") then {
            val err = spinBooleanGetValue(hChunkEnable, isEnabled)
            if isError(err) then {
              printf("\t%s: unable to get chunk entry boolean value (error %d)...\n", entryName, err.value)
              break
            }
          } else
            printf("\t%s: not writable\n", entryName)
            break

          // Consider the case in which chunk data is enabled but not writable
          if isEnabled.getBool then {
            val err = spinBooleanSetValue(hChunkEnable, False)
            if isError(err) then {
              printf("\t%s: unable to set chunk entry boolean value (error %d)...\n", entryName, err.value)
              break
            }
          }

          printf("\t%s: disabled\n", entryName);
      }
    }

    printf("\n")

    // Disabling ChunkModeActive
    booleanSetValue(hNodeMap, "ChunkModeActive", false)
    printf("Chunk mode deactivated...\n");
  }.get

}
