package pylon.samples

import org.bytedeco.javacpp.Pylon5._

/**
  * Based on Pylon SDK example `DeviceRemovalHandling`.
  *
  * This sample program demonstrates how to be informed about the removal of a camera device.
  * It also shows how to reconnect to a removed device.
  *
  */
object DeviceRemovalHandling extends App {

  /** When using Device Specific Instant Camera classes there are specific Configuration event handler classes
    * available which can be used, for example `Pylon5.CBaslerGigEConfigurationEventHandler` or
    * `Pylon5.CBasler1394ConfigurationEventHandler`
    * Example of a configuration event handler that handles device removal events.
    */
  class SampleConfigurationEventHandler extends CConfigurationEventHandler {
    /**
      * This method is called from a different thread when the camera device removal has been detected.
      */
    override def OnCameraDeviceRemoved(camera: CInstantCamera) {
      // Print two new lines, just for improving printed output.
      println("\n\nSampleConfigurationEventHandler.OnCameraDeviceRemoved() called.")
    }
  }

  // Time to wait in quarters of seconds.
  val loopCounterInitialValue = 20 * 4

  val configurationEventPrinter = new ConfigurationEventPrinter()

  // Before using any pylon methods, the pylon runtime must be initialized.
  PylonInitialize()

  try {
    // Get the transport layer factory.
    val tlFactory = CTlFactory.GetInstance()
    val devices = new DeviceInfoList()
    if (tlFactory.EnumerateDevices(devices) == 0) {
      throw new RuntimeException("No camera present.")
    }

    // Create device handle for the first available camera.
    val device = CTlFactory.GetInstance().CreateFirstDevice()
    require(device != null)

    // Creating camera handle
    val camera = new CInstantCamera(device)

    // Print the camera information.
    printCameraInfo(camera)

    // For demonstration purposes only, register another configuration event handler that handles device removal.
    camera.RegisterConfiguration(new SampleConfigurationEventHandler(), RegistrationMode_Append, Cleanup_Delete)

    // For demonstration purposes only, add a sample configuration event handler to print out information
    // about camera use.
    camera.RegisterConfiguration(configurationEventPrinter, RegistrationMode_Append, Cleanup_Delete)

    // Open the camera. Camera device removal is only detected while the camera is open.
    camera.Open()

    // Now, try to detect that the camera has been removed:


    // TODO: add HeartbeatHelper implementation, needed for GigE cameras
    //        /////////////////////////////////////////////////// don't single step beyond this line  (see comments above)
    //
    //        // Before testing the callbacks, we manually set the heartbeat timeout to a short value when using GigE cameras.
    //        // Since for debug versions the heartbeat timeout has been set to 5 minutes, it would take up to 5 minutes
    //        // until detection of the device removal.
    //        val heartbeatHelper = new CHeartbeatHelper(camera)
    //        heartbeatHelper.SetValue(1000);  // 1000 ms timeout

    try {
      // Ask the user to disconnect a device
      var loopCount = loopCounterInitialValue
      println("\nPlease disconnect the device (timeout " + (loopCount / 4) + "s) ")

      // Get a camera parameter using generic parameter access.
      // TODO: val width = new GenICam3.IInteger(camera.GetNodeMap().GetNode(toGCString("Width")))

      // The following loop accesses the camera. It could also be a loop that is
      // grabbing images. The device removal is handled in the exception handler.
      var break = false
      while (loopCount > 0 & !break) {
        // Print a "." every few seconds to tell the user we're waiting for the callback.
        loopCount = loopCount - 1
        if (loopCount % 4 == 0) {
          print(".")
        }
        WaitObject.Sleep(250)

        // Change the width value in the camera depending on the loop counter.
        // Any access to the camera like setting parameters or grabbing images
        // will fail throwing an exception if the camera has been disconnected.
        // TODO: width.SetValue(width.GetMax() - (width.GetInc() * (loopCount % 2)))

        if (camera.IsCameraDeviceRemoved()) break = true
      }
    }
    catch {
      case ex: Exception =>
        if (camera.IsCameraDeviceRemoved()) {
          // The camera device has been removed. This caused the exception.
          println()
          println("The camera has been removed from the PC.")
          println("The camera device removal triggered an exception:" + ex.getMessage)
        }
        else {
          // An unexpected error has occurred.
          // In this example it is handled by exiting the program.
          throw ex
        }
    }

    if (!camera.IsCameraDeviceRemoved())
      println("\nTimeout expired")

    /////////////////////////////////////////////////// Safe to use single stepping (see comments above).

    // Now try to find the detached camera after it has been attached again:

    // Create a device info object for remembering the camera properties.
    val info = new CDeviceInfo()

    // Remember the camera properties that allow detecting the same camera again.
    info.SetDeviceClass(camera.GetDeviceInfo().GetDeviceClass())
    info.SetSerialNumber(camera.GetDeviceInfo().GetSerialNumber())

    // Destroy the Pylon Device representing the detached camera device.
    // It cannot be used anymore.
    camera.DestroyDevice()

    // Ask the user to connect the same device.
    var loopCount = loopCounterInitialValue
    println("\nPlease connect the same device to the PC again (timeout " + (loopCount / 4) + "s) ")

    // Create a filter containing the CDeviceInfo object info which describes the properties of the device we are looking for.
    val filter = new DeviceInfoList()
    filter.push_back(info)

    var break = false
    while (loopCount > 0 && !break) {
      loopCount -= 1
      // Print a . every few seconds to tell the user we're waiting for the camera to be attached
      if (loopCount % 4 == 0) print(".")

      // Try to find the camera we are looking for.
      val devices = new DeviceInfoList
      if (tlFactory.EnumerateDevices(devices, filter) > 0) {
        // Print two new lines, just for improving printed output.
        println("\n")

        // The camera has been found. Create and attach it to the Instant Camera object.
        camera.Attach(tlFactory.CreateDevice(devices.at(0)))
        //Exit waiting
        break = true
      }

      WaitObject.Sleep(250)
    }

    // If the camera has been found.
    if (camera.IsPylonDeviceAttached()) {
      printCameraInfo(camera)

      // All configuration objects and other event handler objects are still registered.
      // The configuration objects will parameterize the camera device and the instant
      // camera will be ready for operation again.

      // Open the camera.
      camera.Open()

      // Now the Instant Camera object can be used as before.
    } else {
      // Timeout
      println("\nTimeout expired.")
    }

  } catch {
    case t: RuntimeException =>
      println("An exception occurred: " + t.getMessage)
    //      t.printStackTrace()
  }

  // Releases all pylon resources.
  PylonTerminate()


  def printCameraInfo(camera: CInstantCamera): Unit = {
    println("Using device " + camera.GetDeviceInfo().GetModelName().c_str().getString)
    println("Friendly Name: " + camera.GetDeviceInfo().GetFriendlyName().c_str().getString)
    println("Full Name    : " + camera.GetDeviceInfo().GetFullName().c_str().getString)
    println("SerialNumber : " + camera.GetDeviceInfo().GetSerialNumber().c_str().getString)
    println()

  }

}
