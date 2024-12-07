package libraw.examples

import libraw.examples.RawProcessor.Params
import org.bytedeco.libraw.global.LibRaw.{LibRaw_errors, libraw_strerror}
import org.bytedeco.libraw.{LibRaw, libraw_output_params_t}

import scala.util.Using

object RawProcessor {
  def libRawVersion: String = Using.resource(LibRaw.version)(_.getString)

  enum ColorSpace(val value: Int):
    case raw extends ColorSpace(0)
    case sRGB extends ColorSpace(1)
    case Adobe extends ColorSpace(2)
    case Wide extends ColorSpace(3)
    case ProPhoto extends ColorSpace(4)
    case XYZ extends ColorSpace(5)
    case ACES extends ColorSpace(6)

  enum BPS(val value: Int):
    case BPS8 extends BPS(8)
    case BPS16 extends BPS(16)

  case class Params( // demosaic_algorithm = None,
                     half_size              : Boolean = false,
                     four_color_rgb         : Boolean = false,
                     dcb_iterations         : Int = 0,
                     dcb_enhance            : Boolean = false,
                     //                    fbdd_noise_reduction = FBDDNoiseReductionMode.Off,
                     //                    noise_thr = None,
                     median_filter_passes   : Int = 0,
                     use_camera_wb          : Boolean = false,
                     use_auto_wb            : Boolean = false,
                     //                     user_wb = None,
                     output_color           : ColorSpace = ColorSpace.sRGB,
                     output_bps             : BPS = BPS.BPS8,
                     //                     user_flip = None,
                     //                     user_black = None,
                     //                     user_sat = None,
                     no_auto_bright         : Boolean = false,
                     //                     auto_bright_thr = None,
                     adjust_maximum_thr     : Float = 0.75,
                     bright                 : Float = 1.0,
                     //                     highlight_mode = HighlightMode.Clip,
                     //                     exp_shift = None,
                     exp_preserve_highlights: Float = 0.0,
                     no_auto_scale          : Boolean = false,
                     gamma                  : Option[(Double, Double)] = None,
                     //                     chromatic_aberration = None,
                     //                     bad_pixels_path = None,
                     output_tiff            : Boolean = false
  ) {
    def applyTo(params: libraw_output_params_t): Unit = {

      params.half_size(toInt(half_size))
      params.four_color_rgb(toInt(four_color_rgb))
      params.dcb_iterations(dcb_iterations)
      params.dcb_enhance_fl(toInt(dcb_enhance))
      params.med_passes(median_filter_passes)
      params.use_camera_wb(toInt(use_camera_wb))
      params.use_auto_wb(toInt(use_auto_wb))
      //
      params.output_color(output_color.value)
      params.output_bps(output_bps.value)
      //
      params.no_auto_bright(toInt(no_auto_bright))
      //
      params.adjust_maximum_thr(adjust_maximum_thr)
      params.bright(bright)
      //
      params.exp_preser(exp_preserve_highlights)
      params.no_auto_scale(toInt(no_auto_scale))
      gamma.foreach { (g0, g1) =>
        params.gamm(0, g0)
        params.gamm(1, g1)
      }
      //
      params.output_tiff(toInt(output_tiff))
    }

    val ext: String = if output_tiff then "tif" else "ppm"


    private def toInt(v: Boolean): Int = if v then 1 else 0
  }

}

class RawProcessor extends AutoCloseable {

  private var libRaw: Option[LibRaw] = None

  /**
   * @throws Exception if error code is other than success
   */
  private def handleError(err: Int, prefix: String): Unit = {
    if (err != LibRaw_errors.LIBRAW_SUCCESS.value) {
      val msg = Using.resource(libraw_strerror(err))(_.toString)
      throw new Exception(prefix + " : " + msg)
    }
  }

  def process(srcFile: String, dstFile: String, params: Params = Params()): Unit = {

    // TODO: this will be created when opening the file
    libRaw = Option(new LibRaw())

    libRaw match {
      case Some(r) =>
        params.applyTo(r.imgdata.params)

        //   if (verbosity > 1)
        //    RawProcessor.set_progress_handler(my_progress_callback,
        //                                      (void *)"Sample data passed");

        System.out.println("Reading: " + srcFile)
        var err = r.open_file(srcFile)
        handleError(err, "Cannot read " + srcFile)


        System.out.println("Unpacking: " + srcFile)
        err = r.unpack()
        handleError(err, "Cannot unpack " + srcFile)

        System.out.println("Processing")
        err = r.dcraw_process
        handleError(err, "Cannot process " + srcFile)

        val dstFile = s"tmp/test.${params.ext}"
        System.out.println("Writing file: " + dstFile)
        err = r.dcraw_ppm_tiff_writer(dstFile)
        handleError(err, "Cannot write " + srcFile)
      case None =>
        throw new IllegalStateException("")
    }

  }

  override def close(): Unit = {
    System.out.println("Cleaning up")
    libRaw.foreach { r =>
      r.recycle()
      r.close()
    }
    libRaw = None
  }
}
