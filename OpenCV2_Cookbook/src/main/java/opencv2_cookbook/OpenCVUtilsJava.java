package opencv2_cookbook;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.bytedeco.javacpp.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

/**
 * Helper methods that simplify use of OpenCV API.
 */
public class OpenCVUtilsJava {

    /** Load an image and show in a CanvasFrame. If image cannot be loaded the application will exit with code 1.
     *
     * @return loaded image
     */
    public opencv_core.Mat loadAndShowOrExit(File file){
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
    public static opencv_core.Mat loadAndShowOrExit(File file, Integer flags){
        opencv_core.Mat image = loadOrExit(file, flags);
        show(image,file.getName());
        return image;
    }


    /** Load an image. If image cannot be loaded the application will exit with code 1.
     *
     * @return loaded image
     */
    public static opencv_core.Mat loadOrExit(File file) {
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
    public static opencv_core.Mat loadOrExit(File file, Integer flags) {
        opencv_core.Mat image = imread(file.getAbsolutePath(), flags);
        if(image.empty()){
            System.out.println("Couldn't load image: " + file.getAbsolutePath());
            System.exit(1);
        }
        return image;
    }

    /** Show image in a window. Closing the window will exit the application. */
    public static void show(opencv_core.Mat mat, String title) {
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
}
