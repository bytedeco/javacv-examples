package libraw.examples

import ij.IJ
import org.bytedeco.javacpp.{BytePointer, IntPointer, Pointer}
import org.bytedeco.libraw.global.LibRaw.LibRaw_openbayer_patterns.LIBRAW_OPENBAYER_RGGB
import org.bytedeco.libraw.global.LibRaw.{LibRaw_errors, LibRaw_openbayer_patterns, LibRaw_progress, libraw_strerror, libraw_strprogress}
import org.bytedeco.libraw.{LibRaw, libraw_output_params_t, progress_callback}

import scala.util.Using

object DemosaicDemo {

  def handleError(err: Int, prefix: String): Unit = {
    if (err != LibRaw_errors.LIBRAW_SUCCESS.value) {
      val msg = Using.resource(libraw_strerror(err))(_.getString)
      throw new Exception(prefix + " : " + msg + " [" + err + "]")
    }
  }

  class MyCallback extends progress_callback {
    override def call(data: Pointer, stage: LibRaw_progress, iteration: Int, expected: Int): Int = {
      println(s"Callback: ${libraw_strprogress(stage.value)},  iteration: $iteration / $expected")
      0
    }
  }

  def main(args: Array[String]): Unit = {

    val myCallback = new MyCallback()

    // Load raw data from a Bayer pattern image
    val imp                 = IJ.openImage("data/Lighthouse_bayerBG8.png")
    val bp                  = imp.getProcessor
    val buffer: Array[Byte] = bp.getPixels.asInstanceOf[Array[Byte]]

    // Process
    Using.resource(new LibRaw()) { rp =>

      // Set progress callback
      rp.set_progress_handler(myCallback, new IntPointer(1L))


      // Prepare parameters for loading the raw data
      val data          : BytePointer = new BytePointer(buffer.length).put(buffer, 0, buffer.length)
      val datalen       : Int         = buffer.length
      val _raw_width    : Short       = bp.getWidth.toShort
      val _raw_height   : Short       = bp.getHeight.toShort
      val _left_margin  : Short       = 0
      val _top_margin   : Short       = 0
      val _right_margin : Short       = 0
      val _bottom_margin: Short       = 0
      val procflags     : Byte        = 0
      val bayer_pattern : Byte        = LibRaw_openbayer_patterns.LIBRAW_OPENBAYER_BGGR.value.toByte
      val unused_bits   : Int         = 0
      val otherflags    : Int         = 0
      val black_level   : Int         = 0

      // Load raw data
      val ret = rp.open_bayer(data, datalen, _raw_width, _raw_height, _left_margin, _top_margin, _right_margin,
                              _bottom_margin, procflags, bayer_pattern, unused_bits, otherflags, black_level)
      handleError(ret, "open_bayer")

      // We will write output as TIFF
      rp.imgdata().params.output_tiff(1)
      // Use DHT(11) interpolation algorithm
      rp.imgdata().params.user_qual(11)

      // Unpack
      handleError(rp.unpack(), "unpack")

      // Process
      handleError(rp.dcraw_process(), "dcraw_process")

      // Save
      handleError(rp.dcraw_ppm_tiff_writer("tmp/open_bayer-test.tif"), "dcraw_ppm_tiff_writer")

      rp.recycle()
    }
  }
}
