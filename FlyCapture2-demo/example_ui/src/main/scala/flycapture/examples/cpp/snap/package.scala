/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp

import java.io.{IOException, PrintWriter, StringWriter}
import java.util.concurrent

import grizzled.slf4j.Logger
import org.apache.log4j.{BasicConfigurator, Level}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.Parent
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, Label, TextArea}
import scalafx.scene.layout.{GridPane, Priority}
import scalafx.stage.Window
import scalafxml.core.{DependenciesByType, FXMLView}

import scala.reflect.runtime.universe._

/**
 * @author Jarek Sacha 
 */
package object snap {

  def initializeLogging(level: Level): Unit = {
    val root = org.apache.log4j.Logger.getRootLogger
    val appenders = root.getAllAppenders
    // Configure default appenders if non created yet
    if (appenders == null || !appenders.hasMoreElements) {
      BasicConfigurator.configure()
    }
    org.apache.log4j.Logger.getRootLogger.setLevel(level)
  }


  def setupUncaughtExceptionHandling(logger: Logger, title: String): Unit = {
    Thread.setDefaultUncaughtExceptionHandler(
      (_: Thread, e: Throwable) => {
        logger.error("Default handler caught exception: " + e.getMessage, e)
        showException(null, title, "Unhandled exception.", e)
      }
    )

    // start is called on the FX Application Thread,
    // so Thread.currentThread() is the FX application thread:
    Thread.currentThread().setUncaughtExceptionHandler(
      (_: Thread, e: Throwable) => {
        logger.error("FX handler caught exception: " + e.getMessage, e)
        e.printStackTrace()
        showException(null, title, "Unhandled FX exception.", e)
      }
    )
  }

  /**
   * Creates FXMLView using provided FXML file (`fxmlFilePath`).
   * @param model model for the created view.
   * @param fxmlFilePath location of FXML file, in relative the in relation to `model`
   * @return
   */
  def createFXMLView(model: Object, modelType: Type, fxmlFilePath: String): Parent = {
    // Load main view
    val resource = getClass.getResource(fxmlFilePath)
    if (resource == null) {
      throw new IOException("Cannot load resource: '" + fxmlFilePath + "'")
    }

    FXMLView(resource, new DependenciesByType(Map(modelType -> model)))
  }

  /**
   * Run operation `op` on FX application thread. Return without waiting for the operation to complete.
   *
   * @param op operation to be performed.
   */
  def onFX[R](op: => R): Unit = {
    if (Platform.isFxApplicationThread) {
      op
    } else {
      Platform.runLater {
        op
      }
    }
  }

  /**
   * Run operation `op` on FX application thread and wait for completion.
   * If the current thread is the FX application, the operation will be run on it.
   *
   * @param op operation to be performed.
   */
  def onFXAndWait[R](op: => R): R = {
    if (Platform.isFxApplicationThread) {
      op
    } else {

      val callable = new concurrent.Callable[R] {
        override def call(): R = op
      }
      // TODO: deal with exceptions
      //      try {
      val future = new concurrent.FutureTask(callable)
      Platform.runLater(future)
      future.get()
      //      }
    }
  }

  /**
   * Run operation `op` off FX application thread and wait for completion.
   * If the current thread is not the FX application, the operation will be run on it (no new thread will ne created).
   *
   * @param op operation to be performed.
   */
  def offFXAndWait[R](op: => R): R = {
    if (!Platform.isFxApplicationThread) {
      op
    } else {
      val callable = new concurrent.Callable[R] {
        override def call(): R = op
      }
      // TODO: deal with exceptions
      //      try {
      val future = new concurrent.FutureTask(callable)
      new Thread(future).start()
      future.get()
      //      }
    }
  }

  /**
   * Show exception dialog.
   */
  def showException(owner: Window, dialogTitle: String, header: String, ex: Throwable): Unit = {
    // Create expandable Exception.
    val exceptionText = {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      ex.printStackTrace(pw)
      sw.toString
    }

    val label = new Label("The exception stacktrace was:")
    val textArea = new TextArea {
      text = exceptionText
      editable = false
      wrapText = true
      maxWidth = Double.MaxValue
      maxHeight = Double.MaxValue
      vgrow = Priority.Always
      hgrow = Priority.Always
    }

    val expContent = new GridPane {
      maxWidth = Double.MaxValue
      add(label, 0, 0)
      add(textArea, 0, 1)
    }

    onFXAndWait {
      new Alert(AlertType.Error) {
        initOwner(owner)
        title = dialogTitle
        headerText = header
        contentText = Option(ex.getMessage).getOrElse(ex.getClass.toString)
        // Set expandable Exception into the dialog pane.
        dialogPane().expandableContent = expContent
      }.showAndWait()
    }
  }

}
