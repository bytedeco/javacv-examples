
Chapter 11: Processing Video Sequences
======================================

| [<Previous: Chapter 10][chapter10] |   |

To process a video sequence you need access to individual frames. In JavaCV frames are accessed using a [FrameGrabber][FrameGrabber]. There are several concrete frame grabber classes for dealing with different devices and video files. Examples here read video sequences from files using [FFmpegFrameGrabber][FFmpegFrameGrabber].

General pattern for processing a video sequence, here from a video file:

```scala
import org.bytedeco.javacv.FFmpegFrameGrabber
import scala.collection.Iterator.continually
  
val grabber = new FFmpegFrameGrabber("my_video.avi")
grabber.start()

// Read frame by frame till the end of the video file (indicated by `null` frame)
for (frame <- continually(grabber.grab()).takeWhile(_ != null)) {
  // Do some processing
}

// Close the video file
grabber.release()

```

Similarly in Java:
 ``` java
FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("my_video.avi");
// Open video video file
grabber.start();

// Read frame by frame till the end of the video file (indicated by `null` frame)
Frame frame;
while ((frame = grabber.grab()) != null) {
  // Do some processing
}

// Close the video file
grabber.release();
```


| [<Previous: Chapter 10][chapter10] | **Chapter 11: Processing Video Sequences** |  |

[chapter10]: /OpenCV_Cookbook/src/main/scala/opencv_cookbook/chapter10
[FrameGrabber]: http://bytedeco.org/javacv/apidocs/org/bytedeco/javacv/FrameGrabber.html
[FFmpegFrameGrabber]: http://bytedeco.org/javacv/apidocs/org/bytedeco/javacv/FFmpegFrameGrabber.html