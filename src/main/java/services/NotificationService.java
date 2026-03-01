package services;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class NotificationService {

    private static NotificationService instance;
    private TrayIcon trayIcon;
    private boolean supported = false;

    // ════════════════════════════════════════
    //  SINGLETON
    // ════════════════════════════════════════
    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    private NotificationService() {
        initialiser();
    }

    // ════════════════════════════════════════
    //  INITIALISATION SYSTEM TRAY
    // ════════════════════════════════════════
    private void initialiser() {
        if (!SystemTray.isSupported()) {
            System.out.println("⚠️ SystemTray non supporté sur cet OS");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Créer une icône simple (carré vert)
            int w = tray.getTrayIconSize().width;
            int h = tray.getTrayIconSize().height;
            Image image = creerIcone(w, h);

            trayIcon = new TrayIcon(image, "🐾 Gestion Animaux");
            trayIcon.setImageAutoSize(true);

            tray.add(trayIcon);
            supported = true;

            System.out.println("✅ Notifications Desktop activées");

        } catch (AWTException e) {
            System.out.println("❌ Erreur initialisation SystemTray : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════
    //  CRÉER ICÔNE SIMPLE
    // ════════════════════════════════════════
    private Image creerIcone(int w, int h) {
        // Crée une icône verte simple
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(w, h,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(46, 125, 50));  // vert foncé
        g2.fillOval(0, 0, w, h);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, w / 2));
        g2.drawString("A", w / 4, h * 3 / 4);
        g2.dispose();
        return img;
    }

    // ════════════════════════════════════════
    //  MÉTHODES DE NOTIFICATION
    // ════════════════════════════════════════

    /** 🔴 Notification CRITIQUE — urgence vétérinaire */
    public void notifierCritique(String codeAnimal, String message) {
        envoyer(
            "🚨 URGENCE — " + codeAnimal,
            message,
            MessageType.ERROR
        );
    }

    /** 🟠 Notification AVERTISSEMENT */
    public void notifierAvertissement(String codeAnimal, String message) {
        envoyer(
            "⚠️ Alerte — " + codeAnimal,
            message,
            MessageType.WARNING
        );
    }

    /** ✅ Notification INFO */
    public void notifierInfo(String titre, String message) {
        envoyer(titre, message, MessageType.INFO);
    }

    /** Notification générique */
    public void envoyer(String titre, String message, MessageType type) {
        if (!supported || trayIcon == null) {
            // Fallback console si SystemTray non disponible
            System.out.println("[NOTIFICATION] " + titre + " : " + message);
            return;
        }
        trayIcon.displayMessage(titre, message, type);
    }

    // ════════════════════════════════════════
    //  NOTIFICATIONS MÉTIER SPÉCIFIQUES
    // ════════════════════════════════════════

    public void notifierTemperatureCritique(String code, double temp) {
        notifierCritique(code,
            "Température critique : " + temp + "°C\nIntervention vétérinaire urgente !");
    }

    public void notifierEtatCritique(String code, String espece) {
        notifierCritique(code,
            espece + " en état CRITIQUE\nConsultez un vétérinaire immédiatement !");
    }

    public void notifierPoidsAnormal(String code, double poids) {
        notifierAvertissement(code,
            "Poids anormal détecté : " + poids + " kg\nVérifier l'alimentation");
    }

    public void notifierRythmeAnormal(String code, int rythme) {
        notifierAvertissement(code,
            "Rythme cardiaque anormal : " + rythme + " bpm\nSurveillance renforcée");
    }

    public void notifierSuiviAjoute(String code, String etat) {
        notifierInfo("✅ Suivi enregistré — " + code,
            "État : " + etat + "\nSuivi médical mis à jour avec succès");
    }

    public void notifierAnimalAjoute(String code, String espece) {
        notifierInfo("🐾 Nouvel animal — " + code,
            espece + " ajouté(e) au système avec succès");
    }

    public boolean isSupported() {
        return supported;
    }
}
