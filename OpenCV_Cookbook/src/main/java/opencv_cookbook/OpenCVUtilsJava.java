/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook;


import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

/**
 * Helper methods that simplify use of OpenCV API.
 */
public class OpenCVUtilsJava {

    /** Load an image and show in a CanvasFrame. If image cannot be loaded the application will exit with code 1.
     *
     * @return loaded image
     */
    public Mat loadAndShowOrExit(File file) {
        return loadAndShowOrExit(file,IMREAD_COLOR);
    }

    /** Load an image and show in a CanvasFrame. If image cannot be loaded the application will exit with code 1.
     *
     * @param flags Flags specifying the color type of a loaded image:
     *              <ul>
     *              <li> `>0` Return a 3-channel color image</li>
     *              <li> `=0` Return a gray scale image</li>
     *              <li> `<0` Return the loaded image as is. Note that in the current implementation
     *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
     *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
     *              </ul>
     *              Default is gray scale.
     * @return loaded image
     */
    public static Mat loadAndShowOrExit(File file, Integer flags) {
        Mat image = loadOrExit(file, flags);
        show(image,file.getName());
        return image;
    }


    /** Load an image. If image cannot be loaded the application will exit with code 1.
     *
     * @return loaded image
     */
    public static Mat loadOrExit(File file) {
        return loadOrExit(file,IMREAD_COLOR);
    }

    /** Load an image. If image cannot be loaded the application will exit with code 1.
     *
     * @param flags Flags specifying the color type of a loaded image:
     *              <ul>
     *              <li> `>0` Return a 3-channel color image</li>
     *              <li> `=0` Return a gray scale image</li>
     *              <li> `<0` Return the loaded image as is. Note that in the current implementation
     *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
     *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
     *              </ul>
     *              Default is gray scale.
     * @return loaded image
     */
    public static Mat loadOrExit(File file, Integer flags) {
        Mat image = imread(file.getAbsolutePath(), flags);
        if(image.empty()){
            System.out.println("Couldn't load image: " + file.getAbsolutePath());
            System.exit(1);
        }
        return image;
    }

    /** Show image in a window. Closing the window will exit the application. */
    public static void show(Mat mat, String title) {
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        CanvasFrame canvas = new CanvasFrame(title, 1);
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.showImage(converter.convert(mat));
    }

    /** Show image in a window. Closing the window will exit the application. */
    public static void show(BufferedImage image, String title) {
        CanvasFrame canvas = new CanvasFrame(title, 1);
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.showImage(image);
    }

    /** Save the image to the specified file.
     *
     * The image format is chosen based on the filename extension (see `imread()` in OpenCV documentation for the list of extensions).
     * Only 8-bit (or 16-bit in case of PNG, JPEG 2000, and TIFF) single-channel or
     * 3-channel (with ‘BGR’ channel order) images can be saved using this function.
     * If the format, depth or channel order is different, use Mat::convertTo() , and cvtColor() to convert it before saving.
     *
     * @param file file to save to. File name extension decides output image format.
     * @param image image to save.
     */
    public void save(File file, Mat image) {
        imwrite(file.getAbsolutePath(), image);
    }
}
