/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import org.bytedeco.javacpp.FlyCapture2.CameraBase

import scala.reflect.runtime.universe.typeOf
import scalafx.scene.Parent
import scalafx.stage.Stage

/**
 * Create camera configuration UI.
 *
 * @author Jarek Sacha 
 */
class CameraConfiguration(camera: CameraBase, parent:Stage) {

  require(camera != null)

  val model: CameraConfigurationModel = new CameraConfigurationModel(camera, parent)

  val view: Parent = onFXAndWait {
    createFXMLView(model, typeOf[CameraConfigurationModel], "CameraConfigurationView.fxml")
  }

  onFXAndWait {
    // This model creates layout based on camera properties, so do it early and on FX thread.
    model.initialize()
  }
}
