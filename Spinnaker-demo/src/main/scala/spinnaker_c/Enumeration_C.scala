package spinnaker_c

import org.bytedeco.javacpp.SizeTPointer
import org.bytedeco.spinnaker.Spinnaker_C.*
import org.bytedeco.spinnaker.global.Spinnaker_C.*
import spinnaker_c.helpers.*

import scala.util.Using

/**
 *  Enumeration_C.c shows how to enumerate interfaces and cameras.
 *  Knowing this is mandatory for doing anything with the Spinnaker SDK, and
 *  is therefore the best place to start learning how to use the SDK.
 *
 *  This example introduces the preparation, use, and cleanup of the system
 *  object, interface and camera lists, interfaces, and cameras. It also
 *  touches on retrieving both nodes from nodemaps and information from
 *  nodes.
 *
 *  Once comfortable with enumeration, we suggest checking out either the
 *  Acquisition_C or NodeMapInfo_C examples. Acquisition_C demonstrates using a
 *  camera to acquire images while NodeMapInfo_C explores retrieving
 *  information from various node types.
 */
object Enumeration_C {

  /**
   * Example entry point; this function sets up the system and retrieves
   * interfaces for the example.
   */
  def main(args: Array[String]): Unit = {

    Using.Manager { use =>
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
      val hSystem = use(new spinSystem())
      exitOnError(spinSystemGetInstance(hSystem), "Unable to retrieve system instance.")
      try {

        // Print out current library version
        printLibraryVersion(hSystem)

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

        // Create empty interface list
        val hInterfaceList = use(new spinInterfaceList())
        exitOnError(spinInterfaceListCreateEmpty(hInterfaceList), "Unable to create empty interface list")

        // Retrieve interfaces from system
        val numInterfaces = use(new SizeTPointer(1))
        exitOnError(spinSystemGetInterfaces(hSystem, hInterfaceList), "Unable to retrieve interface list.")

        // Retrieve number of interfaces
        exitOnError(spinInterfaceListGetSize(hInterfaceList, numInterfaces), "Unable to retrieve number of interfaces.")

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
        val hCameraList = use(new spinCameraList())
        exitOnError(spinCameraListCreateEmpty(hCameraList), "Unable to create camera list.")

        try {
          exitOnError(spinSystemGetCameras(hSystem, hCameraList), "Unable to retrieve camera list.")

          // Retrieve number of cameras
          val numCameras = use(new SizeTPointer(1))
          exitOnError(spinCameraListGetSize(hCameraList, numCameras), "Unable to retrieve number of cameras.")
          println("Number of cameras detected: " + numCameras.get + "\n")

          if (numCameras.get > 0 && numInterfaces.get > 0) {
            System.out.println("\n*** QUERYING INTERFACES ***\n")

            // Run example on each interface
            // In order to run all interfaces in a loop, each interface needs to
            // retrieved using its index.
            for (i <- 0 until numInterfaces.get.toInt) do
              // Select interface
              val hInterface = use(new spinInterface())

              val error = printOnError(
                spinInterfaceListGet(hInterfaceList, i, hInterface),
                "Unable to retrieve interface from list."
              )
              if !error then
                try
                  // Run example
                  queryInterface(hInterface)
                catch
                  case ex: SpinnakerSDKException => println(ex.getMessage)

                // Release interface, no error check
                spinInterfaceRelease(hInterface)
          } else {
            println("Not enough cameras/interfaces!")
          }

        } finally
          //
          // Clear and destroy camera list before releasing system
          //
          // *** NOTES ***
          // Camera lists are not shared pointers and do not automatically clean
          // themselves up and break their own references. Therefore, this must be
          // done manually. The same is true of interface lists.
          //
          exitOnError(spinCameraListClear(hCameraList), "Unable to clear camera list.")
          exitOnError(spinCameraListDestroy(hCameraList), "Unable to destroy camera list.")

        //
        // Clear and destroy interface list before releasing system
        //
        // *** NOTES ***
        // Interface lists are not shared pointers and do not automatically clean
        // themselves up and break their own references. Therefore, this must be
        // done manually. The same is true of camera lists.
        //
        // Clear and destroy interface list before releasing system
        exitOnError(spinInterfaceListClear(hInterfaceList), "Unable to clear interface list.")
        exitOnError(spinInterfaceListDestroy(hInterfaceList), "Unable to destroy interface list.")

      } finally
        //
        // Release system
        //
        // *** NOTES ***
        // The system should be released, but if it is not, it will do so itself.
        // It is often at the release of the system (whether manual or automatic)
        // that unbroken references and still registered events will throw an
        // exception.
        //
        exitOnError(spinSystemReleaseInstance(hSystem), "Unable to release system instance.")
    }.get

    println("\nDone!\n")
  }

