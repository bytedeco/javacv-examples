/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import flycapture.CheckMacro.check
import grizzled.slf4j.Logger
import javafx.scene.control.MultipleSelectionModel
import org.bytedeco.flycapture.FlyCapture2._
import scalafx.Includes._
import scalafx.beans.property.{IntegerProperty, ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.subscriptions.Subscription

/**
  * Model for camera selection UI.
  *
  * @author Jarek Sacha
  */
class CameraSelectionModel(busManager: BusManager) {

  // TODO automatically update list of cameras when cameras are connected/disconnected

  val fc2Version = StringProperty("?.?.?.?")
  val numberOfDetectedCameras = IntegerProperty(0)
  val serialNumber = StringProperty("?")
  val cameraModel = StringProperty("?")
  val sensor = StringProperty("?")
  val resolution = StringProperty("?")
  val cameraListViewItems = new ObservableBuffer[CameraID]()
  val cameraItemsSelectionModel = new ObjectProperty[MultipleSelectionModel[CameraID]]()

  private val logger = Logger(this.getClass)
  private var subscription: Option[Subscription] = None

  cameraItemsSelectionModel.onChange { (_, _, newSelectionModel) =>
    subscription.foreach(_.cancel())
    subscription = if (newSelectionModel != null) {
      Some(newSelectionModel.selectedItem.onChange { (_, _, newCamera) =>
        serialNumber() = if (newCamera != null) newCamera.cameraInfo.serialNumber().toString else "?"
        cameraModel() = if (newCamera != null) newCamera.cameraInfo.modelName().getString else "?"
        sensor() = if (newCamera != null) newCamera.cameraInfo.sensorInfo().getString else "?"
        resolution() = if (newCamera != null) newCamera.cameraInfo.sensorResolution().getString else "?"
      })
    } else {
      None
    }
  }


  def initialize(): Unit = {
    logger.trace("Initializing FlyCapture camera connections")

    // Get library version
    val version = new FC2Version()
    Utilities.GetLibraryVersion(version)
    fc2Version() = s"${version.major}.${version.minor}.${version.`type`}.${version.build}"
    logger.trace(s"FlyCapture2 library version: ${fc2Version()}")


    // Query connected cameras
    // TODO: monitor connected/disconnected camera events and update camera list appropriately
    val numCameras = Array[Int](0)
    check(busManager.GetNumOfCameras(numCameras))
    logger.trace("Number of cameras detected: " + numCameras(0))

    numberOfDetectedCameras() = numCameras(0)

    for (i <- 0 until numCameras(0)) {
      val guid = new PGRGuid()
      check(busManager.GetCameraFromIndex(i, guid))

      // Connect to a camera
      val cam = new Camera()
      check(cam.Connect(guid))

      // Get the camera information
      val camInfo = new CameraInfo()
      check(cam.GetCameraInfo(camInfo))
      //      printCameraInfo(camInfo)

      cameraListViewItems += CameraID(guid, camInfo)

      // Disconnect the camera
      check(cam.Disconnect())
    }

    if (numCameras(0) > 0) cameraItemsSelectionModel().selectFirst()
  }

  def selectedItem: CameraID = cameraItemsSelectionModel().selectedItem()

}
