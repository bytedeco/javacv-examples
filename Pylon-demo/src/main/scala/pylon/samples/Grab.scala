package pylon.samples


import java.io.File

import org.bytedeco.javacpp.Pylon5._

/**
  * This sample illustrates how to grab and process images using the CInstantCamera class.
  * The images are grabbed and processed asynchronously, i.e.,
  * while the application is processing a buffer, the acquisition of the next buffer is done
  * in parallel.
  *
  * The CInstantCamera class uses a pool of buffers to retrieve image data
  * from the camera device. Once a buffer is filled and ready,
  * the buffer can be retrieved from the camera object for processing. The buffer
  * and additional image data are collected in a grab result.
  */
object Grab extends App {

  println("Dir: " + new File(".").getCanonicalPath)

  // Number of images to be grabbed.
  val countOfImagesToGrab = 100

  // The exit code of the sample application.
  var exitCode = 0

  // Call PylonInitialize  to ensure the pylon runtime system
  // is initialized during the lifetime of this object.
  println("PylonInitialize()")
  PylonInitialize()

  try {

    val version = GetPylonVersionString()

    println("Pylon version: " + version.getString)

    // Get all attached devices and exit application if no device is found.
    // Get the transport layer factory.
    val tlFactory = CTlFactory.GetInstance()
    val devices = new DeviceInfoList()
    if (tlFactory.EnumerateDevices(devices) == 0) {
      throw new RuntimeException("No camera present.")
    }

    println("Create device handle for the first available camera.")
    val device = CTlFactory.GetInstance().CreateFirstDevice()
    require(device != null)

    // Creating camera handle
    val camera = new CInstantCamera(device)

    println("Using device " + camera.GetDeviceInfo().GetModelName().c_str().getString)

    // The parameter MaxNumBuffer can be used to control the count of buffers
    // allocated for grabbing. The default value of this parameter is 10.
    camera.MaxNumBuffer().SetValue(5)

    // Start the grabbing of c_countOfImagesToGrab images.
    // The camera device is parameterized with a default configuration which
    // sets up free-running continuous acquisition.
    camera.StartGrabbing(countOfImagesToGrab)

    // This smart pointer will receive the grab result data.
    val ptrGrabResult = new CGrabResultPtr()

    // Camera.StopGrabbing() is called automatically by the RetrieveResult() method
    // when c_countOfImagesToGrab images have been retrieved.
    while (camera.IsGrabbing()) {
      // Wait for an image and then retrieve it. A timeout of 5000 ms is used.
      camera.RetrieveResult(5000, ptrGrabResult, TimeoutHandling_ThrowException)

      // Image grabbed successfully?
      if (ptrGrabResult.access().GrabSucceeded()) {
        // Access the image data.
        println("SizeX: " + ptrGrabResult.access().GetWidth())
        println("SizeY: " + ptrGrabResult.access().GetHeight())
        val pImageBuffer = ptrGrabResult.access().GetBuffer()
        val v = pImageBuffer.asByteBuffer().get() & 0xFF
        println("Gray value of first pixel: " + v)

        //#ifdef PYLON_WIN_BUILD
        //                // Display the grabbed image.
        //                Pylon::DisplayImage(1, ptrGrabResult);
        //#endif
      }
      else {
        println("Error: " + ptrGrabResult.access().GetErrorCode() + " " +
          ptrGrabResult.access().GetErrorDescription().c_str().getString)
      }
    }
  } catch {
    case t: RuntimeException =>
      println("An exception occurred: " + t.getMessage)
    //      t.printStackTrace()
  }


  println("PylonTerminate()")
  PylonTerminate()

}
