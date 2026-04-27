package otemps.utils;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import javafx.embed.swing.SwingFXUtils;
import otemps.entites.Objet;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

public class PDFGenerator {

    public static void generatePDF(Objet objet, javafx.scene.image.Image fxImage, String filePath) {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Couleurs OTEMPS
            Color otempsGold = new Color(197, 160, 89);

            // 1. Titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, otempsGold);
            Paragraph title = new Paragraph(objet.getNom().toUpperCase(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("\n"));

            // 2. Insertion de l'Image
            if (fxImage != null) {
                // Conversion JavaFX Image vers OpenPDF Image
                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                ImageIO.write(SwingFXUtils.fromFXImage(fxImage, null), "png", byteOutput);
                Image pdfImg = Image.getInstance(byteOutput.toByteArray());

                pdfImg.setAlignment(Element.ALIGN_CENTER);
                pdfImg.scaleToFit(300, 300); // Redimensionnement pour le PDF
                document.add(pdfImg);
            }

            document.add(new Paragraph("\n"));

            // 3. Détails Techniques
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Époque : " + objet.getEpoque(), labelFont));
            document.add(new Paragraph("Origine : " + objet.getOrigine(), labelFont));
            document.add(new Paragraph("Matériaux : " + objet.getMateriaux(), labelFont));

            document.add(new Paragraph("\n"));

            // 4. Description
            document.add(new Paragraph("HISTOIRE & RÉCIT", labelFont));
            document.add(new Paragraph(objet.getDescription()));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}