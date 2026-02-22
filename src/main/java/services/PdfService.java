package services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;

public class PdfService {

    public static void genererRapportTache(String description, String date, String cout, String filename) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            // --- AJOUT DU LOGO ICI ---
            try {
                // Remplace "src/main/resources/images/logo.png" par le vrai chemin de ton image
                Image logo = Image.getInstance("src/main/resources/images/logo.png");
                logo.scaleToFit(80, 80); // Redimensionner l'image (largeur, hauteur)
                logo.setAlignment(Element.ALIGN_CENTER); // Centrer le logo
                document.add(logo);
            } catch (Exception e) {
                System.out.println("Logo non trouvé, on continue sans image.");
            }
            // --------------------------

            // Titre du PDF
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titre = new Paragraph("AGRICORE - RAPPORT DE PLANIFICATION", fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(new Paragraph("\n")); // Espace

            // Contenu
            document.add(new Paragraph("Details de l'intervention :"));

            document.add(new Paragraph("Description technique : " + description));
            document.add(new Paragraph("Date prevue : " + date));
            document.add(new Paragraph("Cout estime : " + cout + " DT"));




            document.close();
            System.out.println("PDF genere avec succes avec logo !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}