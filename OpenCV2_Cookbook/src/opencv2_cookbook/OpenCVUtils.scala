/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook

import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_features2d.KeyPoint
import com.googlecode.javacv.cpp.opencv_highgui._

import java.awt._
import java.awt.image.BufferedImage
import java.awt.geom.Ellipse2D
import java.io.{FileNotFoundException, IOException, File}
import javax.swing.JFrame


object OpenCVUtils {

    /**
     * Load an image and show in a CanvasFrame.
     *
     * If image cannot be loaded the application will exit with code 1.
     *
     * @param file image file
     * @param flags Flags specifying the color type of a loaded image:
     *              <ul>
     *              <li> `>0` Return a 3-channel color image</li>
     *              <li> `=0` Return a grayscale image</li>
     *              <li> `<0` Return the loaded image as is. Note that in the current implementation
     *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
     *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
     *              </ul>
     *              Default is grayscale.
     * @return Loaded image
     */
    def loadAndShowOrExit(file: File, flags: Int = CV_LOAD_IMAGE_GRAYSCALE): IplImage = {
        try {
            val image = loadOrExit(file, flags)
            show(image, file.getName)
            image
        }
        catch {
            case ex: IOException => {
                println("Couldn't load image: " + file.getAbsolutePath)
                sys.exit(1)
            }
        }
    }


    /**
     * Load an image, if image cannot be loaded the application will exit with code 1.
     *
     * @param file image file
     * @param flags Flags specifying the color type of a loaded image:
     *              <ul>
     *              <li> `>0` Return a 3-channel color image</li>
     *              <li> `=0` Return a grayscale image</li>
     *              <li> `<0` Return the loaded image as is. Note that in the current implementation
     *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
     *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
     *              </ul>
     *              Default is grayscale.
     * @throws FileNotFoundException when file does not exist
     * @throws IOException when image cannot be read
     * @return Loaded image
     */
    def loadOrExit(file: File, flags: Int = CV_LOAD_IMAGE_GRAYSCALE): IplImage = {
        // Verify file
        if (!file.exists()) {
            throw new FileNotFoundException("Image file does not exist: " + file.getAbsolutePath)
        }
        // Read input image
        val image = cvLoadImage(file.getAbsolutePath, flags)
        if (image == null) {
            throw new IOException("Couldn't load image: " + file.getAbsolutePath)
        }
        image
    }


    /**
     * Load an image and show in a CanvasFrame. If image cannot be loaded the application will exit with code 1.
     * @param flags Flags specifying the color type of a loaded image:
     *              <ul>
     *              <li> `>0` Return a 3-channel color image</li>
     *              <li> `=0` Return a grayscale image</li>
     *              <li> `<0` Return the loaded image as is. Note that in the current implementation
     *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
     *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
     *              </ul>
     *              Default is grayscale.
     * @return loaded image
     */
    def loadMatAndShowOrExit(file: File, flags: Int = CV_LOAD_IMAGE_GRAYSCALE): CvMat = {
        // Read input image
        val image = loadMatOrExit(file, flags)
        show(image, file.getName)
        image
    }


    /**
     * Load an image. If image cannot be loaded the application will exit with code 1.
     * @param flags Flags specifying the color type of a loaded image:
     *              <ul>
     *              <li> `>0` Return a 3-channel color image</li>
     *              <li> `=0` Return a grayscale image</li>
     *              <li> `<0` Return the loaded image as is. Note that in the current implementation
     *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
     *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
     *              </ul>
     *              Default is grayscale.
     * @return loaded image
     */
    def loadMatOrExit(file: File, flags: Int = CV_LOAD_IMAGE_GRAYSCALE): CvMat = {
        // Read input image
        val image = cvLoadImageM(file.getAbsolutePath, flags)
        if (image == null) {
            println("Couldn't load image: " + file.getAbsolutePath)
            sys.exit(1)
        }
        image
    }


    /**
     * Show image in a window. Closing the window will exit the application.
     */
    def show(image: IplImage, title: String) {
        val canvas = new CanvasFrame(title)
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        canvas.showImage(image)
    }


    /**
     * Show image in a window. Closing the window will exit the application.
     */
    def show(mat: CvMat, title: String) {
        val canvas = new CanvasFrame(title)
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        canvas.showImage(toIplImage(mat))
    }


    /**
     * Show image in a window. Closing the window will exit the application.
     */
    def show(image: Image, title: String) {
        val canvas = new CanvasFrame(title)
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        canvas.showImage(image)
    }


    /**
     * Draw circles at feature point locations on an image.
     */
    def drawOnImage(image: IplImage, points: CvPoint2D32f): Image = {
        //        val color = CvScalar.WHITE
        //        val radius: Int = 3
        //        val thickness: Int = 2
        //        points.foreach(p => {
        //            println("(" + p.x + ", " + p.y + ")")
        //            val center = new CvPoint(new CvPoint2D32f(p.x, p.y))
        //            cvCircle(image, center, radius, color, thickness, 8, 0)
        //        })

        // OpenCV drawing seems to crash a lot, so use Java2D
        val radius = 3;
        val bi = image.getBufferedImage
        val g2d = bi.getGraphics.asInstanceOf[Graphics2D]
        val w = radius * 2;
        g2d.setColor(Color.WHITE)

        val n = points.capacity()
        println("n: " + n)
        for (i <- 0 until n) {
            val p = points.position(i)
            g2d.draw(new Ellipse2D.Double(p.x - radius, p.y - radius, w, w))
        }

        bi
    }


