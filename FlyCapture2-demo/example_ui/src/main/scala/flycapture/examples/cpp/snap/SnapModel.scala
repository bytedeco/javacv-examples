/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import java.io.{File, IOException}
import java.util.concurrent.locks.ReentrantReadWriteLock

import flycapture.CheckMacro.check
import flycapture.examples.cpp.FC2Exception
import flycapture.examples.cpp.FC2Utils._
import grizzled.slf4j.Logger
import javafx.{concurrent => jfxc}
import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._
import org.bytedeco.javacpp.{IntPointer, Pointer}
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.concurrent.Worker
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control.{Alert, ButtonType, Dialog}
import scalafx.scene.image.{PixelFormat, WritableImage}
import scalafx.scene.{Scene, image => sfxsi}
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Stage}
import scalafx.util.Duration
import scalafxml.core.{DependenciesByType, FXMLView}

import scala.reflect.runtime.universe.typeOf

object SnapModel {


}

/**
 * Model for [[flycapture.examples.cpp.snap.SnapView]].
 *
 * @author Jarek Sacha 
 */
class SnapModel {

  // TODO: Handle situation when camera gets disconnected

  private val logger = Logger(this.getClass)

  var parent: Stage = _

  val fc2Version = new StringProperty("?.?.?.?")
  val cameraInfo = new StringProperty("No camera connected")
  val selectedCamera: ObjectProperty[Option[CameraBase]] = ObjectProperty[Option[CameraBase]](None)
  private var selectedCameraID: Option[CameraID] = None
  val snappedImage = new ObjectProperty[javafx.scene.image.Image]()
  val canSnap = BooleanProperty(value = false)
  val canStart = BooleanProperty(value = false)
  val canStop = BooleanProperty(value = false)
  val canSave = BooleanProperty(value = false)

  private val liveViewOn = BooleanProperty(value = false)

  /**
   * Image in FC2 format, so we can use FC2 API to save it to a file.
   */
  private val snappedFC2ImageLock = new ReentrantReadWriteLock()
  private var snappedFC2Image: Option[Image] = None

  private val imageCaptureScheduledService = new ImageCaptureScheduledService()

  private val version = new FC2Version()
  private val saveImageFileChooser = new FileChooser {
    title = "Save Image"
    extensionFilters ++= Seq(new ExtensionFilter("PNG Images", "*.png"))
    initialFileName = "image.png"
  }


  // Logic for enabling controls
  canSnap <== !liveViewOn && selectedCamera =!= None
  canStart <== !liveViewOn && selectedCamera =!= None
  canStop <== liveViewOn && selectedCamera =!= None
  canSave <== snappedImage =!= null


  private val busCallback = new BusEventCallback() {
    def eventName(id: Int): String = id match {
      case 1 => "BUS_RESET"
      case 2 => "ARRIVAL"
      case 3 => "REMOVAL"
      case _ => "?"
    }

    logger.trace("BusEventCallback")
    override def call(pParameter: Pointer, serialNumber: Int): Unit = {
      val id = pParameter.asByteBuffer().get()
      logger.info("" +
        "BusEventCallback\n" +
        "  Event ID    : " + id + " " + eventName(id) + "\n" +
        "  serialNumber: " + serialNumber)

      selectedCamera().foreach { _ =>
        if ("REMOVAL".equals(eventName(id))) {
          if (selectedCameraID.get.cameraInfo.serialNumber == serialNumber) {
            logger.info("Disconnecting camera")
            disconnectCurrentCamera()
          }
        }
      }
    }
  }
  private val callbackHandle = new CallbackHandle(new IntPointer(8L))

  private var busManager: Option[BusManager] = None

  private def disconnectCurrentCamera(noFX: Boolean = false) {
    logger.trace("disconnectCurrentCamera() - start")

    onStopLiveCapture()
    selectedCamera().foreach { camera =>
      if (camera.IsConnected()) check(camera.Disconnect())
    }

    onFXAndWait {
      logger.trace("disconnectCurrentCamera() - clear captured images")
      cameraInfo() = "No camera connected"
      selectedCamera() = None
      snappedImage() = null
    }
    snappedFC2ImageLock.writeLock.lock()
    snappedFC2Image = None
    snappedFC2ImageLock.writeLock.unlock()
    logger.trace("disconnectCurrentCamera() - end")
  }

