/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import java.util.concurrent.atomic.AtomicBoolean

import flycapture.CheckMacro.check
import flycapture.examples.cpp.FC2Exception
import javafx.{concurrent => jfxc}
import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._
import org.bytedeco.javacpp.IntPointer
import scalafx.Includes._
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.geometry.Pos
import scalafx.scene.control._
import scalafx.scene.layout.GridPane
import scalafx.stage.Stage
import scalafx.util.Duration


object CameraConfigurationModel {

  private class PropertyControls(val propType: Int, val name: String, val isUsingValueB: Boolean = false) {
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

  object TestPattern {
    val Pattern1 = new TestPattern(1)
    val Pattern2 = new TestPattern(2)
    val None = new TestPattern(0)

    val values = Seq(Pattern1, Pattern2, None)
  }

  sealed class TestPattern(val code: Int)

}

/**
 * Model for camera configuration UI.
 *
 * @author Jarek Sacha 
 */
class CameraConfigurationModel(camera: CameraBase, parent: Stage) extends ShowMessage {


  import flycapture.examples.cpp.snap.CameraConfigurationModel._

  require(camera != null)

  val title = "Camera Configuration"

  val absoluteMode = BooleanProperty(value = true)
  val testPattern = new ObjectProperty[TestPattern]()
  var propertyGridPane: GridPane = _

  val testPatternReg: Int = 0x104C


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

  private val updateViewScheduledService = new UpdateViewScheduledService()

  testPattern.onChange { (_, _, _) =>

  }

  override def parentWindow: Stage = parent

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
        pc.slider.value.onChange { (_, _, newValue) =>
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

  private def updatePropertyControl(cam: CameraBase, p: PropertyControls): Unit = {
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

  def readTestPatternRegister(): TestPattern = {
    val valuePtr = new IntPointer(1L)
    check(camera.ReadRegister(testPatternReg, valuePtr))
    val v = valuePtr.get & 0x00

    TestPattern.values.find(_.code == v).getOrElse(throw new Exception("Unrecognized test pattern code: " + v))
  }

  def writeTestPatternRegister(newTestPattern: TestPattern): Unit = {


    val pattern = try {
      readTestPatternRegister()
    } catch {
      case ex: Exception => showException(title, "Error reading test pattern register.", ex)
        return
    }

    val newPatternRegisterValue = newTestPattern match {
      case TestPattern.None => pattern.code & 0x00
      case TestPattern.Pattern1 => pattern.code | (0x1 << 0)
      case TestPattern.Pattern2 => pattern.code | (0x1 << 1)
      case t => throw new Exception("Unsupported test pattern: " + t)
    }

    try {
      check(camera.WriteRegister(testPatternReg, newPatternRegisterValue))
    } catch {
      case ex: FC2Exception => showException(title, "Error writing test pattern register.", ex)
    }
  }

  private class UpdateViewScheduledService() extends jfxc.ScheduledService[Unit]() {
    setPeriod(Duration(100))

    def createTask(): jfxc.Task[Unit] = new jfxc.Task[Unit]() {
      override def call(): Unit = {
        onFXAndWait {
          properties.zipWithIndex.foreach { case (p, _) => updatePropertyControl(camera, p) }
        }
      }

      override def failed(): Unit = {
        super.failed()
        getException.printStackTrace()
      }
    }
  }

}
