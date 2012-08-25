/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook

import com.googlecode.javacv.cpp.opencv_core.IplImage
import ij.process._
import java.awt.image._
import java.awt.image.{BufferedImage => BI}


/** Helper methods for integration with ImageJ. */
object OpenCVImageJUtils {

    /** Convert OpenCV `IplImage` to ImageJ's ImageProcessor.
      *
      * Depending on the type input image different instance
      * of `ImageProcessor` will be created, for color images it will be `ColorProcessor`, for 8-bit gray level `ByteProcessor`.
      * Other pixel types are currently not supported.
      *
      * @param image input image.
      */
    def toImageProcessor(image: IplImage): ImageProcessor = {
        val bi = image.getBufferedImage
        bi.getType match {
            case BI.TYPE_BYTE_GRAY => new ByteProcessor(bi)
            case BI.TYPE_3BYTE_BGR => new ColorProcessor(bi)
            case t => throw new IllegalArgumentException("Unsuported BufferedImage type " + t)
        }
    }


    /** Convert OpenCV `IplImage` to ImageJ's `ColorProcessor`.
      *
      * @param image input image.
      * @throws IllegalArgumentException if `IplImage` is not a color image.
      */
    def toColorProcessor(image: IplImage): ColorProcessor = {
        val ip = toImageProcessor(image)
        if (ip.isInstanceOf[ColorProcessor]) {
            ip.asInstanceOf[ColorProcessor]
        } else {
            throw new IllegalArgumentException("Input image is not a color image.")
        }
    }


    /** Convert ImageJ's `ImageProcessor` to `BufferedImage`. */
    def toBufferedImage(ip: ImageProcessor): BufferedImage = {

        val BP = classOf[ByteProcessor]
        val CP = classOf[ColorProcessor]
        ip.getClass match {
            case BP => ip.getBufferedImage
            case CP => {
                // Create BufferedImage of RGB type that JavaCV likes
                val dest = new BufferedImage(ip.getWidth, ip.getHeight, BI.TYPE_3BYTE_BGR)
                // Easiest way to transfer the data is to draw the input image on the output image,
                // This handles all needed color representation conversions, since both are variants of
                val g = dest.getGraphics
                g.drawImage(ip.getBufferedImage, 0, 0, null)
                dest
            }
            case _ => throw new IllegalArgumentException("Unsupported ImageProcessor type: " + ip.getClass)
        }

    }


    /** Convert BufferedImage to ImageJ's ImageProcessor.
      *
      * Based on net.sf.ij.jaiio.ImagePlusCreator#createProcessor
      *
      * @param image     input image
      * @return ImageProcessor created from input BufferedImage.
      * @throws UnsupportedImageModelException when enable to create ImagePlus.
      */
    def toImageProcessor(image: BufferedImage): ImageProcessor = {
        val raster = image.getRaster
        val colorModel = image.getColorModel
        val dataBuffer = raster.getDataBuffer
        val numBanks = dataBuffer.getNumBanks
        if (numBanks > 1 && colorModel == null) {
            throw new IllegalArgumentException("Don't know what to do with image with no color model and multiple banks.")
        }

        val sampleModel = raster.getSampleModel
        if (numBanks > 1 || sampleModel.getNumBands > 1) {
            val bi = new BufferedImage(colorModel, raster, false, null)
            new ColorProcessor(bi)
        } else if (sampleModel.getSampleSize(0) < 8) {
            val bi = new BufferedImage(colorModel, raster, false, null)
            val w = image.getWidth
            val h = image.getHeight
            bi.getType match {
                case BI.TYPE_BYTE_GRAY => new ByteProcessor(bi)
                case BI.TYPE_BYTE_BINARY =>
                    val bp = new ByteProcessor(w, h)
                    val data = bi.getData
                    val p = Array(0)
                    for (y <- 0 until h; x <- 0 until w) {
                        data.getPixel(x, y, p)
                        bp.set(x, y, p(0))
                    }
                    bp.setColorModel(colorModel)
                    bp
                case _ => throw new IllegalArgumentException("Unable to process buffered image of type: " + bi.getType)
            }
        } else {
            if (dataBuffer.getOffset != 0) {
                throw new IllegalArgumentException("Expecting BufferData with no offset.")
            }
            val w = image.getWidth
            val h = image.getHeight
            import java.awt.image.{DataBuffer => DB}
            dataBuffer.getDataType match {
                case DB.TYPE_BYTE =>
                    new ByteProcessor(w, h, (dataBuffer.asInstanceOf[DataBufferByte]).getData, colorModel)
                case DB.TYPE_USHORT =>
                    new ShortProcessor(w, h, (dataBuffer.asInstanceOf[DataBufferUShort]).getData, colorModel)
                case DB.TYPE_SHORT =>
                    val pixels = (dataBuffer.asInstanceOf[DataBufferShort]).getData
                    pixels.foreach(_ + 32768)
                    new ShortProcessor(w, h, pixels, colorModel)
                case DB.TYPE_INT =>
                    new FloatProcessor(w, h, (dataBuffer.asInstanceOf[DataBufferInt]).getData)
                case DB.TYPE_FLOAT =>
                    new FloatProcessor(w, h, dataBuffer.asInstanceOf[DataBufferFloat].getData, colorModel)
                case DB.TYPE_DOUBLE =>
                    new FloatProcessor(w, h, (dataBuffer.asInstanceOf[DataBufferDouble]).getData)
                case DB.TYPE_UNDEFINED =>
                    throw new IllegalArgumentException("Pixel type is undefined.")
                case _ =>
                    throw new IllegalArgumentException("Unrecognized DataBuffer data type")
            }
        }
    }

}