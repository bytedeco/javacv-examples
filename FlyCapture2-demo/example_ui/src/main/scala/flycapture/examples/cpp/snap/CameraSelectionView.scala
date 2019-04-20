/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import javafx.beans.{binding => jfxbb}
import scalafx.Includes._
import scalafx.scene.control._
import scalafxml.core.macros.sfxml

/**
 * Camera selection view. Links FXML to UI model.
 *
 * @author Jarek Sacha 
 */
@sfxml
class CameraSelectionView(private val fc2VersionLabel: Label,
                          private val camerasDetectedLabel: Label,
                          private val cameraListView: ListView[CameraID],
                          private val serialNumberLabel: Label,
                          private val modelLabel: Label,
                          private val sensorLabel: Label,
                          private val resolutionLabel: Label,
                          private val model: CameraSelectionModel) {

  fc2VersionLabel.text <== model.fc2Version
  cameraListView.items = model.cameraListViewItems
  camerasDetectedLabel.text <== when(model.numberOfDetectedCameras < 1) choose
    "(no cameras detected)" otherwise (
    when(model.numberOfDetectedCameras === 1) choose
      jfxbb.Bindings.format("(%s camera detected)", model.numberOfDetectedCameras.delegate) otherwise
      jfxbb.Bindings.format("(%s cameras detected)", model.numberOfDetectedCameras.delegate)
    )
  model.cameraItemsSelectionModel <== cameraListView.selectionModel
  cameraListView.selectionModel().selectionMode = SelectionMode.Single

  serialNumberLabel.text <== model.serialNumber
  modelLabel.text <== model.cameraModel
  sensorLabel.text <== model.sensor
  resolutionLabel.text <== model.resolution
}
