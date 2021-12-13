/*
 * Copyright (c) 2021 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11

import org.bytedeco.javacv.{FFmpegFrameGrabber, JavaFXFrameConverter}
import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene
import scalafx.scene.image.ImageView
import scalafx.scene.layout.StackPane
import scalafx.stage.Stage

import java.nio.{ByteBuffer, ShortBuffer}
import java.util.concurrent.{Executors, TimeUnit}
import java.util.logging.{Level, Logger}
import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, SourceDataLine}
import scala.util.Using

/**
 * Example of playing video that contains audio.
 *
 * You can provide you own video to play on command line. It can be either a file path or an URL.
 *
 * This example is based on JavaCV sample
 * [[https://github.com/bytedeco/javacv/blob/master/samples/JavaFxPlayVideoAndAudio.java JavaFxPlayVideoAndAudio.java]],
 * here it is implemented using [[https://github.com/scalafx/scalafx ScalaFX]]
 */
object ScalaFXPlayVideoAndAudio extends JFXApp3 {

  private val LOG                        = Logger.getLogger(this.getClass.getName)
  private val AppName                    = "Play Video with Audio"
  private var playThread: Option[Thread] = None

  /**
   * Playback timer that helps to ensure video is played in real time (not to fast not too slow).
   * If video contains sound, it will be used to sync.
   *
   * @param soundLine video sound line, if video has sounds.
   */
  private class PlaybackTimer(soundLine: Option[DataLine]) {
    private var startTime: Option[Long] = None

    def start(): Unit = if (soundLine.isEmpty) startTime = Option(System.nanoTime)

    def elapsedMicros: Long = soundLine match {
      case Some(sl) =>
        sl.getMicrosecondPosition
      case None =>
        startTime match {
          case Some(t) =>
            (System.nanoTime - t) / 1000
          case None =>
            throw new IllegalStateException("PlaybackTimer not initialized.")
        }
    }
  }

  override def start(): Unit = {

    val imageView = new ImageView()

    // Create simple UI with only an image view
    val primaryStage = new JFXApp3.PrimaryStage() {
      title = AppName
      scene = new Scene(640, 480) {
        root = new StackPane() {
          children += imageView
        }
      }
    }

    imageView.fitWidth <== primaryStage.width
    imageView.fitHeight <== primaryStage.height

    // Read video url from from command line, if provided
    val videoFilename = parameters.raw.headOption.getOrElse("data/oow2010-2.flv")

    // Create thread to play video
    playThread = Option(
      new Thread(() => playVideo(primaryStage, imageView, videoFilename))
    )
    // Start playback
    playThread.foreach(_.start())
  }

  override def stopApp(): Unit = {
    // Stop video playback on application close
    playThread.foreach(_.interrupt())
  }

  private def playVideo(primaryStage: Stage, imageView: ImageView, videoFilename: String): Unit = {
    try {
      val grabber = new FFmpegFrameGrabber(videoFilename)
      grabber.start()

      println(s"Frame rate: ${grabber.getFrameRate}")

      // Resize main frame to match video size
      primaryStage.setWidth(grabber.getImageWidth)
      primaryStage.setHeight(grabber.getImageHeight)

      val (playbackTimer, soundLine): (PlaybackTimer, Option[SourceDataLine]) =
        if (grabber.getAudioChannels > 0) {
          val audioFormat = new AudioFormat(grabber.getSampleRate, 16, grabber.getAudioChannels, true, true)
          val info        = new DataLine.Info(classOf[SourceDataLine], audioFormat)
          val soundLine   = AudioSystem.getLine(info).asInstanceOf[SourceDataLine]
          soundLine.open(audioFormat)
          soundLine.start()
          val playbackTimer = new PlaybackTimer(Option(soundLine))
          (playbackTimer, Option(soundLine))
        } else {
          (new PlaybackTimer(None), None)
        }

      val audioExecutor = Executors.newSingleThreadExecutor()
      val imageExecutor = Executors.newSingleThreadExecutor()

      var lastTimeStamp = -1L

      Using.resource(new JavaFXFrameConverter()) { converter =>
        val maxReadAheadBufferMicros = 1000 * 1000L

        var frame = grabber.grab()
        while (!Thread.interrupted() && frame != null) {

          if (lastTimeStamp < 0) playbackTimer.start()

          lastTimeStamp = frame.timestamp
          println(lastTimeStamp)

          if (frame.image != null) {
            // Decode image frame

            val imageFrame = frame.clone
            imageExecutor.submit[Unit](() => {
              val image                = converter.convert(imageFrame)
              val timeStampDeltaMicros = imageFrame.timestamp - playbackTimer.elapsedMicros
              imageFrame.close()

              if (timeStampDeltaMicros > 0) {
                // We are ahead, we will need to slow down to keep synced with the audio
                // Wait before displaying next frame
                val delayMillis = timeStampDeltaMicros / 1000L
                try {
                  Thread.sleep(delayMillis)
                } catch {
                  case e: InterruptedException =>
                    e.printStackTrace()
                }
              }
              Platform.runLater(imageView.setImage(image))
            })
          } else if (frame.samples != null) {
            // Decode audio frame

            if (soundLine.isEmpty) {
              throw new IllegalStateException("Internal error: sound playback not initialized")
            }

            val channelSamplesShortBuffer = frame.samples(0).asInstanceOf[ShortBuffer]
            channelSamplesShortBuffer.rewind()
            val outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity * 2)
            for (i <- 0 until channelSamplesShortBuffer.capacity) {
              val v = channelSamplesShortBuffer.get(i)
              outBuffer.putShort(v)
            }

            // Write audio without blocking
            audioExecutor.submit(() => {
              soundLine.foreach(_.write(outBuffer.array, 0, outBuffer.capacity))
              outBuffer.clear()
            })
          }

          // Check if we are grabbing frames too fast (avoid using too much memory)
          val timeStampDeltaMicros = frame.timestamp - playbackTimer.elapsedMicros
          if (timeStampDeltaMicros > maxReadAheadBufferMicros) {
            Thread.sleep((timeStampDeltaMicros - maxReadAheadBufferMicros) / 1000)
          }

          // Get next frame
          frame = grabber.grab()
        }
      }

      if (!Thread.interrupted) {
        // Make sure that we played to the end of the last timestamp
        val delay = (lastTimeStamp - playbackTimer.elapsedMicros) / 1000 + math.round(1 / grabber.getFrameRate * 1000)
        Thread.sleep(Math.max(0, delay))
      }

      // Stop frame grabber
      grabber.stop()
      grabber.release()

      // Stop audio line
      soundLine.foreach(_.stop())

      // Stop rendering threads
      audioExecutor.shutdownNow()
      audioExecutor.awaitTermination(10, TimeUnit.SECONDS)
      imageExecutor.shutdownNow()
      imageExecutor.awaitTermination(10, TimeUnit.SECONDS)

      // Exit application
      Platform.exit()
    } catch {
      case exception: Exception =>
        LOG.log(Level.SEVERE, null, exception)
        System.exit(1)
    }
  }
}
