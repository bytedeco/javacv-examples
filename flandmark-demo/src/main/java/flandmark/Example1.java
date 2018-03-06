package flandmark;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.naming.OperationNotSupportedException;
import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.bytedeco.javacpp.flandmark.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;

/**
 * JVM version of flandmark example 1:
 * https://github.com/uricamic/flandmark/blob/master/examples/example1.cpp
 */
public final class Example1 {

    private static CvHaarClassifierCascade loadFaceCascade(final File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("Face cascade file does not exist: " + file.getAbsolutePath());
        }

        final CvHaarClassifierCascade faceCascade =
                cvLoadHaarClassifierCascade(file.getCanonicalPath(), cvSize(0, 0));

        if (faceCascade == null) {
            throw new IOException("Failed to load face cascade from file: " + file.getAbsolutePath());
        }

        return faceCascade;
    }

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

    private static IplImage loadImage(File file) throws IOException {
        // Verify file
        if (!file.exists()) {
            throw new FileNotFoundException("Image file does not exist: " + file.getAbsolutePath());
        }
        // Read input image
        IplImage image = cvLoadImage(file.getAbsolutePath());
        if (image == null) {
            throw new IOException("Couldn't load image: " + file.getAbsolutePath());
        }
        return image;
    }

    private static void show(final IplImage image, final String title) {
        final IplImage image1 = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, image.nChannels());
        cvCopy(image, image1);
        CanvasFrame canvas = new CanvasFrame(title, 1);
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

        canvas.showImage(converter.convert(image1));
    }

    private static void detectFaceInImage(final IplImage orig,
                                          final IplImage input,
                                          final CvHaarClassifierCascade cascade,
                                          final FLANDMARK_Model model,
                                          final int[] bbox,
                                          final double[] landmarks) throws Exception {

        CvMemStorage storage = cvCreateMemStorage(0);
        cvClearMemStorage(storage);
        try {
            double search_scale_factor = 1.1;
            int flags = CV_HAAR_DO_CANNY_PRUNING;
            CvSize minFeatureSize = cvSize(40, 40);
            CvSeq rects = cvHaarDetectObjects(input, cascade, storage, search_scale_factor, 2, flags, minFeatureSize, cvSize(0, 0));
            int nFaces = rects.total();
            if (nFaces == 0) {
                throw new Exception("No faces detected");
            }


            for (int iface = 0; iface < nFaces; ++iface) {
                BytePointer elem = cvGetSeqElem(rects, iface);
                CvRect rect = new CvRect(elem);

                bbox[0] = rect.x();
                bbox[1] = rect.y();
                bbox[2] = rect.x() + rect.width();
                bbox[3] = rect.y() + rect.height();

                flandmark_detect(input, bbox, model, landmarks);

                // display landmarks
                cvRectangle(orig, cvPoint(bbox[0], bbox[1]), cvPoint(bbox[2], bbox[3]), CV_RGB(255, 0, 0));
                cvRectangle(orig,
                        cvPoint((int) model.bb().get(0), (int) model.bb().get(1)),
                        cvPoint((int) model.bb().get(2), (int) model.bb().get(3)), CV_RGB(0, 0, 255));
                cvCircle(orig,
                        cvPoint((int) landmarks[0], (int) landmarks[1]), 3, CV_RGB(0, 0, 255), CV_FILLED, 8, 0);
                for (int i = 2; i < 2 * model.data().options().M(); i += 2) {
                    cvCircle(orig, cvPoint((int) (landmarks[i]), (int) (landmarks[i + 1])), 3, CV_RGB(255, 0, 0), CV_FILLED, 8, 0);

                }

            }

        } finally {
            cvReleaseMemStorage(storage);
        }
    }


    public static void main(String[] args) throws OperationNotSupportedException {

        final File inputImage = new File("face.jpg");
        final File faceCascadeFile = new File("haarcascade_frontalface_alt.xml");
        final File flandmarkModelFile = new File("flandmark_model.dat");

        try {
            final CvHaarClassifierCascade faceCascade = loadFaceCascade(faceCascadeFile);
            System.out.println("Count: " + faceCascade.count());

            final FLANDMARK_Model model = loadFLandmarkModel(flandmarkModelFile);
            System.out.println("Model w_cols: " + model.W_COLS());
            System.out.println("Model w_rows: " + model.W_ROWS());

            final IplImage image = loadImage(inputImage);
            show(image, "Example 1 - original");

            final IplImage imageBW = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, 1);
            cvCvtColor(image, imageBW, CV_BGR2GRAY);
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
