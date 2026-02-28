package utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Gestionnaire d'images pour les équipements.
 *
 * Principe : stocke les images dans un dossier local "images_equipements/"
 * et associe le nom de l'équipement à son chemin via un fichier
 * "equipements_images.properties" — SANS modifier la BDD ni les entités.
 *
 * Clé = nom de l'équipement normalisé (minuscule, sans espaces superflus)
 * Valeur = chemin absolu vers le fichier image copié
 */
public class ImageManager {

    // Dossier où les images copiées sont stockées (relatif au répertoire de travail)
    private static final String IMAGES_DIR = "images_equipements";
    // Fichier de correspondance nom → chemin image
    private static final String PROPS_FILE = "equipements_images.properties";

    private static final Properties props = new Properties();

    static {
        chargerProps();
        // Créer le dossier images s'il n'existe pas
        try { Files.createDirectories(Paths.get(IMAGES_DIR)); }
        catch (IOException ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────
    //  LECTURE / ÉCRITURE du fichier properties
    // ─────────────────────────────────────────────────────────────────

    private static void chargerProps() {
        File f = new File(PROPS_FILE);
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
    }

    private static void sauvegarderProps() {
        try (OutputStream out = new FileOutputStream(PROPS_FILE)) {
            props.store(out, "Images equipements AgriCore");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Normalise le nom pour en faire une clé properties valide */
    private static String normaliserCle(String nom) {
        if (nom == null) return "";
        return nom.trim().toLowerCase()
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-z0-9_àâäéèêëîïôùûüç]", "");
    }

    // ─────────────────────────────────────────────────────────────────
    //  API PUBLIQUE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Associe un fichier image à un équipement.
     * Copie le fichier dans le dossier "images_equipements/" et mémorise le lien.
     *
     * @param nomEquipement  nom de l'équipement (tel que saisi dans le formulaire)
     * @param sourceFichier  fichier image choisi par l'utilisateur via FileChooser
     */
    public static void sauvegarderImage(String nomEquipement, File sourceFichier) {
        if (nomEquipement == null || nomEquipement.isBlank() || sourceFichier == null) return;
        try {
            String ext = obtenirExtension(sourceFichier.getName());
            String cle  = normaliserCle(nomEquipement);
            String dest = IMAGES_DIR + File.separator + cle + ext;
            Files.copy(sourceFichier.toPath(), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
            props.setProperty(cle, new File(dest).getAbsolutePath());
            sauvegarderProps();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Supprime l'association image pour un équipement (et le fichier copié).
     */
    public static void supprimerImage(String nomEquipement) {
        String cle = normaliserCle(nomEquipement);
        String chemin = props.getProperty(cle);
        if (chemin != null) {
            try { Files.deleteIfExists(Paths.get(chemin)); } catch (IOException ignored) {}
            props.remove(cle);
            sauvegarderProps();
        }
    }

    /**
     * Retourne le chemin absolu de l'image associée au nom d'équipement,
     * ou null si aucune image n'est enregistrée.
     */
    public static String getImagePath(String nomEquipement) {
        if (nomEquipement == null || nomEquipement.isBlank()) return null;
        String chemin = props.getProperty(normaliserCle(nomEquipement));
        if (chemin == null) return null;
        return new File(chemin).exists() ? chemin : null;
    }

    /**
     * Indique si une image existe pour cet équipement.
     */
    public static boolean aUneImage(String nomEquipement) {
        return getImagePath(nomEquipement) != null;
    }

    // ─────────────────────────────────────────────────────────────────
    //  RENDU JAVAFX
    // ─────────────────────────────────────────────────────────────────

    /**
     * Crée un StackPane contenant soit l'image de l'équipement,
     * soit un placeholder avec l'emoji du type si aucune image n'existe.
     *
     * @param nomEquipement  nom de l'équipement
     * @param width          largeur souhaitée
     * @param height         hauteur souhaitée
     * @param emoji          emoji de secours (ex: "🚜")
     * @param couleurBg      couleur de fond du placeholder (ex: "#e8f2e6")
     */
    public static StackPane creerVignetteImage(String nomEquipement,
                                                double width, double height,
                                                String emoji, String couleurBg) {
        StackPane container = new StackPane();
        container.setPrefSize(width, height);
        container.setMaxSize(width, height);
        container.setMinSize(width, height);

        String chemin = getImagePath(nomEquipement);
        if (chemin != null) {
            try {
                Image img = new Image(
                    new File(chemin).toURI().toString(),
                    width, height, true, true
                );
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(width);
                    iv.setFitHeight(height);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    // Clip arrondi en bas (les coins hauts sont souvent couverts par la bande)
                    Rectangle clip = new Rectangle(width, height);
                    clip.setArcWidth(0);
                    clip.setArcHeight(0);
                    iv.setClip(clip);
                    container.setStyle("-fx-background-color: #f0f5ef;");
                    container.getChildren().add(iv);
                    return container;
                }
            } catch (Exception ignored) {}
        }

        // ── Placeholder emoji ────────────────────────────────────────
        container.setStyle(
            "-fx-background-color: " + (couleurBg != null ? couleurBg : "#e8f2e6") + ";"
        );
        Label lbl = new Label(emoji != null ? emoji : "📦");
        lbl.setStyle("-fx-font-size: " + (int)(height * 0.42) + "px;");
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(lbl);
        return container;
    }

    // ─────────────────────────────────────────────────────────────────
    //  UTILITAIRES INTERNES
    // ─────────────────────────────────────────────────────────────────

    private static String obtenirExtension(String nomFichier) {
        int idx = nomFichier.lastIndexOf('.');
        return idx >= 0 ? nomFichier.substring(idx).toLowerCase() : ".jpg";
    }
}
