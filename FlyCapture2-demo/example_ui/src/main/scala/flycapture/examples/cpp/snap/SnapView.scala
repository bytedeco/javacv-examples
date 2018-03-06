/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.image.ImageView
import scalafxml.core.macros.sfxml

/**
 * The main application window. This class only hook-ups FXML UI to the [[flycapture.examples.cpp.snap.SnapModel]].
 * Most functionality is implemented in the model.
 *
 * @author Jarek Sacha 
 */
@sfxml
class SnapView(private val selectCameraButton: Button,
               private val snapButton: Button,
               private val startButton: Button,
               private val stopButton: Button,
               private val saveImageButton: Button,
               private val settingsButton: Button,
               private val snapImageView: ImageView,
               private val flycaptureVersionLabel: Label,
               private val cameraInfoLabel: Label,
               private val model: SnapModel) {

  // TODO resize image when frame size is changing

  snapImageView.image <== model.snappedImage
  flycaptureVersionLabel.text <== model.fc2Version
  cameraInfoLabel.text <== model.cameraInfo

  // Disable `onSnap` if no camera is selected
  snapButton.disable <== !model.canSnap
  startButton.disable <== !model.canStart
  stopButton.disable <== !model.canStop
  saveImageButton.disable <== !model.canSave
  settingsButton.disable <== model.selectedCamera === None

  selectCameraButton.onAction = (_: ActionEvent) => model.selectCamera()
  snapButton.onAction = (_: ActionEvent) => model.onSnap()
  startButton.onAction = (_: ActionEvent) => model.onStartLiveCapture()
  stopButton.onAction = (_: ActionEvent) => model.onStopLiveCapture()
  saveImageButton.onAction = (_: ActionEvent) => model.onSaveImage()
  settingsButton.onAction = (_: ActionEvent) => model.onSettings()
}
