/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook;

import org.bytedeco.opencv.opencv_core.DMatch;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


/**
 * JavaCPP has a ability to copy values referenced by pointer to another pointer using Pointer.put().
 * <p/>
 * This test validates Pointer.put() in different scenarios.
 *
 * @author Jarek Sacha
 * @since 8/11/12 8:43 PM
 */
public final class PointerPutTest {


    /**
     * Check copy constructor.
     */
    @Test
    public void copyConstructorDMatch() throws Exception {

        final MyDMatch expected = new MyDMatch(11.1f, 12, 13, 14);

        final DMatch src = new DMatch(expected.queryIdx, expected.trainIdx, expected.imgIdx, expected.distance);
        assertEquals("src", expected, src, 0.1);

        final DMatch dest = new DMatch(src);
        assertEquals("dest", expected, dest, 0.1);
    }


    /**
     * Check Pointer.put on a single object.
     */
    @Test
    public void copySingleDMatch() throws Exception {

        final MyDMatch expected = new MyDMatch(11.1f, 12, 13, 14);

        final DMatch src = new DMatch();
        src.distance(expected.distance);
        src.imgIdx(expected.imgIdx);
        src.queryIdx(expected.queryIdx);
        src.trainIdx(expected.trainIdx);
        assertEquals("src", expected, src, 0.1);

        final DMatch dest = new DMatch();
        dest.put(src);
        assertEquals("dest", expected, dest, 0.1);
    }


    /**
     * Check Pointer.put on a elements of a native array - copy all.
     */
    @Test
    public void copyContainerAllDMatch() throws Exception {

        final MyDMatch[] expected = new MyDMatch[]{
                new MyDMatch(11.1f, 12, 13, 14),
                new MyDMatch(21.1f, 22, 23, 24),
                new MyDMatch(31.1f, 32, 33, 34),
                new MyDMatch(41.1f, 42, 43, 44),
        };

        // Allocate native array of DMatch with 3 elements
        final DMatch src = new DMatch(expected.length);

        // Assign values to source array elements
        for (int i = 0; i < expected.length; i++) {
            src.position(i).distance(expected[i].distance);
            src.position(i).imgIdx(expected[i].imgIdx);
            src.position(i).queryIdx(expected[i].queryIdx);
            src.position(i).trainIdx(expected[i].trainIdx);
        }

        // Verify initialization
        for (int i = 0; i < expected.length; i++) {
            assertEquals("src.position(" + i + ")", expected[i], src.position(i), 0.1);
        }

        // Copy to a new native array
        final DMatch dest = new DMatch(src.capacity());

        // Copy from source to destination
        for (int i = 0; i < expected.length; i++) {
            dest.position(i).put(src.position(i));
        }

        Assert.assertEquals("Capacity", src.capacity(), dest.capacity(), 0.1);

        // Verify copy
        for (int i = 0; i < expected.length; i++) {
            assertEquals("dest.position(" + i + ")", expected[i], dest.position(i), 0.1);
        }
    }


    private static void assertEquals(final String message, final MyDMatch expected, final DMatch actual, final double tolerance) {
        assertNotNull(message + " `expected` should not be null", expected);
        assertNotNull(message + " `expected` should not be null", actual);
        Assert.assertEquals(message + " distance", expected.distance, actual.distance(), tolerance);
        Assert.assertEquals(message + " imgIdx", expected.imgIdx, actual.imgIdx());
        Assert.assertEquals(message + " queryIdx", expected.queryIdx, actual.queryIdx());
        Assert.assertEquals(message + " trainIdx", expected.trainIdx, actual.trainIdx());
    }


    /**
     * Helper for keeping expected values of DMatch fields.
     */
    public static class MyDMatch {
        public final float distance;
        public final int imgIdx;
        public final int queryIdx;
        public final int trainIdx;

        public MyDMatch(final float distance, final int imgIdx, final int queryIdx, final int trainIdx) {
            this.distance = distance;
            this.imgIdx = imgIdx;
            this.queryIdx = queryIdx;
            this.trainIdx = trainIdx;
        }
    }
}