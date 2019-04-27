/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FPS;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_COUNT;
import static org.junit.Assert.*;

/**
 * Test reading of a video from a file using C API.
 */
public final class VideoFileCaptureTest {

    @Test
    @Ignore("Fails with JavCV 0.3 and after")
    public void captureFromFile() throws Exception {

        final File file = new File("data/bike.avi");
        assertTrue("Input video file exists: " + file.getAbsolutePath(), file.exists());

        final VideoCapture capture = new VideoCapture("data/bike.avi");
        assertNotNull("'capture' cannot be null.", capture);
        assertTrue("`capture` must be opened", capture.isOpened());
        try {
            final long nbFrames = (long) capture.get(CAP_PROP_FRAME_COUNT);
            assertEquals(119, nbFrames);

            final double fps = (long) capture.get(CAP_PROP_FPS);
            assertEquals(15, fps, 0.0001);

            final Mat frame = new Mat();
            long count = 0;
            while (capture.read(frame)) {
                assertEquals(320, frame.cols());
                assertEquals(240, frame.rows());
                count++;
            }

            assertEquals(nbFrames, count);
        } finally {
            capture.release();
        }
    }
}
