package spinnaker_c

import org.bytedeco.javacpp._
import org.bytedeco.spinnaker.Spinnaker_C._
import org.bytedeco.spinnaker.global.Spinnaker_C._
import spinnaker_c.Utils._

/**
  * Code based on C version, EnumerationEvents_C4J.cpp, from Spinnaker SDK by FLIR.
  *
  * EnumerationEvents_C.cpp explores arrival and removal events on
  * interfaces and the system. It relies on information provided in the
  * Enumeration_C, Acquisition_C, and NodeMapInfo_C examples.
  *
  * It can also be helpful to familiarize yourself with the NodeMapCallback_C
  * example, as a callback can be thought of as a simpler, easier-to-use event.
  * Although events are more cumbersome, they are also much more flexible and
  * extensible.
  *
  * NOTE: Due to JavaCPP limitation handling of events / callbacks requires some care.
  * In particular, in JavaCPP only one event handler can be registered per event type,
  * see more info on that here [[https://groups.google.com/d/msg/javacpp-project/bxTAlvLKn0M/SUS0z4qMvyAJ]]
  */
object EnumerationEvents_C {

  // Define types of events processed by a single handler
  // [[https://groups.google.com/d/msg/javacpp-project/bxTAlvLKn0M/SUS0z4qMvyAJ]]
  val EVENT_TYPE_INTERFACE = 1
  val EVENT_TYPE_SYSTEM = 2

  /**
    * Helper trait to implement event handler, avoid code duplication.
    */
  trait OnDeviceEvent {
    protected def onInterface(deviceSerialNumber: Int, interfaceNum: Int): Unit

    protected def onSystem(deviceSerialNumber: Int, hSystem: spinSystem): Unit = {
      var err: _spinError = null

      // Retrieve count
      val hCameraList = new spinCameraList
      err = spinCameraListCreateEmpty(hCameraList)
      if (printOnError(err, "Unable to create camera list (system arrival).")) return

      err = spinSystemGetCameras(hSystem, hCameraList)
      if (printOnError(err, "Unable to retrieve cameras (system arrival).")) return

      val numCameras = new SizeTPointer(1)
      err = spinCameraListGetSize(hCameraList, numCameras)
      if (printOnError(err, "Unable to retrieve camera list size (system arrival).")) return

      // Print count
      println("System event handler:\n")
      println("\tThere " + (if (numCameras.get == 1) "is" else "are") + " "
        + numCameras.get + " " + (if (numCameras.get == 1) "device" else "devices") + " on the system.\n")

      // Clear and destroy camera list while still in scope
      err = spinCameraListClear(hCameraList)
      if (printOnError(err, "Unable to clear camera list.")) return

      err = spinCameraListDestroy(hCameraList)
      if (printOnError(err, "Unable to destroy camera list.")) return
    }

    protected def doCall(deviceSerialNumber: Int, pUserData: Pointer): Unit = {
      // Decode event type and event data. Call handler method for each type.
      val pp = new PointerPointer[Pointer](pUserData)
      new IntPointer(pp.get(0)).get match {
        case EVENT_TYPE_INTERFACE =>
          onInterface(deviceSerialNumber, new IntPointer(pp.get(1)).get())
        case EVENT_TYPE_SYSTEM =>
          onSystem(deviceSerialNumber, new spinSystem(pp.get(1)))
        case v =>
          throw new IllegalArgumentException("Invalid EVENT_FUNCTION_TYPE: " + v)
      }
    }
  }

  class OnDeviceArrival extends spinArrivalEventFunction with OnDeviceEvent {
    override def onInterface(deviceSerialNumber: Int, interfaceNum: Int): Unit = { // Cast user data to expected type
      // Print arrival information
      println("Interface event handler:")
      println("\tDevice " + deviceSerialNumber + " has arrived on interface " + interfaceNum + ".\n")
    }

    override def call(deviceSerialNumber: Int, pUserData: Pointer): Unit = doCall(deviceSerialNumber, pUserData)
  }

  class OnDeviceRemoval extends spinRemovalEventFunction with OnDeviceEvent {
    override def onInterface(deviceSerialNumber: Int, interfaceNum: Int): Unit = {
      // Print removal information
      println("Interface event handler:\n")
      println("\tDevice " + deviceSerialNumber + " was removed from interface " + interfaceNum + ".\n")
    }

