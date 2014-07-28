/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 * Author's e-mail: jpsacha at gmail dot com
 */
package flycapture.examples.cpp

import org.bytedeco.javacpp.FlyCapture2._
import org.bytedeco.javacpp.Pointer

/**
 * The GrabCallbackEx sample program demonstrates how to set up an asynchronous image callback
 * application using FlyCapture2 API.
 *
 * Example of using FlyCapture2 C++ API. Based on GrabCallbackEx.cpp example from FlyCapture SDK.
 */
object GrabCallbackEx extends App {

  /**
   * Callback function object that will be passed to `Camera::StartCapture`.
   * The actual callback is in the `call` method.
   *
   * Original C++ definition looks like this:
   * {{{
   *  void OnImageGrabbed(Image* pImage, const void* pCallbackData)
   *  &#123;
   *    printf( "Grabbed image %d\n", imageCnt++ );
   *    return;
   *  &#125;
   * }}}
   */
  object OnImageGrabbed extends ImageEventCallback {
    println("OnImageGrabbed() - constructor")
    var counter = 0

    /**
     * Actual callback method.
     */
    override def call(pImage: Image, pCallbackData: Pointer) {
      println("OnImageGrabbed::call(...)")
      counter += 1
      println("OnImageGrabbed -> Grabbed image " + counter)
    }
  }

  def runSingleCamera(guid: PGRGuid) {
    val numImages = 10

    // Connect to a camera
    val cam = new Camera()
    check(cam.Connect(guid), " - Connect")

    // Get the camera information
    val camInfo = new CameraInfo()
    check(cam.GetCameraInfo(camInfo), " - GetCameraInfo")
    printCameraInfo(camInfo)

    // Start capturing images
    println("Start capture")
    check(cam.StartCapture(OnImageGrabbed, null), " - StartCapture")

    val frameRateProp = new Property(FRAME_RATE)
    check(cam.GetProperty(frameRateProp), " - GetProperty")

    println("Capture in loop")
    for (imageCnt <- 0 until numImages) {
      println("loop imageCnt: " + imageCnt)
      val millisecondsToSleep = (1000 / frameRateProp.absValue).toInt
      println("millisecondsToSleep: " + millisecondsToSleep)
      Thread.sleep(millisecondsToSleep)
    }

    // Stop capturing images
    check(cam.StopCapture(), " - StopCapture")

    // Disconnect the camera
    check(cam.Disconnect(), " - Disconnect")
  }

  printBuildInfo()

  val busMgr = new BusManager()
  val numCameras = Array[Int](0)
  check(busMgr.GetNumOfCameras(numCameras), " - GetNumOfCameras")
  println("Number of cameras detected: " + numCameras(0))

  for (i <- 0 until numCameras(0)) {
    val guid = new PGRGuid()
    check(busMgr.GetCameraFromIndex(i, guid), " - GetCameraFromIndex")

    runSingleCamera(guid)
  }

  println("Done!")
}
