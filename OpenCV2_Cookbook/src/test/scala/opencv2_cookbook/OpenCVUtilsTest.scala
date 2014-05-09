/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook

import org.bytedeco.javacpp.opencv_core._
import org.junit.Assert._
import org.junit.Test


class OpenCVUtilsTest {

  @Test
  def testToRectangle() {
    val rect = cvRect(15, 21, 33, 48)
    val awtRect = OpenCVUtils.toRectangle(rect)

    assertEquals(rect.x(), awtRect.x)
    assertEquals(rect.y(), awtRect.y)
    assertEquals(rect.width(), awtRect.width)
    assertEquals(rect.height(), awtRect.height)
  }

}
