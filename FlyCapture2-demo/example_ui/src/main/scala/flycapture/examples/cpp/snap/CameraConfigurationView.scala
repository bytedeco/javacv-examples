/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import flycapture.examples.cpp.snap.CameraConfigurationModel.TestPattern
import scalafx.Includes._
import scalafx.scene.control.{CheckBox, RadioButton, ToggleGroup}
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
                              private val displayTestPattern1RadioButton: RadioButton,
                              private val displayTestPattern2RadioButton: RadioButton,
                              private val displayTestPatternNoneRadioButton: RadioButton,
                              private val displayTestPatternGroup: ToggleGroup,
                              private val model: CameraConfigurationModel) {

  private val pattern1ID = displayTestPattern1RadioButton.id
  private val pattern2ID = displayTestPattern2RadioButton.id
  private val patternNoneID = displayTestPatternNoneRadioButton.id

  require(propertyGridPane != null)
  require(model != null)

  model.absoluteMode <==> absoluteModeCheckBox.selected
  model.propertyGridPane = propertyGridPane

  // Initialize test pattern toggle
  try {
    model.readTestPatternRegister() match {
      case TestPattern.Pattern1 => displayTestPattern1RadioButton.selected = true
      case TestPattern.Pattern2 => displayTestPattern2RadioButton.selected = true
      case TestPattern.None => displayTestPatternNoneRadioButton.selected = true
      case t => throw new Exception("Unsupported test pattern: " + t)
    }
  } catch {
    case ex: Exception =>
      ex.printStackTrace()
  }

  displayTestPatternGroup.selectedToggle.onChange { (_, _, newToggle) =>
    if (displayTestPatternGroup.selectedToggle() != null) {
      newToggle.getProperties
      val id = newToggle.asInstanceOf[javafx.scene.control.RadioButton].id
      id match {
        case `pattern1ID` => model.writeTestPatternRegister(TestPattern.Pattern1)
        case `pattern2ID` => model.writeTestPatternRegister(TestPattern.Pattern2)
        case `patternNoneID` => model.writeTestPatternRegister(TestPattern.None)
        case _ => throw new IllegalStateException("Unrecognized toggle with ID: " + id)
      }
    }
  }

}
