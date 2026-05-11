package Controller;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.github.sarxos.webcam.Webcam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

public class FaceScanController {

    @FXML private ImageView cameraView;
    @FXML private Button captureButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private Region statusDot;
    @FXML private Region scanLine;

    private Webcam webcam;
    private volatile boolean capturing = false;
    private Thread cameraThread;
    private TranslateTransition scanAnim;

    private FaceCapturedListener faceCapturedListener;

    public interface FaceCapturedListener {
        void onFaceCaptured(byte[] imageBytes);
    }

    public void setFaceCapturedListener(FaceCapturedListener listener) {
        this.faceCapturedListener = listener;
    }

    @FXML
    public void initialize() {
        setStatus("Initialisation de la caméra...", "idle");
        startScanAnimation();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        }));

        startCameraThread();
    }

    private void startScanAnimation() {
        if (scanLine == null) return;
        scanAnim = new TranslateTransition(Duration.seconds(2.2), scanLine);
        scanAnim.setFromY(-210);
        scanAnim.setToY(210);
        scanAnim.setAutoReverse(true);
        scanAnim.setCycleCount(TranslateTransition.INDEFINITE);
        scanAnim.setInterpolator(Interpolator.EASE_BOTH);
        scanAnim.play();
    }

    private void setStatus(String text, String state) {
        Runnable r = () -> {
            if (statusLabel != null) statusLabel.setText(text);
            if (statusDot != null) {
                statusDot.getStyleClass().removeAll("idle", "scanning", "error");
                if (state != null) statusDot.getStyleClass().add(state);
            }
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private void startCameraThread() {

        cameraThread = new Thread(() -> {
            try {
                openCamera();

                capturing = true;
                setStatus("Positionnez votre visage dans le cadre", "scanning");

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
                setStatus("Erreur : caméra indisponible", "error");
            } finally {
                closeCamera();
            }
        }, "face-scan-camera");

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void openCamera() throws Exception {

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
                setStatus("Échec de la capture, réessayez", "error");
                return;
            }

            setStatus("Analyse en cours...", "scanning");
            if (captureButton != null) captureButton.setDisable(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(captured, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            if (faceCapturedListener != null) {
                faceCapturedListener.onFaceCaptured(imageBytes);
            }

        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Erreur de capture", "error");
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
        if (scanAnim != null) scanAnim.stop();
        closeCamera();
        Stage stage = (Stage) (cancelButton != null
                ? cancelButton.getScene().getWindow()
                : captureButton.getScene().getWindow());
        Platform.runLater(stage::close);
    }

    private void closeCamera() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            webcam = null;
        }
    }
}
