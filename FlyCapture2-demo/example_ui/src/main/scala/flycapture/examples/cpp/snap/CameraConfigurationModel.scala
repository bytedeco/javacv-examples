/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import java.util.concurrent.atomic.AtomicBoolean
import javafx.{concurrent => jfxc}

import flycapture.CheckMacro.check
import org.bytedeco.javacpp.FlyCapture2._

import scalafx.Includes._
import scalafx.beans.property.BooleanProperty
import scalafx.geometry.Pos
import scalafx.scene.control._
import scalafx.scene.layout.GridPane
import scalafx.util.Duration

/**
 * Model for camera configuration UI.
 *
 * @author Jarek Sacha 
 */
class CameraConfigurationModel(camera: CameraBase) {
  require(camera != null)

  class PropertyControls(val propType: Int, val name: String, val isUsingValueB: Boolean = false) {
    val slider = new Slider()
    val textField = new TextField {
      editable = false
      alignment = Pos.BaselineRight
    }
    val unitLabel = new Label()
    val autoCheckBox = new CheckBox()
    val onOffCheckBox = new CheckBox()
    val onePushButton = new Button("  ")
    val isUpdating = new AtomicBoolean(false)
  }

  val absoluteMode = BooleanProperty(value = true)

  private val properties = Seq(
    new PropertyControls(BRIGHTNESS, "Brightness"),
    new PropertyControls(AUTO_EXPOSURE, "Exposure"),
    new PropertyControls(SHARPNESS, "Sharpness"),
    new PropertyControls(HUE, "Hue"),
    new PropertyControls(SATURATION, "Saturation"),
    new PropertyControls(GAMMA, "Gamma"),
    new PropertyControls(IRIS, "Iris"),
    new PropertyControls(FOCUS, "Focus"),
    new PropertyControls(ZOOM, "Zoom"),
    new PropertyControls(PAN, "Pan"),
    new PropertyControls(TILT, "Tilt"),
    new PropertyControls(SHUTTER, "Shutter"),
    new PropertyControls(GAIN, "Gain"),
    new PropertyControls(FRAME_RATE, "Frame Rate")
    // W.B.(Red)
    // W.B.(Blue) using valueB
    // Power
    //    new PropertyControls(TEMPERATURE, "Temperature")
    //    new PropertyControls(TRIGGER_MODE, "TRIGGER_MODE"),
    //    new PropertyControls(TRIGGER_DELAY, "TRIGGER_DELAY"),
    //    new PropertyControls(WHITE_BALANCE, "WHITE_BALANCE"),
    //    new PropertyControls(TEMPERATURE, "TEMPERATURE")
  )

  var propertyGridPane: GridPane = _

  private val updateViewScheduledService = new UpdateViewScheduledService()


  def initialize(): Unit = {

    if (!camera.IsConnected()) check(camera.Connect())

    onFXAndWait {
      var row = 3
      properties.foreach { p =>
        val added = addPropertyControls(camera, p, row)
        if (added) row += 1
      }
    }

    updateViewScheduledService.restart()
  }