    override def call(deviceSerialNumber: Int, pUserData: Pointer): Unit = doCall(deviceSerialNumber, pUserData)
  }

  private def systemData(data: Pointer): PointerPointer[Nothing] = eventData(EVENT_TYPE_SYSTEM, data)

  private def interfaceData(data: Pointer): PointerPointer[Nothing] = eventData(EVENT_TYPE_INTERFACE, data)

  /**
    * Encode event type and event data into single PointerPointer.
    *
    * @param eventType event type
    * @param data      dat a to be passed rto the event handler
    * @return
    */
  private def eventData(eventType: Int, data: Pointer): PointerPointer[Nothing] = {
    val ip = new IntPointer(1L)
    ip.put(eventType)

    val pp = new PointerPointer(2L)
    pp.put(0, ip)
    pp.put(1, data)
    pp
  }

  /**
    * Example entry point; this function sets up the example to act appropriately
    * upon arrival and removal events; please see Enumeration example for more
    * in-depth comments on preparing and cleaning up the system.
    */
  def main(args: Array[String]): Unit = {
    var err: _spinError = null

    // Retrieve singleton reference to system object
    val hSystem = new spinSystem()
    err = spinSystemGetInstance(hSystem)
    exitOnError(err, "Unable to retrieve system instance.")

    //        // Print out current library version
    //        spinLibraryVersion hLibraryVersion;
    //        spinSystemGetLibraryVersion(hSystem, &hLibraryVersion);
    //        printf("Spinnaker library version: %d.%d.%d.%d\n\n",
    //                hLibraryVersion.major,
    //                hLibraryVersion.minor,
    //                hLibraryVersion.type,
    //                hLibraryVersion.build);

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

    // Retrieve list of interfaces from the system
    // *** NOTES ***
    // MacOS interfaces are only registered if they are active.
    // For this example to have the desired outcome all devices must be connected
    // at the beginning and end of this example in order to register and deregister
    // an event handler on each respective interface.
    val hInterfaceList = new spinInterfaceList()
    val numInterfaces = new SizeTPointer(1)
    err = spinInterfaceListCreateEmpty(hInterfaceList)
    exitOnError(err, "Unable to create interface list.")

    err = spinSystemGetInterfaces(hSystem, hInterfaceList)
    exitOnError(err, "Unable to retrieve interface list.")

    err = spinInterfaceListGetSize(hInterfaceList, numInterfaces)
    exitOnError(err, "Unable to retrieve number of interfaces.")

    println("Number of interfaces detected: " + numInterfaces.get + "\n")
    println("\n*** CONFIGURE ENUMERATION EVENTS ***\n")

    // Create interface event for the system
    // The function for the system has been constructed to accept a system in
    // order to print the number of cameras on the system. Notice that there
    // are 3 types of events that can be created: arrival events, removal events,
    // and interface events, which are a combination of arrival and removal
    // events. Here, the an interface event is created, which requires both
    // an arrival event and a removal event object.
    // *** LATER ***
    // In Spinnaker C, every event that is created must be destroyed to avoid
    // memory leaks.
    val interfaceEventSystem = new spinInterfaceEvent()

    val onDeviceArrival = new OnDeviceArrival()
    val onDeviceRemoval = new OnDeviceRemoval()
    err = spinInterfaceEventCreate(interfaceEventSystem, onDeviceArrival, onDeviceRemoval, systemData(hSystem))
    exitOnError(err, "Unable to create interface event for system.")
    println("Interface event for system created...")

    // Register interface event for the system
    // Arrival, removal, and interface events can all be registered to
    // interfaces or the system. Do not think that interface events can only be
    // registered to an interface.
    // Arrival, removal, and interface events must all be unregistered manually.
    // This must be done prior to releasing the system and while they are still
    // in scope.
    err = spinSystemRegisterInterfaceEvent(hSystem, interfaceEventSystem)
    exitOnError(err, "Unable to register interface event on system.")
    println("Interface event registered to system...")

    // Prepare user data
    val hInterface = Array.fill(numInterfaces.get.toInt)(new spinInterface())

    // Create and register arrival and removal events to each interface
    // Separate arrival and event objects have been created for each interface.
    // This is for demonstration purposes as an interface event object (which is
    // simply a combination of an arrival and removal event object) is more
    // appropriate in this instance.
    // in scope. Also, every event that is created must be destroyed to avoid
    // memory leaks.
    val arrivalEvents = new Array[spinArrivalEvent](numInterfaces.get.toInt)
    val removalEvents = new Array[spinRemovalEvent](numInterfaces.get.toInt)

    for (i <- 0 until numInterfaces.get.toInt) {
      println("Setting up interface: " + i)

      // Initialize user data for selected interface
      val interfaceNum = new IntPointer(1L)
      interfaceNum.put(i)

      err = spinInterfaceListGet(hInterfaceList, i, hInterface(i))
      exitOnError(err, "Unable to retrieve interface" + i + ".")

      // Create arrival event for selected interface
      arrivalEvents(i) = new spinArrivalEvent()

      // We will use just the `interfaceNum` as user data, to simplify implementation
      err = spinArrivalEventCreate(arrivalEvents(i), onDeviceArrival, interfaceData(interfaceNum))
      exitOnError(err, "Unable to create arrival event for interface " + i + ".")

      // Create removal event for selected interface
      removalEvents(i) = new spinRemovalEvent()
      err = spinRemovalEventCreate(removalEvents(i), onDeviceRemoval, interfaceData(interfaceNum))
      exitOnError(err, "Unable to create removal event for interface " + i + ".")

      // Register arrival event to selected interface
      err = spinInterfaceRegisterArrivalEvent(hInterface(i), arrivalEvents(i))
      exitOnError(err, "Unable to register arrival event for interface " + i + ".")

      // Register removal event to selected interface
      err = spinInterfaceRegisterRemovalEvent(hInterface(i), removalEvents(i))
      exitOnError(err, "Unable to register removal event for interface " + i + ".")
    }

    println("Arrival and removal events created and registered to all interfaces...\n")

    // Wait for user to plug in and/or remove camera devices
    println("Ready! Remove/Plug in cameras to test or press Enter to exit...")
    System.in.read()

    //
    // Unregister arrival and removal events from each interface
    // *** NOTES ***
    // It is important to unregister all arrival, removal, and interface events
    // from all interfaces that they may be registered to.
    for (i <- 0 until numInterfaces.get.toInt) {
      err = spinInterfaceUnregisterArrivalEvent(hInterface(i), arrivalEvents(i))
      exitOnError(err, "Unable to unregister arrival event from interface " + i + ".")

      err = spinInterfaceUnregisterRemovalEvent(hInterface(i), removalEvents(i))
      exitOnError(err, "Unable to unregister removal event from interface " + i + ".")

      // Release interface
      err = spinInterfaceRelease(hInterface(i))
      exitOnError(err, "Unable to release interface " + i + ".")
    }
    println("Event handlers unregistered from interfaces...")

    // Destroy interface list
    err = spinInterfaceListDestroy(hInterfaceList)
    exitOnError(err, "Unable to destroy interface list.")

    // Destroy arrival and removal events and release interfaces
    // Events must be destroyed in order to avoid memory leaks.
    for (i <- 0 until numInterfaces.get.toInt) {
      err = spinArrivalEventDestroy(arrivalEvents(i))
      exitOnError(err, "Unable to destroy arrival event " + i + ".")

      err = spinRemovalEventDestroy(removalEvents(i))
      exitOnError(err, "Unable to destroy removal event " + i + ".")
    }

    println("Interface event handlers destroyed...")

    // Unregister system event from system object
    // registered to the system.
    err = spinSystemUnregisterInterfaceEvent(hSystem, interfaceEventSystem)
    exitOnError(err, "Unable to unregister interface event from system.")
    println("Event handlers unregistered from system...")

    // Destroy interface events
    err = spinInterfaceEventDestroy(interfaceEventSystem)
    exitOnError(err, "Unable to destroy interface event.")
    println("System event handler destroyed...")

    // Clear and destroy camera list before releasing system
    err = spinCameraListClear(hCameraList)
    exitOnError(err, "Unable to clear camera list.")
    err = spinCameraListDestroy(hCameraList)
    exitOnError(err, "Unable to destroy camera list.")

    // Release system
    err = spinSystemReleaseInstance(hSystem)
    exitOnError(err, "Unable to release system instance.")
    println("Done!")
  }

}
