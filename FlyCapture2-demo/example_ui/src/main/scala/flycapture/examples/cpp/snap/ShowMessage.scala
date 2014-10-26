/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import org.controlsfx.dialog.Dialogs

import scalafx.stage.Window

/**
 * @author Jarek Sacha 
 */
trait ShowMessage {

  def parentWindow: Window

  def showException(title: String, message: String, t: Throwable): Unit = {
    t.printStackTrace()
    onFXAndWait {
      Dialogs.
        create().
        owner(if (parentWindow != null) parentWindow.delegate else null).
        title(title).
        masthead(message).
        showException(t)
    }
  }

}
