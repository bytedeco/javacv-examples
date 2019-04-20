package flandmark;

import org.bytedeco.flandmark.FLANDMARK_Model;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.bytedeco.flandmark.global.flandmark.flandmark_detect;
import static org.bytedeco.flandmark.global.flandmark.flandmark_init;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


/**
 * JVM version of flandmark example 1:
 * https://github.com/uricamic/flandmark/blob/master/examples/example1.cpp
 */
public final class Example1 {

    private static FLANDMARK_Model loadFLandmarkModel(final File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("FLandmark model file does not exist: " + file.getAbsolutePath());
        }

        final FLANDMARK_Model model = flandmark_init("flandmark_model.dat");
        if (model == null) {
            throw new IOException("Failed to load FLandmark model from file: " + file.getAbsolutePath());
        }

        return model;
    }

    private static void show(final Mat image, final String title) {
        CanvasFrame canvas = new CanvasFrame(title, 1);
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

        canvas.showImage(converter.convert(image));
    }

    private static void detectFaceInImage(final Mat orig,
                                          final Mat input,
                                          final CascadeClassifier faceCascade,
                                          final FLANDMARK_Model model,
                                          final int[] bbox,
                                          final double[] landmarks) throws Exception {

        RectVector faces = new RectVector();
        faceCascade.detectMultiScale(input, faces);

        long nFaces = faces.size();
        System.out.println("Faces detected: " + nFaces);
        if (nFaces == 0) {
            throw new Exception("No faces detected");
        }

        for (int iface = 0; iface < nFaces; ++iface) {
            Rect rect = faces.get(iface);

            bbox[0] = rect.x();
            bbox[1] = rect.y();
            bbox[2] = rect.x() + rect.width();
            bbox[3] = rect.y() + rect.height();

            flandmark_detect(new IplImage(input), bbox, model, landmarks);

            // display landmarks
            rectangle(orig, new Point(bbox[0], bbox[1]), new Point(bbox[2], bbox[3]), new Scalar(255, 0, 0, 128));
            rectangle(orig,
                    new Point((int) model.bb().get(0), (int) model.bb().get(1)),
                    new Point((int) model.bb().get(2), (int) model.bb().get(3)), new Scalar(0, 0, 255, 128));
            circle(orig,
                    new Point((int) landmarks[0], (int) landmarks[1]), 3,
                    new Scalar(0, 0, 255, 128), CV_FILLED, 8, 0);
            for (int i = 2; i < 2 * model.data().options().M(); i += 2) {
                circle(orig, new Point((int) (landmarks[i]), (int) (landmarks[i + 1])), 3,
                        new Scalar(255, 0, 0, 128), CV_FILLED, 8, 0);
            }
        }
    }


    public static void main(String[] args) {

        final File inputImage = new File("face.jpg");
        final File faceCascadeFile = new File("haarcascade_frontalface_alt.xml");
        final File flandmarkModelFile = new File("flandmark_model.dat");

        try {
            CascadeClassifier faceCascade = new CascadeClassifier(faceCascadeFile.getCanonicalPath());

            final FLANDMARK_Model model = loadFLandmarkModel(flandmarkModelFile);
            System.out.println("Model w_cols: " + model.W_COLS());
            System.out.println("Model w_rows: " + model.W_ROWS());

            Mat image = imread(inputImage.getCanonicalPath());
            show(image, "Example 1 - original");

            Mat imageBW = new Mat();
            cvtColor(image, imageBW, CV_BGR2GRAY);
            show(imageBW, "Example 1 - BW input");

            final int[] bbox = new int[4];
            final double[] landmarks = new double[2 * model.data().options().M()];
            detectFaceInImage(image, imageBW, faceCascade, model, bbox, landmarks);

            show(image, "Example 1 - output");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
