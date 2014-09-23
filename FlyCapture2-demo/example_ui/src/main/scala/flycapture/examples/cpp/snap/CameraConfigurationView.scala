/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import scalafx.scene.control.CheckBox
import scalafx.scene.layout.GridPane
import scalafxml.core.macros.sfxml

/**
 * Camera configuration view. Links FXML to UI model.
 *
 * @author Jarek Sacha 
 */
@sfxml
class CameraConfigurationView(private val absoluteModeCheckBox: CheckBox,
                              private val propertyGridPane: GridPane,
                              private val model: CameraConfigurationModel) {

  require(propertyGridPane != null)
  require(model != null)

  model.absoluteMode <==> absoluteModeCheckBox.selected
  model.propertyGridPane = propertyGridPane
}
