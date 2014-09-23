/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import grizzled.slf4j.Logger
import org.apache.log4j.Level
import org.bytedeco.javacpp.FlyCapture2.BusManager
import org.controlsfx.dialog.Dialogs

import scala.reflect.runtime.universe.typeOf
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.concurrent.Task
import scalafx.scene.Scene

/**
 * @author Jarek Sacha 
 */
object CameraSelectionViewDemo extends JFXApp {

  lazy val title = "Fly Capture CameraSelectionView Demo"
  val model = new CameraSelectionModel(new BusManager())
  private val logger = Logger(this.getClass)

  initializeLogging(Level.INFO)
  setupUncaughtExceptionHandling(logger, title)

  try {
    val root = createFXMLView(model, typeOf[CameraSelectionModel], "CameraSelectionView.fxml")

    // Create UI
    stage = new PrimaryStage() {
      title = CameraSelectionViewDemo.title
      scene = new Scene(root)
    }

    // Initialize camera connections
    // Use worker thread for non-UI operations
    // TODO Handle possible exceptions from Task
    new Thread(Task {model.initialize()}).start()
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

}
