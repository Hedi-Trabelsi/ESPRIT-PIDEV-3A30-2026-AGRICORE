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

            try {
                Image logo = Image.getInstance("src/main/resources/images/logo.png");
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            } catch (Exception e) {
                System.out.println("Logo non trouvé, on continue sans image.");
            }

            // Polices
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font fontGras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // Titre principal
            Paragraph titre = new Paragraph("AGRICORE - RAPPORT DE PLANIFICATION", fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(new Paragraph("\n"));

            // Section Détails
            Paragraph sectionTitre = new Paragraph("Details de l'intervention :", fontGras);
            document.add(sectionTitre);
            document.add(new Paragraph("\n"));

            // Ligne Description
            Paragraph p1 = new Paragraph();
            p1.add(new Chunk("Description technique : ", fontGras));
            p1.add(new Chunk(description, fontNormal));
            document.add(p1);

            // Ligne Date
            Paragraph p2 = new Paragraph();
            p2.add(new Chunk("Date prevue : ", fontGras));
            p2.add(new Chunk(date, fontNormal));
            document.add(p2);

            // Ligne Coût
            Paragraph p3 = new Paragraph();
            p3.add(new Chunk("Cout estime : ", fontGras));
            p3.add(new Chunk(cout + " DT", fontNormal));
            document.add(p3);

            document.close();
            System.out.println("PDF genere avec succes avec logo !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}