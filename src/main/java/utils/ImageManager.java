package utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Gestionnaire d'images pour les équipements — aligné avec le projet Symfony.
 *
 * Les images sont stockées dans le même dossier que celui utilisé par
 * VichUploaderBundle côté Symfony :
 *   {SYMFONY_PROJECT}/public/uploads/equipements
 *
 * Le nom du fichier (et lui seul) est persisté en base dans la colonne
 * `equipements.image_filename`. Les deux applications partagent ainsi le
 * même stockage physique et la même convention de nommage.
 */
public class ImageManager {

    /**
     * Dossier physique où sont stockées les images. Doit pointer vers le même
     * dossier que celui configuré dans `config/packages/vich_uploader.yaml`
     * du projet Symfony associé.
     */
    private static final Path UPLOAD_DIR = Paths.get(
            "C:", "Users", "SBS", "Documents",
            "ESPRIT-PIWEB-3A30-2026-AGRICORE", "public", "uploads", "equipements");

    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        try { Files.createDirectories(UPLOAD_DIR); }
        catch (IOException ignored) {}
    }

    /**
     * Copie le fichier source dans le dossier d'upload partagé et renvoie
     * le nom de fichier généré (à stocker dans `equipements.image_filename`).
     * Le nom suit la convention de SmartUniqueNamer de Vich :
     * `{slug}-{hex aléatoire}.{ext}`.
     */
    public static String storeImage(String nomEquipement, File sourceFichier) throws IOException {
        if (sourceFichier == null) return null;
        Files.createDirectories(UPLOAD_DIR);

        String ext = obtenirExtension(sourceFichier.getName());
        String slug = slug(nomEquipement);
        if (slug.isEmpty()) slug = "equipement";

        String filename = slug + "-" + randomHex(11) + ext;
        Path target = UPLOAD_DIR.resolve(filename);
        Files.copy(sourceFichier.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    /** Supprime le fichier physique correspondant à un nom de fichier stocké en base. */
    public static void deleteImage(String filename) {
        if (filename == null || filename.isBlank()) return;
        try { Files.deleteIfExists(UPLOAD_DIR.resolve(filename)); }
        catch (IOException ignored) {}
    }

    /** Renvoie le chemin absolu de l'image (ou null si introuvable). */
    public static String getImagePath(String filename) {
        if (filename == null || filename.isBlank()) return null;
        Path p = UPLOAD_DIR.resolve(filename);
        return Files.exists(p) ? p.toAbsolutePath().toString() : null;
    }

    public static boolean hasImage(String filename) {
        return getImagePath(filename) != null;
    }

    /**
     * Resize an image (loaded from a File) to fit within {@code maxDim}x{@code maxDim}
     * and return JPEG-encoded bytes ready for storage in `equipements.image`.
     * Keeps the BLOB compact (under MySQL's default `max_allowed_packet`).
     */
    public static byte[] readAndResizeForBlob(File source, int maxDim) throws IOException {
        if (source == null) return null;
        java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(source);
        if (src == null) throw new IOException("Format d'image non reconnu : " + source.getName());
        double ratio = Math.min((double) maxDim / src.getWidth(), (double) maxDim / src.getHeight());
        if (ratio > 1.0) ratio = 1.0;
        int w = Math.max(1, (int) (src.getWidth()  * ratio));
        int h = Math.max(1, (int) (src.getHeight() * ratio));
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(
                w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(out, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * Build a JavaFX vignette from raw image bytes (BLOB in DB). Falls back to
     * the file-based path if bytes are null. Falls back to an emoji placeholder
     * if both are missing.
     */
    public static StackPane creerVignetteFromBytes(byte[] bytes, String filenameFallback,
                                                    double width, double height,
                                                    String emoji, String couleurBg) {
        StackPane container = new StackPane();
        container.setPrefSize(width, height);
        container.setMaxSize(width, height);
        container.setMinSize(width, height);

        Image img = null;
        if (bytes != null && bytes.length > 0) {
            try {
                img = new Image(new java.io.ByteArrayInputStream(bytes), width, height, true, true);
            } catch (Exception ignored) {}
        }
        if (img == null || img.isError()) {
            String chemin = getImagePath(filenameFallback);
            if (chemin != null) {
                try {
                    img = new Image(new File(chemin).toURI().toString(), width, height, true, true);
                } catch (Exception ignored) {}
            }
        }
        if (img != null && !img.isError()) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(width);
            iv.setFitHeight(height);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setClip(new Rectangle(width, height));
            container.setStyle("-fx-background-color: #f0f5ef;");
            container.getChildren().add(iv);
            return container;
        }

        container.setStyle(
                "-fx-background-color: " + (couleurBg != null ? couleurBg : "#e8f2e6") + ";");
        Label lbl = new Label(emoji != null ? emoji : "📦");
        lbl.setStyle("-fx-font-size: " + (int)(height * 0.42) + "px;");
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(lbl);
        return container;
    }

    /**
     * Construit une vignette JavaFX à partir du nom de fichier stocké en base.
     * Si le fichier est manquant, retombe sur un placeholder emoji.
     */
    public static StackPane creerVignetteImage(String filename,
                                                double width, double height,
                                                String emoji, String couleurBg) {
        StackPane container = new StackPane();
        container.setPrefSize(width, height);
        container.setMaxSize(width, height);
        container.setMinSize(width, height);

        String chemin = getImagePath(filename);
        if (chemin != null) {
            try {
                Image img = new Image(new File(chemin).toURI().toString(),
                        width, height, true, true);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(width);
                    iv.setFitHeight(height);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    Rectangle clip = new Rectangle(width, height);
                    iv.setClip(clip);
                    container.setStyle("-fx-background-color: #f0f5ef;");
                    container.getChildren().add(iv);
                    return container;
                }
            } catch (Exception ignored) {}
        }

        container.setStyle(
                "-fx-background-color: " + (couleurBg != null ? couleurBg : "#e8f2e6") + ";");
        Label lbl = new Label(emoji != null ? emoji : "📦");
        lbl.setStyle("-fx-font-size: " + (int)(height * 0.42) + "px;");
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(lbl);
        return container;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String slug(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer
                .normalize(input.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String obtenirExtension(String nomFichier) {
        if (nomFichier == null) return ".jpg";
        int idx = nomFichier.lastIndexOf('.');
        return idx >= 0 ? nomFichier.substring(idx).toLowerCase(Locale.ROOT) : ".jpg";
    }
}
