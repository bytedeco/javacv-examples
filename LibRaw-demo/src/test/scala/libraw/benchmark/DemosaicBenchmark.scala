package libraw.benchmark

import ij.process.{ByteProcessor, ColorProcessor, ShortProcessor}
import ij.{CompositeImage, ImagePlus, ImageStack}
import org.bytedeco.javacpp.{BytePointer, Pointer}
import org.bytedeco.libraw.global.LibRaw.*
import org.bytedeco.libraw.{LibRaw, progress_callback}

import scala.util.Using

object DemosaicBenchmark {

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

  private def dcrawImageToImagePlus(output_bps: Int, lr: LibRaw): ImagePlus = {

    val err = Array[Int](0)
    Using.resource(lr.dcraw_make_mem_image(err)) { im =>
      handleError(err.head, "Error while calling `dcraw_make_mem_image` ")

      assert(im.`type`().value == LibRaw_image_formats.LIBRAW_IMAGE_BITMAP.value)

      val height = im.height()
      val width  = im.width()
      val colors = im.colors()

      output_bps match {
        case 8 =>
          val data = new Array[Byte](width * height * colors)
          im.data().get(data)
          val slicePixels = Array.fill(colors)(new Array[Byte](width * height))
          for (i <- 0 until width * height) {
            for (c <- 0 until colors) {
              slicePixels(c)(i) = data(i * colors + c)
            }
          }

          if (colors == 3) {
            val cp = new ColorProcessor(width, height)
            cp.setRGB(slicePixels(0), slicePixels(1), slicePixels(2))
            new ImagePlus("", cp)
          } else {
            val imageStack = new ImageStack(width, height)
            for ((pixels, i) <- slicePixels.zipWithIndex) {
              imageStack.addSlice(s"${i + 1}", new ByteProcessor(width, height, pixels))
            }
            new ImagePlus("", imageStack)
          }

        case 16 =>
          val data = new Array[Byte](width * height * colors * 2)
          im.data().get(data)

          val slicePixels = Array.fill(colors)(new Array[Short](width * height))
          for (y <- 0 until height) {
            val ySrcOffset = y * width * colors * 2
            val yDstOffset = y * width
            for (x <- 0 until width) {
              val xSrcOffset = x * colors * 2 + ySrcOffset
              val xDstOffset = x + yDstOffset
              for (c <- 0 until colors) {
                val cSrcOffset = c * 2 + xSrcOffset
                val p0         = data(cSrcOffset + 0) & 0xFF
                val p1         = data(cSrcOffset + 1) & 0xFF
                val p          = (((p1 << 8) + p0) & 0xFFFF).toShort
                slicePixels(c)(xDstOffset) = p
              }
            }
          }

          val imageStack = new ImageStack(width, height)
          val sliceNames = if (colors == 3) Seq("Red", "Green", "Blue") else (1 to colors).map(_.toString)
          for ((pixels, name) <- slicePixels.zip(sliceNames)) {
            imageStack.addSlice(name, new ShortProcessor(width, height, pixels, null))
          }

          val dst = new ImagePlus("", imageStack)
          if (colors <= CompositeImage.MAX_CHANNELS)
            new CompositeImage(dst, if (colors == 3) CompositeImage.COMPOSITE else CompositeImage.GRAYSCALE)
          else
            dst
      }
    }
  }


  def debayer(bp: ByteProcessor): ImagePlus = {

    val buffer: Array[Byte] = bp.getPixels.asInstanceOf[Array[Byte]]

    Using.resource(new LibRaw()) { rp =>

      // Set progress callback
      //      rp.set_progress_handler(myCallback, new IntPointer(1L))


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

      // TODO: convert to ColorProcessor

      //      // Save
      //      handleError(rp.dcraw_ppm_tiff_writer("tmp/open_bayer-test.tif"), "dcraw_ppm_tiff_writer")

      val imp = dcrawImageToImagePlus(8, rp)

      rp.recycle()

      imp
    }

  }

  def main(args: Array[String]): Unit = {


    val bp = new ByteProcessor(2048, 1536)

    debayer(bp)

    val helper = new BenchmarkHelper(testIter = 5)


    val nbIter = 5

    for (i <- 0 until nbIter) {
      println()
      println(s"Run ${i + 1}")

      helper.measure[ByteProcessor, ImagePlus](
        tag = "debayer          ", bp,
        debayer)
    }

    println()
    println("Min time:")
    helper.printResults()

  }
}