  /**
   * This function queries an interface for its cameras and then prints out
   * device information.
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("When a Spinnaker SDK operation fails.")
  def queryInterface(hInterface: spinInterface): Unit = Using.Manager { use =>

    //
    // Retrieve TL nodemap from interface
    //
    // *** NOTES ***
    // Each interface has a nodemap that can be retrieved in order to access
    // information about the interface itself, any devices connected, or
    // addressing information if applicable.
    //
    val hNodeMapInterface = use(new spinNodeMapHandle()) // NULL
    check(spinInterfaceGetTLNodeMap(hInterface, hNodeMapInterface), "Unable to retrieve interface nodemap.")

    //
    // Print interface display name, use `nodeGetStringValue` helper to get the node value
    //
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
    val hCameraList = use(new spinCameraList())

    // Create empty camera list
    check(spinCameraListCreateEmpty(hCameraList), "Unable to create camera list.")

    // Retrieve cameras
    check(spinInterfaceGetCameras(hInterface, hCameraList), "Unable to retrieve camera list.")

    // Retrieve number of cameras
    val numCameras = use(new SizeTPointer(1))
    check(spinCameraListGetSize(hCameraList, numCameras), "Unable to retrieve number of cameras.")

    //
    // Print info about detected cameras
    //
    if (numCameras.get > 0) {
      printDeviceVendorAndModel(hCameraList)
    } else {
      println("\tNo devices detected.\n\n")
    }

    //
    // Clear and destroy camera list before losing scope
    //
    // *** NOTES ***
    // Camera lists do not automatically clean themselves up. This must be done
    // manually. The same is true of interface lists.
    //
    check(spinCameraListClear(hCameraList), "Unable to clear camera list.")
    check(spinCameraListDestroy(hCameraList), "Unable to destroy camera list.")
  }.get

  /**
   * Print information about cameras on the list
   *
   * @param hCameraList list of cameras that was already allocated and retrieved
   */
  @throws[spinnaker_c.helpers.SpinnakerSDKException]("When a Spinnaker SDK operation fails.")
  def printDeviceVendorAndModel(hCameraList: spinCameraList): Unit = Using.Manager { use =>

    // Retrieve number of cameras
    val numCameras = use(new SizeTPointer(1))
    check(spinCameraListGetSize(hCameraList, numCameras), "Unable to retrieve number of cameras.")

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
      val hCam = use(new spinCamera())
      check(spinCameraListGet(hCameraList, i, hCam), "Unable to retrieve camera.")

      // Retrieve TL device nodemap; please see NodeMapInfo_C example for
      // additional comments on transport layer nodemaps.
      val hNodeMapTLDevice = use(new spinNodeMapHandle())
      check(spinCameraGetTLDeviceNodeMap(hCam, hNodeMapTLDevice), "Unable to retrieve TL device nodemap.")

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

      println(s"\tDevice $i / $deviceVendorName / $deviceModelName \n")

      //
      // Release camera before losing scope
      //
      // *** NOTES ***
      // Every handle that is created for a camera must be released before
      // the system is released or an exception will be thrown.
      //
      check(spinCameraRelease(hCam), "Unable to release camera.")
    }
  }.get
}