  private def addPropertyControls(cam: CameraBase, pc: PropertyControls, row: Int): Boolean = {
    val propertyInfo = new PropertyInfo(pc.propType)
    check(cam.GetPropertyInfo(propertyInfo))
    if (propertyInfo.present) {
      propertyGridPane.add(Label(pc.name), 0, row)
      if (propertyInfo.readOutSupported) {
        propertyGridPane.add(pc.slider, 1, row)
        propertyGridPane.add(pc.textField, 2, row)
        propertyGridPane.add(pc.unitLabel, 3, row)

        // Slider
        pc.slider.value.onChange { (_, oldValue, newValue) =>
          if (pc.isUpdating.compareAndSet(false, true)) {
            try {
              val property = new Property(pc.propType)
              check(cam.GetProperty(property))
              if (propertyInfo.absValSupported && absoluteMode()) {
                property.absControl(true)
                property.absValue(newValue.floatValue())
              } else {
                property.absControl(false)
                val intValue = newValue.intValue()
                if (pc.isUsingValueB) {
                  property.valueB(intValue)
                } else {
                  // Bug 19306
                  if (property.`type` == SHUTTER) {
                    property.valueB(intValue >> 12)
                  }
                  property.valueA(intValue)
                }
              }
              check(cam.SetProperty(property))
            } finally {
              pc.isUpdating.set(false)
            }
          }
        }

        // Auto check box
        if (propertyInfo.manualSupported && propertyInfo.autoSupported) {
          propertyGridPane.add(pc.autoCheckBox, 4, row)
          pc.autoCheckBox.onAction = handle {
            val property = new Property(pc.propType)
            check(cam.GetProperty(property))
            if (propertyInfo.absValSupported) property.absControl(absoluteMode())
            property.autoManualMode(pc.autoCheckBox.selected())
            property.onOff(pc.onOffCheckBox.selected())
            check(cam.SetProperty(property))
          }
        }

        // On/Off check box
        if (propertyInfo.onOffSupported()) {
          propertyGridPane.add(pc.onOffCheckBox, 5, row)
          pc.onOffCheckBox.onAction = handle {
            val property = new Property(pc.propType)
            check(cam.GetProperty(property))
            if (propertyInfo.absValSupported) property.absControl(absoluteMode())
            property.autoManualMode(pc.autoCheckBox.selected())
            property.onOff(pc.onOffCheckBox.selected())
            check(cam.SetProperty(property))
          }
        }

        // One Push button
        if (propertyInfo.onePushSupported()) {
          propertyGridPane.add(pc.onePushButton, 6, row)
          pc.onePushButton.onAction = handle {
            val property = new Property(pc.propType)
            check(cam.GetProperty(property))
            if (propertyInfo.absValSupported) property.absControl(absoluteMode())
            property.onePush(true)
            check(cam.SetProperty(property))
          }
        }
      }
      true
    } else {
      false
    }
  }

  def updatePropertyControl(cam: CameraBase, p: PropertyControls): Unit = {
    if (p.isUpdating.compareAndSet(false, true)) {
      try {
        val propertyInfo = new PropertyInfo(p.propType)
        check(cam.GetPropertyInfo(propertyInfo))
        if (propertyInfo.present) {
          if (propertyInfo.readOutSupported) {
            val property = new Property(p.propType)
            check(cam.GetProperty(property))
            if (absoluteMode() && propertyInfo.absValSupported) {
              p.slider.min = propertyInfo.absMin
              p.slider.max = propertyInfo.absMax
              val actualValue = math.min(math.max(property.absValue(), propertyInfo.absMin), propertyInfo.absMax)
              p.slider.value = actualValue
              p.textField.text = actualValue.toString
              p.unitLabel.text = propertyInfo.pUnitAbbr.getString
            } else {
              p.slider.min = propertyInfo.min
              p.slider.max = propertyInfo.max
              val value = if (p.propType == SHUTTER) {
                // Bug 19306
                property.valueA + (property.valueB << 12)
              } else {
                if (p.isUsingValueB) property.valueB else property.valueA
              }
              val actualValue = math.min(math.max(value, propertyInfo.min), propertyInfo.max)
              p.slider.value = actualValue
              p.textField.text = actualValue.toString
              p.unitLabel.text = ""
            }
            if (propertyInfo.autoSupported) p.autoCheckBox.selected = property.autoManualMode()
            if (propertyInfo.onOffSupported()) p.onOffCheckBox.selected = property.onOff()
          }
        }
      } finally {
        p.isUpdating.set(false)
      }
    }
  }

  class UpdateViewScheduledService() extends jfxc.ScheduledService[String]() {
    private var counter = 0
    setPeriod(Duration(100))

    def createTask(): jfxc.Task[String] = new jfxc.Task[String]() {
      override def call(): String = {
        onFXAndWait {
          properties.zipWithIndex.foreach { case (p, i) => updatePropertyControl(camera, p)}
        }
        counter += 1
        counter.toString
      }

      override def failed() = {
        super.failed()
        getException.printStackTrace()
      }
    }
  }

}
