/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook;

import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jarek Sacha
 * @since 9/24/12 10:34 PM
 */
public final class OpenCVFrameGrabberTest {

    /**
     * Test use of OpenCVFrameGrabber in `while (grabber.grab() != null)` loop.
     *
     * @throws Exception
     */
    @Test
    @Ignore("Fails with JavCV 0.3 and after")
    public void whileFrameNotNull() throws Exception {

        final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber("data/bike.avi");
        grabber.start();

        try {
            final long length = grabber.getLengthInFrames();
            long frameCount = 0;
            while (grabber.grab() != null) {
                frameCount++;
            }

            // All frames should be read
            assertEquals(length, frameCount);
        } finally {
            // Close the video file
            grabber.release();
        }
    }
}
