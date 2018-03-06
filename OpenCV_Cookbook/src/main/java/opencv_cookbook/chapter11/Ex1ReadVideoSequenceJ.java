package opencv_cookbook.chapter11;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import javax.swing.*;

/**
 * The example for section "Reading video sequences" in Chapter 10, page 248.
 * <p>
 * This version of the example is implemented using JavaCV `FFmpegFrameGrabber`class.
 */
public class Ex1ReadVideoSequenceJ {

    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("data/bike.avi");
        // Open video video file
        grabber.start();

        // Prepare window to display frames
        CanvasFrame canvasFrame = new CanvasFrame("Extracted Frame", 1);
        canvasFrame.setCanvasSize(grabber.getImageWidth(), grabber.getImageHeight());
        // Exit the example when the canvas frame is closed
        canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        long delay = Math.round(1000d / grabber.getFrameRate());

        // Read frame by frame, stop early if the display window is closed
        Frame frame;
        while ((frame = grabber.grab()) != null && canvasFrame.isVisible()) {
            // Capture and show the frame
            canvasFrame.showImage(frame);
            // Delay
            Thread.sleep(delay);
        }

        // Close the video file
        grabber.release();

    }

}
