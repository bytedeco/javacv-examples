package opencv_cookbook.chapter01;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;
import java.util.Optional;

import static opencv_cookbook.OpenCVUtilsJava.toFXImage;
import static org.bytedeco.opencv.global.opencv_core.flip;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;

/**
 * The last section in the chapter 1 of the Cookbook demonstrates how to create a simple GUI application.
 * <p>
 * The Cookbook is using Qt GUI Toolkit. This example is using JavaFX to create a similar application.
 * <p>
 * The application has two buttons on the left "Open Image" and "Process".
 * The opened image is displayed in the middle.
 * When "Process" button is pressed the image is flipped upside down and its red and blue channels are swapped.
 * <p>
 * Note, in SBT use "Ex2MyFirstGUIFXAppJavaLauncher" to start this application.
 */
public class Ex2MyFirstGUIFXAppJava extends Application {

    // Variable for holding loaded image
    private final ObjectProperty<Optional<Mat>> imageMat = new SimpleObjectProperty<>(Optional.empty());

    private final FileChooser fileChooser = new FileChooser();

    private ImageView imageView = null;
    private Stage stage = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        stage.setTitle("My First GUI FX (Java)");

        BorderPane root = new BorderPane();

        Button openImageButton = new Button("Open Image");
        openImageButton.setOnAction(actionEvent -> Ex2MyFirstGUIFXAppJava.this.onOpenImage());
        openImageButton.setMaxWidth(Double.MAX_VALUE);

        Button processButton = new Button("Process");
        processButton.setOnAction(actionEvent -> Ex2MyFirstGUIFXAppJava.this.onProcess());
        processButton.disableProperty().bind(imageMat.isEqualTo(Optional.empty()));
        processButton.setMaxWidth(Double.MAX_VALUE);

        VBox leftPane = new VBox();
        leftPane.getChildren().add(openImageButton);
        leftPane.getChildren().add(processButton);

        root.setLeft(leftPane);

        imageView = new ImageView();

        root.setCenter(imageView);

        stage.setScene(new Scene(root, 640, 480));
        stage.show();
    }

    void onOpenImage() {
        openImage().ifPresent(mat -> {
            imageMat.setValue(Optional.of(mat));
            imageView.setImage(toFXImage(mat));
        });
    }

    void onProcess() {
        imageMat.get().ifPresent(mat -> {
                    processImage(mat);
                    imageView.setImage(toFXImage(mat));
                }
        );

        if (imageMat.get().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle("Process");
            alert.setHeaderText("Image not opened");
            alert.showAndWait();
        }
    }


    /**
     * Ask user for location and open new image.
     */
    private Optional<Mat> openImage() {
        // Ask user for the location of the image file
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            // Load the image
            String path = file.getAbsolutePath();
            Mat newImage = imread(path);
            if (!newImage.empty()) {
                return Optional.of(newImage);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(stage);
                alert.setTitle("Open Image");
                alert.setHeaderText("Cannot open image file: " + path);
                alert.showAndWait();
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Process image in place.
     */
    private static void processImage(Mat src) {
        // Flip upside down
        flip(src, src, 0);
        // Swap red and blue channels
        cvtColor(src, src, COLOR_BGR2RGB);
    }
}