  def initialize(): Unit = {
    onFXAndWait {

      logger.debug("Initializing FlyCapture camera connections")

      def asIntPointer(v: Int) = {
        val r = new IntPointer(1L)
        r.put(v)
        r
      }

      // Get library version
      Utilities.GetLibraryVersion(version)
      fc2Version() = s"${version.major}.${version.minor}.${version.`type`}.${version.build}"
      logger.trace(s"FlyCapture2 library version: ${fc2Version()}")

      val id1 = new IntPointer(1L)
      id1.put(1)
      busManager = Some(new BusManager())
      busManager.foreach { bm =>
        bm.RegisterCallback(busCallback, BUS_RESET, asIntPointer(1), callbackHandle)
        bm.RegisterCallback(busCallback, ARRIVAL, asIntPointer(2), callbackHandle)
        bm.RegisterCallback(busCallback, REMOVAL, asIntPointer(3), callbackHandle)
      }
    }

    selectCamera()
  }


  def selectCamera(): Unit = {
    onFXAndWait {
      logger.debug("selectCamera()")

      busManager match {
        case Some(bm) => selectCamera(bm)
        case None =>
      }
    }
  }

  private def selectCamera(bm: BusManager): Unit = {
    logger.debug("selectCamera(...)")

    disconnectCurrentCamera()

    val selectedItem = onFXAndWait {
      logger.trace("Load CameraSelectionView")

      //      deviceConnected() = false

      // Load main view
      val resourcePath = "CameraSelectionView.fxml"
      val resource = getClass.getResource(resourcePath)
      if (resource == null) throw new IOException("Cannot load resource: '" + resourcePath + "'")

      val model = new CameraSelectionModel(bm)

      val root = FXMLView(resource, new DependenciesByType(Map(typeOf[CameraSelectionModel] -> model)))

      model.initialize()

      val connectButtonType = new ButtonType("Connect", ButtonData.OKDone)
      new Dialog[CameraID] {
        initOwner(parent)
        title = "Select FlyCapture Camera"
        resizable = false
        dialogPane().content = root
        dialogPane().buttonTypes = Seq(connectButtonType, ButtonType.Cancel)
        resultConverter = dialogButton =>
          if (dialogButton == connectButtonType) model.selectedItem
          else null
      }.showAndWait().asInstanceOf[Option[CameraID]]
    }

    logger.trace("Selected camera: " + selectedItem)

    selectedItem match {
      case Some(id) =>
        selectedCamera().foreach(camera => check(camera.Disconnect()))
        onFXAndWait {
          selectedCamera() = None
          cameraInfo() = "No camera connected"
        }

        selectedCameraID = Some(id)

        // Determine camera type
        val interfaceType = new IntPointer(1L)
        check(busManager.get.GetInterfaceTypeFromGuid(id.guid, interfaceType))
        val cam = interfaceType.get match {
          case INTERFACE_GIGE => new GigECamera()
          case _ => new Camera()
        }

        // Connect to a camera
        check(cam.Connect(id.guid))

        onFXAndWait {
          selectedCamera() = Some(cam)
          val camInfo = id.cameraInfo
          cameraInfo() = s"Camera: ${camInfo.modelName.getString}, SN: ${camInfo.serialNumber}"
          onStartLiveCapture()
        }

      case None =>
    }

  }

  def shutDown(): Unit = {
    logger.debug("shutDown()")
    onFXAndWait {
      imageCaptureScheduledService.cancel()
    }

    logger.debug("Disconnect cameras")
    selectedCamera().foreach { camera =>
      if (camera.IsConnected()) check(camera.Disconnect())
    }

    logger.debug("Release buffers")
    snappedFC2Image.foreach(_.ReleaseBuffer())

    logger.debug("Unregister callback")
    busManager.foreach(bm =>
      bm.UnregisterCallback(callbackHandle)
    )
  }

  def onSnap(): Unit = {
    logger.debug("onSnap()")
    new Thread(new javafx.concurrent.Task[Unit] {

      override def call(): Unit = selectedCamera().foreach(snap)

      override def failed(): Unit = {
        super.failed()
        showException(parent, "Snap", "Error while snapping an image.", getException)
      }
    }).start()
  }

