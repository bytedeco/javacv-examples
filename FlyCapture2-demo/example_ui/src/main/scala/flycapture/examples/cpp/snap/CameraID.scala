/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp.snap

import org.bytedeco.javacpp.FlyCapture2.{CameraInfo, PGRGuid}

/**
 * Basic camera information.
 *
 * @author Jarek Sacha 
 */
case class CameraID(guid: PGRGuid, cameraInfo: CameraInfo) {
  override def toString = s"#${cameraInfo.serialNumber} (${cameraInfo.modelName.getString})"
}
