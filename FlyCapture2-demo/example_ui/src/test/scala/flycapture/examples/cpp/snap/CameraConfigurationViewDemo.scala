/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import flycapture.CheckMacro.check
import grizzled.slf4j.Logger
import org.apache.log4j.Level
import org.bytedeco.flycapture.FlyCapture2._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.Scene

/**
  * @author Jarek Sacha
  */
object CameraConfigurationViewDemo extends JFXApp {

  lazy val title = "Fly Capture CameraConfiguration Demo"
  initializeLogging(Level.INFO)

  private val logger = Logger(this.getClass)
  setupUncaughtExceptionHandling(logger, title)

  private val busMgr = new BusManager()
  private val numCameras = Array[Int](0)
  check(busMgr.GetNumOfCameras(numCameras))
  println("Number of cameras detected: " + numCameras(0))

  private val guid = new PGRGuid()
  check(busMgr.GetCameraFromIndex(0, guid))

  // Connect to a camera
  private val cam = new Camera()
  check(cam.Connect(guid))

  try {
    val cameraConfiguration = new CameraConfiguration(cam, null)

    // Create UI
    stage = new PrimaryStage() {
      title = CameraSelectionViewDemo.title
      scene = new Scene(cameraConfiguration.view)
    }
  } catch {
    case t: Throwable =>
      logger.error("Unexpected error. Application will terminate.", t)
      showException(null, title, "Unexpected error. Application will terminate.", t)

      Platform.exit()
  }

  override def stopApp(): Unit = {
    super.stopApp()

    cam.Disconnect()
  }
}
