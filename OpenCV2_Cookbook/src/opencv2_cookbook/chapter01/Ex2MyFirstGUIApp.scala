/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter01

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.awt.Cursor._
import javax.swing.ImageIcon
import scala.swing.Dialog.Message.Error
import scala.swing.FileChooser.Result.Approve
import scala.swing._


/** The last section in the chapter 1 of the Cookbook demonstrates how to create a simple GUI application.
  *
  * The Cookbook is using Qt GUI Toolkit. This example is using Scala Swing to create an similar application.
  *
  * The application has two buttons on the left "Open Image" and "Process".
  * The opened image is displayed in the middle.
  * When "Process" button is pressed the image is flipped upside down and its red and blue channels are swapped.
  *
  * Unlike most of other examples in this module, this example is done the Scala way,
  * without regard to for direct porting to Java. A Java equivalent is in [[opencv2_cookbook.chapter01.Ex2MyFirstGUIAppJava]].
  */
object Ex2MyFirstGUIApp extends SimpleSwingApplication {

    private lazy val fileChooser = new FileChooser


    def top: Frame = new MainFrame {
        title = "My First GUI Scala App"

        // Variable for holding loaded image
        var image: Option[IplImage] = None


        //
        // Define actions
        //

        // Action performed when "Open Image" button is pressed
        val openImageAction = Action("Open Image") {
            cursor = getPredefinedCursor(WAIT_CURSOR)
            try {
                // Load image and update display. If new image was not loaded do nothing.
                openImage() match {
                    case Some(x) =>
                        image = Some(x)
                        imageView.icon = new ImageIcon(x.getBufferedImage)
                        processAction.enabled = true
                    case None => {}
                }
            } finally {
                cursor = getPredefinedCursor(DEFAULT_CURSOR)
            }
        }

        // Action performed when "Process" button is pressed
        val processAction = Action("Process") {
            cursor = getPredefinedCursor(WAIT_CURSOR)
            try {
                // Process and update image display if image is loaded
                image match {
                    case Some(x) =>
                        processImage(x)
                        imageView.icon = new ImageIcon(x.getBufferedImage)
                    case None =>
                        Dialog.showMessage(null, "Image not opened", title, Error)
                }
            } finally {
                cursor = getPredefinedCursor(DEFAULT_CURSOR)
            }
        }
        processAction.enabled = false

        //
        // Create UI
        //

        // Component for displaying the image
        val imageView = new Label

        // Create button panel
        val buttonsPanel = new GridPanel(rows0 = 0, cols0 = 1) {
            contents += new Button(openImageAction)
            contents += new Button(processAction)
            vGap = 5
        }

        // Layout frame contents
        contents = new BorderPanel() {
            // Action buttons on the left
            add(new FlowPanel(buttonsPanel), BorderPanel.Position.West)
            // Image display in the center
            val imageScrollPane = new ScrollPane(imageView) {
                preferredSize = new Dimension(640, 480)
            }
            add(imageScrollPane, BorderPanel.Position.Center)
        }

        // Mark for display in the center of the screen
        centerOnScreen()
    }


    /** Ask user for location and open new image. */
    private def openImage(): Option[IplImage] = {
        // Ask user for the location of the image file
        if (fileChooser.showOpenDialog(null) != Approve) {
            return None
        }

        // Load the image
        val path = fileChooser.selectedFile.getAbsolutePath
        val newImage = cvLoadImage(path)
        if (newImage != null) {
            Some(newImage)
        } else {
            Dialog.showMessage(null, "Cannot open image file: " + path, top.title, Error)
            None
        }
    }


    /** Process image in place.  */
    private def processImage(src: IplImage) {
        // Flip upside down
        cvFlip(src, src, 0)
        // Swap red and blue channels
        cvCvtColor(src, src, CV_BGR2RGB)
    }
}
