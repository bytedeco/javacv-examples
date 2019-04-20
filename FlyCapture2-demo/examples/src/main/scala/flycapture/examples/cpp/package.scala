/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */
package flycapture.examples

import flycapture.CheckMacro.check
import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._

/**
 * Helper functions for C++ API examples.
 */
package object cpp {

  /**
   * Query and print version of FlyCapture SDK.
   */
  def printBuildInfo() {
    val version = new FC2Version()
    Utilities.GetLibraryVersion(version)

    println(s"FlyCapture2 library version: ${version.major}.${version.minor}.${version.`type`}.${version.build}")
  }

  def printCameraInfo(camInfo: CameraInfo) {
    println("\n*** CAMERA INFORMATION ***\n" +
      s"Serial number       - ${camInfo.serialNumber} \n" +
      s"Camera model        - ${camInfo.modelName.getString}\n" +
      s"Camera vendor       - ${camInfo.vendorName.getString}\n" +
      s"Sensor              - ${camInfo.sensorInfo.getString}\n" +
      s"Resolution          - ${camInfo.sensorResolution.getString}\n" +
      s"Firmware version    - ${camInfo.firmwareVersion.getString}\n" +
      s"Firmware build time - ${camInfo.firmwareBuildTime.getString}\n")
  }

  def printPropertyInfo(cam: Camera): Unit = {
    val props = List(
      BRIGHTNESS -> "BRIGHTNESS",
      AUTO_EXPOSURE -> "AUTO_EXPOSURE",
      SHARPNESS -> "SHARPNESS",
      WHITE_BALANCE -> "WHITE_BALANCE",
      HUE -> "HUE",
      GAMMA -> "GAMMA",
      IRIS -> "IRIS",
      FOCUS -> "FOCUS",
      ZOOM -> "ZOOM",
      PAN -> "PAN",
      TILT -> "TILT",
      SHUTTER -> "SHUTTER",
      GAIN -> "GAIN",
      TRIGGER_MODE -> "TRIGGER_MODE",
      TRIGGER_DELAY -> "TRIGGER_DELAY",
      FRAME_RATE -> "FRAME_RATE",
      TEMPERATURE -> "TEMPERATURE"
    )

    props.foreach { case (t, n) => printPropertyInfo(cam, t, n)}
  }

  def printPropertyInfo(cam: Camera, propType: Int, propName: String): Unit = {
    val propertyInfo = new PropertyInfo(propType)
    check(cam.GetPropertyInfo(propertyInfo))
    println("Property info " + propName + " [present : " + propertyInfo.present + "]")
    if (propertyInfo.present) {
      println("  autoSupported    : " + propertyInfo.autoSupported)
      println("  manualSupported  : " + propertyInfo.manualSupported)
      println("  onOffSupported   : " + propertyInfo.onOffSupported)
      println("  onePushSupported : " + propertyInfo.onePushSupported)
      println("  absValSupported  : " + propertyInfo.absValSupported)
      println("  readOutSupported : " + propertyInfo.readOutSupported)
      if (propertyInfo.readOutSupported) {
        println("  min              : " + propertyInfo.min)
        println("  max              : " + propertyInfo.max)
        println("  absMin           : " + propertyInfo.absMin)
        println("  absMax           : " + propertyInfo.absMax)
        println("  pUnits           : " + propertyInfo.pUnits.getString)
        println("  pUnitAbbr        : " + propertyInfo.pUnitAbbr.getString)
        val property = new Property(propType)
        check(cam.GetProperty(property))
        println("  property value")
        println("    absControl     : " + property.absControl)
        println("    absValue       : " + property.absValue)
        println("    autoManualMode : " + property.autoManualMode)
        println("    onePush        : " + property.onePush)
        println("    onOff          : " + property.onOff)
        println("    present        : " + property.present)
        println("    type           : " + property.`type`)
        println("    valueA         : " + property.valueA)
        println("    valueB         : " + property.valueB)
      }
    }
  }
}
