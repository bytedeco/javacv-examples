/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook;

import org.junit.Test;

import java.io.File;

import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_videoio.*;
import static org.junit.Assert.*;

/**
 * Test reading of a video from a file using C API.
 */
public final class VideoFileCaptureTest {

    @Test
    public void captureFromFile() throws Exception {

        final File file = new File("data/bike.avi");
        assertTrue("Input video file exists: "+file.getAbsolutePath(), file.exists());

        final CvCapture capture = cvCreateFileCapture(file.getPath());
        assertNotNull(capture);
        try {
            final long nbFrames = (long) cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_COUNT);
            assertEquals(119, nbFrames);

            final double fps = (long) cvGetCaptureProperty(capture, CV_CAP_PROP_FPS);
            assertEquals(15, fps, 0.0001);

            IplImage frame;
            long count = 0;
            while (cvGrabFrame(capture) != 0 && (frame = cvRetrieveFrame(capture)) != null) {
                assertEquals(320, frame.width());
                assertEquals(240, frame.height());
                count++;
            }

            assertEquals(nbFrames, count);
        } finally {
            cvReleaseCapture(capture);
        }
    }
}
