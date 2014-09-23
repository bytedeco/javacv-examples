/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp

import java.io.IOException
import java.util.concurrent

import grizzled.slf4j.Logger
import org.apache.log4j.{BasicConfigurator, Level}
import org.controlsfx.dialog.Dialogs

import scala.reflect.runtime.universe._
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.scene.Parent
import scalafxml.core.{DependenciesByType, FXMLView}

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
      new Thread.UncaughtExceptionHandler {
        override def uncaughtException(t: Thread, e: Throwable): Unit = {
          logger.error("Default handler caught exception: " + e.getMessage, e)
          Dialogs.create().title(title).masthead("Unhandled exception.").showException(e)
        }
      }
    )

    // start is called on the FX Application Thread,
    // so Thread.currentThread() is the FX application thread:
    Thread.currentThread().setUncaughtExceptionHandler(
      new Thread.UncaughtExceptionHandler {
        override def uncaughtException(t: Thread, e: Throwable): Unit = {
          logger.error("FX handler caught exception: " + e.getMessage, e)
          e.printStackTrace()
          onFX {
            Dialogs.create().title(title).masthead("Unhandled exception.").showException(e)
          }
        }
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
    // TODO: Move to FXUtils
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
    // TODO: Move to FXUtils
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
    // TODO: Move to FXUtils
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


}
