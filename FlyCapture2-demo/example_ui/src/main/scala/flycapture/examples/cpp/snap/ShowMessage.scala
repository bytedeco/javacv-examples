/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import scalafx.stage.Window

/**
 * @author Jarek Sacha 
 */
trait ShowMessage {

  def parentWindow: Window

  def showException(title: String, message: String, t: Throwable): Unit = {
    t.printStackTrace()
    flycapture.examples.cpp.snap.showException(parentWindow, title, message, t)
  }

}
