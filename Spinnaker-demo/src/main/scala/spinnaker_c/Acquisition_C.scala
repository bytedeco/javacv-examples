package spinnaker_c

import org.bytedeco.javacpp.{BytePointer, IntPointer, LongPointer, SizeTPointer}
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.spinColorProcessingAlgorithm.SPINNAKER_COLOR_PROCESSING_ALGORITHM_HQ_LINEAR
import org.bytedeco.spinnaker.global.Spinnaker_C.spinImageStatus.SPINNAKER_IMAGE_STATUS_NO_ERROR
import spinnaker_c.helpers.*

import java.io.File
import scala.util.Using
import scala.util.control.Breaks.*

object Acquisition_C {
  private val MAX_BUFF_LEN = 256

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

      // Print out current library version
      printLibraryVersion(hSystem)

      // Retrieve list of cameras from the system
      val hCameraList = use(new spinCameraList())
      exitOnError(spinCameraListCreateEmpty(hCameraList), "Unable to create camera list.")

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

        runSingleCamera(hCamera)

        // Release camera
        printOnError(spinCameraRelease(hCamera), "Error releasing camera.")
        println(s"Camera $i + example complete...\n")
      }

      // Clear and destroy camera list before releasing system
      exitOnError(spinCameraListClear(hCameraList), "Unable to clear camera list.")

      exitOnError(spinCameraListDestroy(hCameraList), "Unable to destroy camera list.")

      // Release system
      exitOnError(spinSystemReleaseInstance(hSystem), "Unable to release system instance.")
    }
  }

  @throws[spinnaker_c.helpers.SpinnakerSDKException]
  def runSingleCamera(hCam: spinCamera): Unit = Using.Manager { use =>

    // Retrieve TL device nodemap and print device information
    val hNodeMapTLDevice = use(new spinNodeMapHandle)
    check(spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice), "Unable to retrieve TL device nodemap .")

    check(printDeviceInfo(hNodeMapTLDevice), "")

    // Initialize camera
    check(spinCameraInit(hCam), "Unable to initialize camera.")

    try {
      // Retrieve GenICam nodemap
      val hNodeMap = use(new spinNodeMapHandle)
      check(spinCameraGetNodeMap(hCam, hNodeMap), "Unable to retrieve GenICam nodemap.")

      // Acquire images// Acquire images
      acquireImages(hCam, hNodeMap, hNodeMapTLDevice)
    } finally {

      // Deinitialize camera
      check(spinCameraDeInit(hCam), "Unable to deinitialize camera.")
    }
  }

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
      val hAcquisitionMode = use(new spinNodeHandle()) // Empty handle, equivalent to NULL in C

      check(
        spinNodeMapGetNode(hNodeMap, use(new BytePointer("AcquisitionMode")), hAcquisitionMode),
        "Unable to set acquisition mode to continuous (node retrieval)."
      )

      // Retrieve entry node from enumeration node
      val hAcquisitionModeContinuous = use(new spinNodeHandle()) // Empty handle, equivalent to NULL in C

      if isReadable(hAcquisitionMode, "AcquisitionMode") then {
        check(
          spinEnumerationGetEntryByName(
            hAcquisitionMode,
            use(new BytePointer("Continuous")),
            hAcquisitionModeContinuous
          ),
          "Unable to set acquisition mode to continuous (entry 'continuous' retrieval)."
        )
      } else {
        printRetrieveNodeFailure("entry", "AcquisitionMode")
        throw new SpinnakerSDKException("Node 'AcquisitionMode' is not readable", spinError.SPINNAKER_ERR_ACCESS_DENIED)
      }

      // Retrieve integer from entry node
      val acquisitionModeContinuous = use(new LongPointer(1))
      if isReadable(hAcquisitionModeContinuous, "AcquisitionModeContinuous") then {
        check(
          spinEnumerationEntryGetIntValue(hAcquisitionModeContinuous, acquisitionModeContinuous),
          "Unable to set acquisition mode to continuous (entry int value retrieval)."
        )
      } else {
        printRetrieveNodeFailure("entry", "AcquisitionMode 'Continuous'")
        throw new SpinnakerSDKException(
          "Node 'AcquisitionModeContinuous' is not readable",
          spinError.SPINNAKER_ERR_ACCESS_DENIED
        )
      }

      // Set integer as new value of enumeration node
      if isWritable(hAcquisitionMode, "AcquisitionMode") then {
        check(
          spinEnumerationSetIntValue(hAcquisitionMode, acquisitionModeContinuous.get),
          "Unable to set acquisition mode to continuous (entry int value setting)."
        )
      } else {
        printRetrieveNodeFailure("entry", "AcquisitionMode 'Continuous'")
        throw new SpinnakerSDKException(
          "Node 'AcquisitionMode' is not writable",
          spinError.SPINNAKER_ERR_ACCESS_DENIED
        )
      }

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
          val hResultImage = new spinImage // NULL;

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
              new BytePointer(filename),
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
