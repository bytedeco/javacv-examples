package pylon.samples

import org.bytedeco.javacpp.Pylon4.{CTlFactory, DeviceInfoList}
import org.bytedeco.javacpp.{GenICam2, Pylon4}

/**
  * Prints camera model and other general information about connected cameras.
  */
object PrintCameraInformation extends App {

  // Call PylonInitialize  to ensure the pylon runtime system
  // is initialized during the lifetime of this object.
  println("PylonInitialize()")
  Pylon4.PylonInitialize()

  try {

    // Get the transport layer factory.
    val tlFactory = CTlFactory.GetInstance()

    // Get all attached devices and exit application if no device is found.
    val devices = new DeviceInfoList()
    if (tlFactory.EnumerateDevices(devices) == 0) {
      throw new RuntimeException("No camera present.")
    }

    val n = devices.size()
    println(s"Detected ${devices.size()} camera(s).")

    for (i <- 0 until n) {
      println(s"Camera ${i + 1}")
      val deviceInfo = devices.at(i)
      println("  ModelName      : " + asString(deviceInfo.GetModelName()))
      println("  SerialNumber   : " + asString(deviceInfo.GetSerialNumber()))
      println("  DeviceVersion  : " + asString(deviceInfo.GetDeviceVersion()))
      println("  DeviceFactory  : " + asString(deviceInfo.GetDeviceFactory()))
      println("  UserDefinedName: " + asString(deviceInfo.GetUserDefinedName()))
      println("  FriendlyName   : " + asString(deviceInfo.GetFriendlyName()))
      println("  FullName       : " + asString(deviceInfo.GetFullName()))
      println("  VendorName     : " + asString(deviceInfo.GetVendorName()))
    }

  } finally {
    println("PylonTerminate()")
    Pylon4.PylonTerminate()
  }

  def asString(gcs: GenICam2.gcstring): String = {
    gcs.c_str().getString
  }
}