    /**
     * Draw circles at key point locations on an image.
     * Circle radius is proportional to key point size.
     */
    def drawOnImage(image: IplImage, points: Array[KeyPoint]): Image = {

        // OpenCV drawing seems to crash a lot, so use Java2D
        val minR = 2
        val bi = image.getBufferedImage
        val g2d = bi.getGraphics.asInstanceOf[Graphics2D]
        g2d.setColor(Color.WHITE)

        points.foreach(p => {
            val radius = p.size() / 2
            val r = if (radius == Float.NaN || radius < minR) minR else radius
            val pt = p.pt
            g2d.draw(new Ellipse2D.Double(pt.x - r, pt.y - r, r * 2, r * 2))
        })

        bi
    }


    /**
     * Draw a shape on an image.
     * @param image input image
     * @param overlay shape to draw
     * @param color color to use
     * @return new image with drawn overlay
     */
    def drawOnImage(image: IplImage, overlay: Shape, color: Color = Color.RED): Image = {
        val bi = image.getBufferedImage
        val canvas = new BufferedImage(bi.getWidth, bi.getHeight, BufferedImage.TYPE_INT_RGB)
        val g = canvas.createGraphics()
        g.drawImage(bi, 0, 0, null)
        g.setPaint(color)
        g.draw(overlay)
        g.dispose()
        canvas
    }


    /**
     * Save the image to the specified file.
     * The image format is chosen based on the filename extension (see `imread()` in OpenCV documentation for the list of extensions).
     * Only 8-bit (or 16-bit in case of PNG, JPEG 2000, and TIFF) single-channel or
     * 3-channel (with ‘BGR’ channel order) images can be saved using this function.
     * If the format, depth or channel order is different, use Mat::convertTo() , and cvtColor() to convert it before saving.
     *
     * @param file file to save to. File name extension decides output image format.
     * @param image image to save.
     */
    def save(file: File, image: CvArr) {
        cvSaveImage(file.getAbsolutePath, image)
    }

    /**
     * Scale input image pixel values so the minimum value is 0 and maximum is 1.
     * This mostly used to prepare a gray scale floating point images for display.
     * @param image input image
     * @return 32 bit floating point image (depth = `IPL_DEPTH_32F`).
     */
    def scaleTo01(image: IplImage): IplImage = {
        val min = Array(Double.MaxValue)
        val max = Array(Double.MinValue)
        cvMinMaxLoc(image, min, max, null, null, null)
        val scale = 1 / (max(0) - min(0))
        val imageScaled = cvCreateImage(cvGetSize(image), IPL_DEPTH_32F, image.nChannels)
        cvConvertScale(image, imageScaled, scale, 0)
        imageScaled
    }


    /**
     * Convert in `CvMat` object to `IplImage`.
     */
    def toIplImage(mat: CvMat): IplImage = {
        val image = cvCreateImage(mat.cvSize(), mat.elemSize(), 1)
        cvGetImage(mat, image)
        image
    }

    /**
     * Convert in `IplImage` object to `CvMat`.
     *
     */
    def toCvMat(image: IplImage): CvMat = {
        cvGetMat(image, new CvMat(), null, 0)
    }


    /**
     * Convert `IplImage` to one where pixels are represented as 32 floating point numbers (`IPL_DEPTH_32F`).
     * It creates a copy of the input image.
     * @param src input image.
     * @return copy of the input with pixels values represented as 32 floating point numbers
     */
    def toIplImage32F(src: IplImage): IplImage = {
        val dest = cvCreateImage(cvGetSize(src), IPL_DEPTH_32F, src.nChannels)
        cvConvertScale(src, dest, 1, 0)
        dest
    }

    /**
     * Convert `IplImage` to one where pixels are represented as 8 bit unsigned integers (`IPL_DEPTH_8U`).
     * It creates a copy of the input image.
     * @param src input image.
     * @return copy of the input with pixels values represented as 32 floating point numbers
     */
    def toIplImage8U(src: IplImage, doScaling: Boolean = true): IplImage = {
        val min = Array(Double.MaxValue)
        val max = Array(Double.MinValue)
        cvMinMaxLoc(src, min, max, null, null, null)
        val (scale, offset) =
            if (doScaling) {
                (255 / (max(0) - min(0)), -min(0))
            } else {
                (1d, 0d)
            }


        val dest = cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, src.nChannels)
        cvConvertScale(src, dest, scale, offset)
        dest
    }


    /**
     * Create an `IplROI`.
     * @param x top left corner of the ROI
     * @param y top left corner of the ROI
     * @param width width of the ROI
     * @param height height of the ROI
     * @return new IplROI object.
     */
    def toIplROI(x: Int, y: Int, width: Int, height: Int): IplROI = {
        toIplROI(new Rectangle(x, y, width, height))
    }

    /**
     * Convert a rectangle to `IplROI`.
     * @param r rectangle defining location of an ROI.
     * @return new IplROI object.
     */
    def toIplROI(r: Rectangle): IplROI = {
        val roi = new IplROI()
        roi.xOffset(r.x)
        roi.yOffset(r.y)
        roi.width(r.width)
        roi.height(r.height)
        roi
    }

    /**
     * Convert `CvRect` to AWT `Rectangle`.
     */
    def toRectangle(rect: CvRect): Rectangle = {
        new Rectangle(rect.x, rect.y, rect.width, rect height)
    }
}