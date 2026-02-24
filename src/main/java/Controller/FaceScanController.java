package Controller;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import com.github.sarxos.webcam.Webcam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.List;

public class FaceScanController {

    @FXML
    private ImageView cameraView;

    @FXML
    private Button captureButton;

    private Webcam webcam;
    private volatile boolean capturing = false;
    private Thread cameraThread;

    private FaceCapturedListener faceCapturedListener;

    public interface FaceCapturedListener {
        void onFaceCaptured(byte[] imageBytes);
    }

    public void setFaceCapturedListener(FaceCapturedListener listener) {
        this.faceCapturedListener = listener;
    }

    @FXML
    public void initialize() {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        }));

        startCameraThread();
    }

    private void startCameraThread() {

        cameraThread = new Thread(() -> {
            try {
                openCamera();

                capturing = true;

                while (capturing) {
                    BufferedImage image = webcam.getImage();

                    if (image != null) {
                        Image fxImage = SwingFXUtils.toFXImage(image, null);
                        Platform.runLater(() -> cameraView.setImage(fxImage));
                    }

                    Thread.sleep(33);
                }

            } catch (Exception e) {
                System.err.println("Camera error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                closeCamera();
            }
        });

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void openCamera() throws Exception {

        // 🔎 Print all detected webcams
        System.out.println("====== AVAILABLE WEBCAMS ======");
        List<Webcam> webcams = Webcam.getWebcams();
        for (Webcam cam : webcams) {
            System.out.println(" - " + cam.getName());
        }
        System.out.println("================================");

        webcam = Webcam.getDefault();

        if (webcam == null) {
            throw new Exception("No webcam detected!");
        }

        System.out.println("Using webcam: " + webcam.getName());

        // Use first supported resolution instead of forcing QVGA
        Dimension[] sizes = webcam.getViewSizes();
        if (sizes.length > 0) {
            webcam.setViewSize(sizes[0]);
        }

        webcam.open();
    }

    @FXML
    private void handleCapture() {

        try {
            if (webcam == null || !webcam.isOpen()) return;

            BufferedImage captured = webcam.getImage();
            if (captured == null) {
                System.err.println("Captured image is null!");
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(captured, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            if (faceCapturedListener != null) {
                faceCapturedListener.onFaceCaptured(imageBytes);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stopCameraAndCloseWindow();
        }
    }

    @FXML
    public void handleClose() {
        stopCameraAndCloseWindow();
    }

    private void stopCameraAndCloseWindow() {
        capturing = false;
        closeCamera();
        Stage stage = (Stage) captureButton.getScene().getWindow();
        Platform.runLater(stage::close);
    }

    private void closeCamera() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            webcam = null;
        }
    }
}