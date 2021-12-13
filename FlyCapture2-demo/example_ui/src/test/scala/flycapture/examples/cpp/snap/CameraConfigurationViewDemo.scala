/*
 * Copyright (c) 2011-2021 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import com.typesafe.scalalogging.Logger
import flycapture.CheckMacro.check
import org.apache.log4j.Level
import org.bytedeco.flycapture.FlyCapture2._
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene

/**
 * @author Jarek Sacha
 */
object CameraConfigurationViewDemo extends JFXApp3 {

  private val appName = "Fly Capture CameraConfiguration Demo"
  private val logger  = Logger(this.getClass)

  private var cameraOption: Option[Camera] = None

  initializeLogging(Level.INFO)
  setupUncaughtExceptionHandling(logger, appName)

  override def start(): Unit = {
    try {
      val busMgr     = new BusManager()
      val numCameras = Array[Int](0)
      check(busMgr.GetNumOfCameras(numCameras))
      logger.info("Number of cameras detected: " + numCameras(0))

      val guid = new PGRGuid()
      check(busMgr.GetCameraFromIndex(0, guid))

      // Connect to a camera
      cameraOption = Option(new Camera())
      cameraOption.foreach { camera =>
        check(camera.Connect(guid))

        val cameraConfiguration = new CameraConfiguration(camera, null)

        // Create UI
        stage = new PrimaryStage() {
          title = appName
          scene = new Scene(cameraConfiguration.view)
        }
      }
    } catch {
      case t: Throwable =>
        logger.error("Unexpected error. Application will terminate.", t)
        showException(null, appName, "Unexpected error. Application will terminate.", t)

        Platform.exit()
    }
  }

  override def stopApp(): Unit = {
    super.stopApp()
    cameraOption.foreach(_.Disconnect())
  }
}
