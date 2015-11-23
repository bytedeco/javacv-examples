package pylon.samples

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pylon5.{CConfigurationEventHandler, CInstantCamera}

/**
  * @author Jarek Sacha 
  */
class ConfigurationEventPrinter extends CConfigurationEventHandler {

  override def OnAttach(camera: CInstantCamera): Unit = {
    println("OnAttach event")
  }

  override def OnAttached(camera: CInstantCamera): Unit = {
    println("OnAttached event for device " + modelName(camera))
  }

  override def OnOpen(camera: CInstantCamera): Unit = {
    println("OnOpen event for device " + modelName(camera))
  }

  override def OnOpened(camera: CInstantCamera): Unit = {
    println("OnOpened event for device " + modelName(camera))
  }

  override def OnGrabStart(camera: CInstantCamera): Unit = {
    println("OnGrabStart event for device " + modelName(camera))
  }

  override def OnGrabStarted(camera: CInstantCamera): Unit = {
    println("OnGrabStarted event for device " + modelName(camera))
  }

  override def OnGrabStop(camera: CInstantCamera): Unit = {
    println("OnGrabStop event for device " + modelName(camera))
  }

  override def OnGrabStopped(camera: CInstantCamera): Unit = {
    println("OnGrabStopped event for device " + modelName(camera))
  }

  override def OnClose(camera: CInstantCamera): Unit = {
    println("OnClose event for device " + modelName(camera))
  }

  override def OnClosed(camera: CInstantCamera): Unit = {
    println("OnClosed event for device " + modelName(camera))
  }

  override def OnDestroy(camera: CInstantCamera): Unit = {
    println("OnDestroy event for device " + modelName(camera))
  }

  override def OnDestroyed(camera: CInstantCamera): Unit = {
    println("OnDestroyed event")
  }

  override def OnDetach(camera: CInstantCamera): Unit = {
    println("OnDetach event for device " + modelName(camera))
  }

  override def OnDetached(camera: CInstantCamera): Unit = {
    println("OnDetached event for device " + modelName(camera))
  }

  override def OnGrabError(camera: CInstantCamera, errorMessage: BytePointer): Unit = {
    println("OnGrabError event for device " + modelName(camera))
    println("Error Message: " + errorMessage.getString)
  }

  override def OnCameraDeviceRemoved(camera: CInstantCamera): Unit = {
    println("OnCameraDeviceRemoved event for device " + modelName(camera))
  }

  private def modelName(camera: CInstantCamera): String = camera.GetDeviceInfo().GetModelName().c_str().getString
}
