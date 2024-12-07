package libraw.examples

import scala.util.Using

object LibRawDemo {


  def main(args: Array[String]): Unit = {
    
    System.out.println("LibRaw.version(): " + RawProcessor.libRawVersion)

    val srcFile = if (args.length > 0) args(0) else "data/IMG_5604.CR2"

    Using.resource(new RawProcessor()) { rawProcessor =>

      val params = RawProcessor.Params(gamma = Option((1, 1)),
                                       no_auto_bright = true,
                                       output_bps = RawProcessor.BPS.BPS16,
                                       use_camera_wb = false,
                                       use_auto_wb = false,
                                       output_color = RawProcessor.ColorSpace.raw,
                                       no_auto_scale = true,
                                       output_tiff = true)

      val dstFile = s"tmp/test.${params.ext}"
      rawProcessor.process(srcFile, dstFile, params)
    }

    System.out.println("Done")
  }
}
