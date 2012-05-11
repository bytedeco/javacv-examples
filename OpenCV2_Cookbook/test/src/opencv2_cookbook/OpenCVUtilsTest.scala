/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook

import org.junit.Test
import org.junit.Assert._
import com.googlecode.javacv.cpp.opencv_core.CvRect


class OpenCVUtilsTest {

    @Test
    def testToRectangle() {
        val cvRect = new CvRect(15, 21, 33, 48)
        val awtRect = OpenCVUtils.toRectangle(cvRect)

        assertEquals(cvRect.x(), awtRect.x)
        assertEquals(cvRect.y(), awtRect.y)
        assertEquals(cvRect.width(), awtRect.width)
        assertEquals(cvRect.height(), awtRect.height)
    }

}
