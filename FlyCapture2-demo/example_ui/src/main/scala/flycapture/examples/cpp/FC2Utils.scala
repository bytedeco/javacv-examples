/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp

import flycapture.CheckMacro._
import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._
import org.bytedeco.javacpp
import org.bytedeco.javacpp.{FloatPointer, IntPointer}

/**
  * Some helper methods extracted from FlyCapture2 SDK sample application.
  * Arguments and return values and input arguments are modified to fit Scala style.
  * Method names correspond to the original names
  *
  * @author Jarek Sacha
  */
object FC2Utils {

  def getCamResolutionAndPixelFormat(camera: CameraBase): ResolutionAndPixelFormat = {

    // get the current source-image settings
    val cameraInfo = new CameraInfo()
    check(camera.GetCameraInfo(cameraInfo))

    if (cameraInfo.interfaceType == INTERFACE_GIGE) {
      val gigeCam = camera.asInstanceOf[GigECamera]
      val gigeImageSettings = new GigEImageSettings()
      check(gigeCam.GetGigEImageSettings(gigeImageSettings))

      ResolutionAndPixelFormat(
        columns = gigeImageSettings.width,
        rows = gigeImageSettings.height,
        pixelFormat = gigeImageSettings.pixelFormat)
    } else {
      val cam = camera.asInstanceOf[Camera]
      val videoMode = new javacpp.IntPointer(1L)
      val frameRate = new javacpp.IntPointer(1L)
      cam.GetVideoModeAndFrameRate(videoMode, frameRate)

      if (videoMode.get == VIDEOMODE_FORMAT7) {
        val f7ImageSettings = new Format7ImageSettings()
        val packetSize = new IntPointer(1L)
        val percentage = new FloatPointer(1L)

        check(cam.GetFormat7Configuration(f7ImageSettings, packetSize, percentage))

        ResolutionAndPixelFormat(
          columns = f7ImageSettings.width,
          rows = f7ImageSettings.height,
          pixelFormat = f7ImageSettings.pixelFormat)
      } else {
        // if white balance property is present then stippled is true. This detects
        // when camera is in Y8/Y16 and raw bayer output is enabled
        val propInfo = new PropertyInfo()
        propInfo.`type`(WHITE_BALANCE)

        check(camera.GetPropertyInfo(propInfo))

        val isStippled = propInfo.present

        val pixelFormatOption = getPixelFormatFromVideoMode(videoMode.get, isStippled)
        val pixelFormat = pixelFormatOption.getOrElse(PIXEL_FORMAT_RAW8)
        val dimensionOption = getDimensionsFromVideoMode(videoMode.get)
        val dimension = dimensionOption.getOrElse(Dimension(0, 0))
        ResolutionAndPixelFormat(
          columns = dimension.columns,
          rows = dimension.rows,
          pixelFormat = pixelFormat)
      }
    }
  }

  private def getPixelFormatFromVideoMode(videoMode: Int, stippled: Boolean): Option[Int] =
    videoMode match {
      case VIDEOMODE_640x480Y8 | VIDEOMODE_800x600Y8 | VIDEOMODE_1024x768Y8 | VIDEOMODE_1280x960Y8 |
           VIDEOMODE_1600x1200Y8 =>
        if (stippled) Some(PIXEL_FORMAT_RAW8) else Some(PIXEL_FORMAT_MONO8)
      case VIDEOMODE_640x480Y16 | VIDEOMODE_800x600Y16 | VIDEOMODE_1024x768Y16 | VIDEOMODE_1280x960Y16 |
           VIDEOMODE_1600x1200Y16 =>
        if (stippled) Some(PIXEL_FORMAT_RAW16) else Some(PIXEL_FORMAT_MONO16)
      case VIDEOMODE_640x480RGB | VIDEOMODE_800x600RGB | VIDEOMODE_1024x768RGB | VIDEOMODE_1280x960RGB |
           VIDEOMODE_1600x1200RGB =>
        Some(PIXEL_FORMAT_RGB8)
      case VIDEOMODE_320x240YUV422 | VIDEOMODE_640x480YUV422 | VIDEOMODE_800x600YUV422 | VIDEOMODE_1024x768YUV422 |
           VIDEOMODE_1280x960YUV422 | VIDEOMODE_1600x1200YUV422 =>
        Some(PIXEL_FORMAT_422YUV8)
      case VIDEOMODE_160x120YUV444 =>
        Some(PIXEL_FORMAT_444YUV8)
      case VIDEOMODE_640x480YUV411 =>
        Some(PIXEL_FORMAT_411YUV8)
      case VIDEOMODE_FORMAT7 => None
      case _ => None
    }

  private def getDimensionsFromVideoMode(videoMode: Int): Option[Dimension] = videoMode match {
    case VIDEOMODE_160x120YUV444 =>
      Some(Dimension(columns = 160, rows = 120))
    case VIDEOMODE_320x240YUV422 =>
      Some(Dimension(columns = 320, rows = 240))
    case VIDEOMODE_640x480YUV411 | VIDEOMODE_640x480YUV422 | VIDEOMODE_640x480RGB | VIDEOMODE_640x480Y8 |
         VIDEOMODE_640x480Y16 =>
      Some(Dimension(columns = 640, rows = 480));
    case VIDEOMODE_800x600YUV422 | VIDEOMODE_800x600RGB | VIDEOMODE_800x600Y8 | VIDEOMODE_800x600Y16 =>
      Some(Dimension(columns = 800, rows = 600));
    case VIDEOMODE_1024x768YUV422 | VIDEOMODE_1024x768RGB | VIDEOMODE_1024x768Y8 | VIDEOMODE_1024x768Y16 =>
      Some(Dimension(columns = 1024, rows = 768));
    case VIDEOMODE_1280x960YUV422 | VIDEOMODE_1280x960RGB | VIDEOMODE_1280x960Y8 | VIDEOMODE_1280x960Y16 =>
      Some(Dimension(columns = 1280, rows = 960));
    case VIDEOMODE_1600x1200YUV422 | VIDEOMODE_1600x1200RGB | VIDEOMODE_1600x1200Y8 | VIDEOMODE_1600x1200Y16 =>
      Some(Dimension(columns = 1600, rows = 1200))
    case _ =>
      None
  }

  case class Dimension(rows: Int, columns: Int)

  case class ResolutionAndPixelFormat(rows: Int, columns: Int, pixelFormat: Int)
}
