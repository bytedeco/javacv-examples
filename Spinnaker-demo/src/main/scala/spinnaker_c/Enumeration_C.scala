package spinnaker_c

import org.bytedeco.javacpp.SizeTPointer
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import spinnaker_c.helpers.*

object Enumeration_C {

  /**
   * Print information about cameras on the list
   *
   * @param hCameraList list of cameras that was already allocated and retrieved
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("When a Spinnaker SDK operation fails.")
  def printDeviceVendorAndModel(hCameraList: spinCameraList): Unit = {
    var err = spinError.SPINNAKER_ERR_SUCCESS

    // Retrieve number of cameras
    val numCameras = new SizeTPointer(1)
    err = spinCameraListGetSize(hCameraList, numCameras)
    check(err, "Unable to retrieve number of cameras.")

    // Print device vendor and model name for each camera on the interface
    for (i <- 0 until numCameras.get.toInt) {
      //
      // Select camera
      //
      // *** NOTES ***
      // Each camera is retrieved from a camera list with an index. If the
      // index is out of range, an exception is thrown.
      //
      // *** LATER ***
      // Each camera handle needs to be released before losing scope or the
      // system is released.
      //
      val hCam = new spinCamera()

      err = spinCameraListGet(hCameraList, i, hCam)
      check(err, "Unable to retrieve camera.")

      // Retrieve TL device nodemap; please see NodeMapInfo_C example for
      // additional comments on transport layer nodemaps.
      val hNodeMapTLDevice = new spinNodeMapHandle()

      err = spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice)
      check(err, "Unable to retrieve TL device nodemap.")

      //
      // Retrieve device vendor name
      //
      // *** NOTES ***
      // Grabbing node information requires first retrieving the node and
      // then retrieving its information. There are two things to keep in
      // mind. First, a node is distinguished by type, which is related
      // to its value's data type.  Second, nodes should be checked for
      // availability and readability/writability prior to making an
      // attempt to read from or write to the node.
      //
      val deviceVendorName = nodeGetStringValue(hNodeMapTLDevice, "DeviceVendorName")
      val deviceModelName  = nodeGetStringValue(hNodeMapTLDevice, "DeviceModelName")

      println(s"\tDevice $i / $deviceVendorName / $deviceModelName \n\n")

      //
      // Release camera before losing scope
      //
      // *** NOTES ***
      // Every handle that is created for a camera must be released before
      // the system is released or an exception will be thrown.
      //
      err = spinCameraRelease(hCam)
      check(err, "\"Unable to release camera.")
    }
  }

  /**
   * This function queries an interface for its cameras and then prints out
   * device information.
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("When a Spinnaker SDK operation fails.")
  def queryInterface(hInterface: spinInterface): Unit = {
    var err = spinError.SPINNAKER_ERR_SUCCESS

    //
    // Retrieve TL nodemap from interface
    //
    // *** NOTES ***
    // Each interface has a nodemap that can be retrieved in order to access
    // information about the interface itself, any devices connected, or
    // addressing information if applicable.
    //
    val hNodeMapInterface = new spinNodeMapHandle // NULL
    err = spinInterfaceGetTLNodeMap(hInterface, hNodeMapInterface)
    check(err, "Unable to retrieve interface nodemap.")

    // Print interface display name
    val interfaceDisplayName = nodeGetStringValue(hNodeMapInterface, "InterfaceDisplayName")

    println(s"Interface: $interfaceDisplayName")

    //
    // Retrieve list of cameras from the interface
    //
    // *** NOTES ***
    // Camera lists can be retrieved from an interface or the system object.
    // Camera lists retrieved from an interface, such as this one, only return
    // cameras attached on that specific interface whereas camera lists
    // retrieved from the system will return all cameras on all interfaces.
    //
    // *** LATER ***
    // Camera lists must be cleared manually. This must be done prior to
    // releasing the system and while the camera list is still in scope.
    //
    val hCameraList = new spinCameraList()

    // Create empty camera list
    err = spinCameraListCreateEmpty(hCameraList)
    check(err, "Unable to create camera list.")

    // Retrieve cameras
    err = spinInterfaceGetCameras(hInterface, hCameraList)
    check(err, "Unable to retrieve camera list.")

    // Retrieve number of cameras
    val numCameras = new SizeTPointer(1)
    err = spinCameraListGetSize(hCameraList, numCameras)
    check(err, "Unable to retrieve number of cameras.")

    //
    // Print info about detected cameras
    //
    if (numCameras.get > 0) {
      printDeviceVendorAndModel(hCameraList)
    } else {
      System.out.println("\tNo devices detected.\n\n")
    }

    //
    // Clear and destroy camera list before losing scope
    //
    // *** NOTES ***
    // Camera lists do not automatically clean themselves up. This must be done
    // manually. The same is true of interface lists.
    //
    err = spinCameraListClear(hCameraList)
    check(err, "Unable to clear camera list.")
    err = spinCameraListDestroy(hCameraList)
    check(err, "Unable to destroy camera list.")
  }

  /**
   * Example entry point; this function sets up the system and retrieves
   * interfaces for the example.
   */
  def main(args: Array[String]): Unit = {
    var errReturn = spinError.SPINNAKER_ERR_SUCCESS
    var err       = spinError.SPINNAKER_ERR_SUCCESS

    //
    // Retrieve singleton reference to system object
    //
    // *** NOTES ***
    // Everything originates with the system object. It is important to notice
    // that it has a singleton implementation, so it is impossible to have
    // multiple system objects at the same time.
    //
    // *** LATER ***
    // The system object should be cleared prior to program completion.  If not
    // released explicitly, it will be released automatically.
    //
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

    //
    // Retrieve list of interfaces from the system
    //
    // *** NOTES ***
    // Interface lists are retrieved from the system object.
    //
    // *** LATER ***
    // Interface lists must be cleared and destroyed manually. This must be
    // done prior to releasing the system and while the interface list is still
    // in scope.
    //
    val hInterfaceList = new spinInterfaceList()
    val numInterfaces  = new SizeTPointer(1)

    // Create empty interface list
    err = spinInterfaceListCreateEmpty(hInterfaceList)
    exitOnError(err, "Unable to create empty interface list")

    // Retrieve interfaces from system
    err = spinSystemGetInterfaces(hSystem, hInterfaceList)
    exitOnError(err, "Unable to retrieve interface list.")

    // Retrieve number of interfaces
    err = spinInterfaceListGetSize(hInterfaceList, numInterfaces)
    exitOnError(err, "Unable to retrieve number of interfaces.")

    println("Number of interfaces detected: " + numInterfaces.get + "\n")

    //
    // Retrieve list of cameras from the system
    //
    // *** NOTES ***
    // Camera lists can be retrieved from an interface or the system object.
    // Camera lists retrieved from the system, such as this one, return all
    // cameras available on the system.
    //
    // *** LATER ***
    // Camera lists must be cleared and destroyed manually. This must be done
    // prior to releasing the system and while the camera list is still in
    // scope.
    //
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

    if (numCameras.get > 0 && numInterfaces.get > 0) {
      System.out.println("\n*** QUERYING INTERFACES ***\n")
      // Run example on each interface
      // In order to run all interfaces in a loop, each interface needs to
      // retrieved using its index.
      for (i <- 0 until numInterfaces.get().toInt) {
        // Select interface
        val hInterface = new spinInterface

        err = spinInterfaceListGet(hInterfaceList, i, hInterface)
        if (printOnError(err, "Unable to retrieve interface from list.")) {
          errReturn = err
        } else {
          // Run example
          try {
            queryInterface(hInterface)
          } catch {
            case ex: SpinnakerSDKException =>
              println(ex.getMessage)
              errReturn = ex.error
          }

          // Release interface
          err = spinInterfaceRelease(hInterface)
          if (err != spinError.SPINNAKER_ERR_SUCCESS) errReturn = err
        }
      }
    } else {
      println("Not enough cameras/interfaces!")
    }

    //
    // Clear and destroy camera list before releasing system
    //
    // *** NOTES ***
    // Camera lists are not shared pointers and do not automatically clean
    // themselves up and break their own references. Therefore, this must be
    // done manually. The same is true of interface lists.
    //
    err = spinCameraListClear(hCameraList)
    exitOnError(err, "Unable to clear camera list.")

    err = spinCameraListDestroy(hCameraList)
    exitOnError(err, "Unable to destroy camera list.")

    //
    // Clear and destroy interface list before releasing system
    //
    // *** NOTES ***
    // Interface lists are not shared pointers and do not automatically clean
    // themselves up and break their own references. Therefore, this must be
    // done manually. The same is true of camera lists.
    //
    // Clear and destroy interface list before releasing system
    err = spinInterfaceListClear(hInterfaceList)
    exitOnError(err, "Unable to clear interface list.")

    err = spinInterfaceListDestroy(hInterfaceList)
    exitOnError(err, "Unable to destroy interface list.")

    //
    // Release system
    //
    // *** NOTES ***
    // The system should be released, but if it is not, it will do so itself.
    // It is often at the release of the system (whether manual or automatic)
    // that unbroken references and still registered events will throw an
    // exception.
    //
    err = spinSystemReleaseInstance(hSystem)
    exitOnError(err, "Unable to release system instance.")

    println("\nDone!\n")
  }
}
