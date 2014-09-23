/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import flycapture.CheckMacro.check
import grizzled.slf4j.Logger
import org.apache.log4j.Level
import org.bytedeco.javacpp.FlyCapture2.{BusManager, Camera, PGRGuid}
import org.controlsfx.dialog.Dialogs

import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.Scene

/**
 * @author Jarek Sacha 
 */
object CameraConfigurationViewDemo extends JFXApp {

  private val logger = Logger(this.getClass)
  initializeLogging(Level.INFO)
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


  lazy val title = "Fly Capture CameraConfiguration Demo"

  try {
    val cameraConfiguration = new CameraConfiguration(cam)

    // Create UI
    stage = new PrimaryStage() {
      title = CameraSelectionViewDemo.title
      scene = new Scene(cameraConfiguration.view)
    }
  } catch {
    case t: Throwable =>
      logger.error("Unexpected error. Application will terminate.", t)
      Dialogs.
        create().
        owner(null).
        title(title).
        masthead("Unexpected error. Application will terminate.").
        showException(t)

      Platform.exit()
  }
  override def stopApp() = {
    super.stopApp()

    cam.Disconnect()
  }
}