  private def snap(camera: CameraBase) {
    logger.trace("snap(...)")
    // Start capturing images
    if (!camera.IsConnected()) {
      logger.warn("Camera not connected")
      return
    }
    check(camera.StartCapture())

    val rawImage = new Image()
    // Retrieve an image
    check(camera.RetrieveBuffer(rawImage))

    check(camera.StopCapture())

    // Create a converted image
    val convertedImage = new Image()

    // Convert the raw image
    check(rawImage.Convert(PIXEL_FORMAT_RGB, convertedImage))

    // Convert to JFX Image
    val width = rawImage.GetCols()
    val height = rawImage.GetRows()
    val wi = new WritableImage(width, height)
    val pf = PixelFormat.getByteRgbInstance
    val data = convertedImage.GetData
    data.capacity(convertedImage.GetDataSize())
    wi.getPixelWriter.setPixels(0, 0, width, height, pf, data.asBuffer(), width * 3)
    Platform.runLater {
      snappedImage() = wi
    }

    snappedFC2ImageLock.writeLock.lock()
    snappedFC2Image = Some(convertedImage)
    snappedFC2ImageLock.writeLock.unlock()
  }


  def onStartLiveCapture(): Unit = {
    offFXAndWait {
      logger.debug("onStartLiveCapture()")
      // Start capturing images
      selectedCamera().foreach { camera =>
        logger.debug("StartCapture()")
        check(camera.StartCapture())
      }
    }

    onFXAndWait {
      imageCaptureScheduledService.restart()
      liveViewOn() = true
    }
  }

  def onStopLiveCapture(): Unit = {
    onFXAndWait {
      logger.debug("onStopLiveCapture()")
      logger.debug("Current state: " + imageCaptureScheduledService.state())
      imageCaptureScheduledService.state() match {
        case Worker.State.Running.delegate | Worker.State.Ready.delegate | Worker.State.Scheduled.delegate =>
          logger.debug("Cancelling capture service. Current state: " + imageCaptureScheduledService.state())
          imageCaptureScheduledService.cancel()
          logger.debug("Current state: " + imageCaptureScheduledService.state())
          val timeout = 1000
          val sleepTime = 100
          var waitTime = 0
          while (Worker.State.Cancelled.delegate != imageCaptureScheduledService.state() && waitTime < timeout) {
            logger.debug("Waiting for CANCEL to complete, Current state: " + imageCaptureScheduledService.state())
            Thread.sleep(sleepTime)
            waitTime += sleepTime
          }
          if (sleepTime > timeout) throw new Exception("Timeout waiting for `imageCaptureScheduledService` to CANCEL.")
        case _ =>
          logger.debug("No need to cancel capture service")
      }
    }

    offFXAndWait {
      selectedCamera().foreach { camera =>
        logger.debug("StopCapture()")
        if (camera.IsConnected()) camera.StopCapture()
      }
    }

    onFXAndWait {
      liveViewOn() = false
    }
  }

  def onSaveImage(): Unit = {
    logger.debug("onSaveImage()")

    // Do minimal blocking by creating a copy of the snapped image.
    // Blocking while saving would take much longer.
    snappedFC2ImageLock.readLock.lock()
    val image: Option[Image] = try {
      if (snappedFC2Image.isEmpty) None
      else {
        val tmp = new Image()
        try {
          check {tmp.DeepCopy(snappedFC2Image.get)}
          Some(tmp)
        } catch {
          case ex: FC2Exception =>
            showException(parent, "Save Image", "Error preparing image for saving.", ex)
            tmp.ReleaseBuffer()
            None
        }
      }
    } finally {
      snappedFC2ImageLock.readLock.unlock()
    }

    if (image.isEmpty) {
      new Alert(AlertType.Error) {
        initOwner(parent)
        headerText = "No image to save."
      }.showAndWait()
    } else {
      // Ask for file name
      val selectedFile = saveImageFileChooser.showSaveDialog(parent)
      if (selectedFile != null) {

        val file =
          if (selectedFile.getName.toLowerCase.endsWith(".png")) selectedFile
          else new File(selectedFile.getParentFile, selectedFile.getName + ".png")

        // Remember for the next time
        saveImageFileChooser.initialDirectory = file.getParentFile
        saveImageFileChooser.initialFileName = file.getName

        // Save the image. If a file format is not passed in, then the file
        // extension is parsed to attempt to determine the file format.
        val filePath = file.getCanonicalPath
        try {
          check(image.get.Save(filePath))
        } catch {
          case ex: FC2Exception =>
            showException(parent, "Save Image", "Error saving image to file: " + filePath, ex)
        } finally {
          image.get.ReleaseBuffer()
        }

      }
    }
  }

  def onSettings(): Unit = {
    logger.debug("onSettings()")

    selectedCamera() match {
      case Some(camera) =>
        val cameraConfiguration = new CameraConfiguration(camera, parent)
        // Create UI
        val dialogStage = new Stage() {
          initOwner(parent)
          title = "Camera Settings " + cameraInfo()
          scene = new Scene(cameraConfiguration.view) {
            icons += new sfxsi.Image("/flycapture/examples/cpp/snap/logo.png")
          }
        }
        dialogStage.showAndWait()

      case None =>
        new Alert(AlertType.Warning) {
          initOwner(parent)
          title = "Select Camera"
          headerText = "Camera not selected."
        }.showAndWait()
    }
  }


  private class ImageCaptureScheduledService() extends jfxc.ScheduledService[String]() {
    logger.debug("ImageCaptureScheduledService()")

    // Store current image settings. Used to detect a mode change while recording
    private var imageFormat: ResolutionAndPixelFormat = _

    private var counter = 0
    setPeriod(Duration(33))


    override def scheduled(): Unit = {
      logger.trace("ImageCaptureScheduledService::scheduled()")
      require(selectedCamera().isDefined, "Camera must be defined.")
      selectedCamera().foreach { camera =>
        require(camera.IsConnected(), "Camera must be connected.")
        imageFormat = getCamResolutionAndPixelFormat(selectedCamera().get)
      }
    }


    override def createTask(): jfxc.Task[String] =
      new jfxc.Task[String]() {
        override def call(): String = {
          counter += 1
          selectedCamera().foreach { camera =>
            if (!camera.IsConnected()) throw new Exception("Attempting capture when camera is not connected.")

            // Check frequently if the service was not cancelled, as it may happen in-between camera operations
            // It is possible that after `cancel` camera will be disconnected, we should not try to assess it
            // (as it will result in error) nor should we display preview image (will not be current).
            if (isCancelled) {
              logger.trace("Cancelling call() - 1")
              return counter.toString
            }

            // Retrieve an image
            val rawImage = new Image()
            check(camera.RetrieveBuffer(rawImage))

            if (isCancelled) {
              logger.trace("Cancelling call() - 2")
              return counter.toString
            }

            val currentImageFormat = ResolutionAndPixelFormat(
              columns = rawImage.GetCols(),
              rows = rawImage.GetRows(),
              pixelFormat = rawImage.GetPixelFormat()
            )

            val modeChanged = imageFormat != currentImageFormat
            if (modeChanged) {
              onStopLiveCapture()
              new Alert(AlertType.Warning) {
                initOwner(parent)
                title = "Live View"
                headerText = "A mode change was detected while in Live Mode. Live Mode will stop."
              }.showAndWait()
              return counter.toString
            }

            if (isCancelled) {
              logger.trace("Cancelling call() - 3")
              return counter.toString
            }

            // Create a converted image
            val convertedImage = new Image()
            check(rawImage.Convert(PIXEL_FORMAT_RGB, convertedImage))
            check(rawImage.ReleaseBuffer())

            if (isCancelled) return counter.toString

            // Update displayed image.
            // Convert to JFX Image
            val width = convertedImage.GetCols()
            val height = convertedImage.GetRows()
            val wi = new WritableImage(width, height)
            val pf = PixelFormat.getByteRgbInstance
            val data = convertedImage.GetData
            data.capacity(convertedImage.GetDataSize())
            wi.getPixelWriter.setPixels(0, 0, width, height, pf, data.asBuffer(), width * 3)

            if (isCancelled) {
              logger.trace("Cancelling call() - 4")
              return counter.toString
            }

            Platform.runLater {
              snappedImage() = wi
            }

            // Update snapped image used for saving to file
            snappedFC2ImageLock.writeLock.lock()
            snappedFC2Image = Some(convertedImage)
            snappedFC2ImageLock.writeLock.unlock()
          }
          counter.toString
        }

        override def failed(): Unit = {
          super.failed()
          showException(parent, "Live View", "Error while upodating live view.", getException)
        }
      }
  }

}
