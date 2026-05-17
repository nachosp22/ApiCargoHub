package com.cargohub.backend.service;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class AlbaranEntregaPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);

    public byte[] generatePdf(Porte porte) {
        validateSignedPorte(porte);

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Paragraph title = new Paragraph("ALBARÁN DE ENTREGA", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16f);
            document.add(title);

            addSection(document, "Datos del porte", new String[][]{
                    {"Porte", "#" + porte.getId()},
                    {"Estado", safe(porte.getEstado() != null ? porte.getEstado().name() : null)},
                    {"Origen", safe(porte.getOrigen())},
                    {"Destino", safe(porte.getDestino())},
                    {"Fecha recogida", fmt(porte.getFechaRecogida())},
                    {"Fecha entrega", fmt(porte.getFechaEntrega())}
            });

            Cliente cliente = porte.getCliente();
            addSection(document, "Cliente", new String[][]{
                    {"Empresa", safe(cliente != null ? cliente.getNombreEmpresa() : null)},
                    {"CIF", safe(cliente != null ? cliente.getCif() : null)}
            });

            Conductor conductor = porte.getConductor();
            addSection(document, "Transporte", new String[][]{
                    {"Conductor", safe(nombreConductor(conductor))},
                    {"Tipo de vehículo", safe(porte.getTipoVehiculoRequerido() != null ? porte.getTipoVehiculoRequerido().name() : null)}
            });

            addSection(document, "Mercancía", new String[][]{
                    {"Descripción", safe(porte.getDescripcionCliente())},
                    {"Peso total", porte.getPesoTotalKg() != null ? String.format("%.2f kg", porte.getPesoTotalKg()) : "—"},
                    {"Volumen total", porte.getVolumenTotalM3() != null ? String.format("%.2f m³", porte.getVolumenTotalM3()) : "—"}
            });

            addSection(document, "Recepción", new String[][]{
                    {"Firmado por", safe(porte.getFirmaEntregaFirmadoPor())},
                    {"Fecha de firma", fmt(porte.getFirmaEntregaFecha())}
            });

            addSignature(document, porte.getFirmaEntregaBase64());

        } catch (DocumentException e) {
            throw new RuntimeException("No se pudo generar el PDF del albarán.", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void validateSignedPorte(Porte porte) {
        if (porte == null || porte.getId() == null) {
            throw new RuntimeException("No se encontró el porte solicitado.");
        }
        EstadoPorte estado = porte.getEstado();
        if (estado != EstadoPorte.ENTREGADO && estado != EstadoPorte.FACTURADO) {
            throw new RuntimeException("El albarán solo está disponible para portes entregados o facturados.");
        }
        if (porte.getFirmaEntregaBase64() == null || porte.getFirmaEntregaBase64().isBlank()) {
            throw new RuntimeException("El porte no tiene firma de entrega registrada. No se puede generar el albarán.");
        }
        if (porte.getFirmaEntregaFirmadoPor() == null || porte.getFirmaEntregaFirmadoPor().isBlank()) {
            throw new RuntimeException("El porte no tiene nombre de firmante. No se puede generar el albarán.");
        }
    }

    private void addSection(Document document, String title, String[][] rows) throws DocumentException {
        Paragraph sectionTitle = new Paragraph(title, LABEL_FONT);
        sectionTitle.setSpacingBefore(8f);
        sectionTitle.setSpacingAfter(4f);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(new float[]{28, 72});
        table.setWidthPercentage(100);
        for (String[] row : rows) {
            PdfPCell labelCell = new PdfPCell(new Phrase(row[0], LABEL_FONT));
            labelCell.setBorder(Rectangle.BOX);
            labelCell.setPadding(5f);
            table.addCell(labelCell);

            PdfPCell valueCell = new PdfPCell(new Phrase(row[1], VALUE_FONT));
            valueCell.setBorder(Rectangle.BOX);
            valueCell.setPadding(5f);
            table.addCell(valueCell);
        }
        document.add(table);
    }

    private void addSignature(Document document, String signatureBase64) throws DocumentException {
        Paragraph signatureTitle = new Paragraph("Firma", LABEL_FONT);
        signatureTitle.setSpacingBefore(12f);
        signatureTitle.setSpacingAfter(6f);
        document.add(signatureTitle);

        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            Image signatureImage = Image.getInstance(signatureBytes);
            signatureImage.scaleToFit(260, 100);
            signatureImage.setAlignment(Element.ALIGN_LEFT);
            document.add(signatureImage);
        } catch (Exception e) {
            Paragraph fallback = new Paragraph("(No se pudo renderizar la firma)", VALUE_FONT);
            document.add(fallback);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String fmt(LocalDateTime value) {
        return value == null ? "—" : value.format(DATE_FMT);
    }

    private String nombreConductor(Conductor conductor) {
        if (conductor == null) return null;
        String nombre = conductor.getNombre() != null ? conductor.getNombre().trim() : "";
        String apellidos = conductor.getApellidos() != null ? conductor.getApellidos().trim() : "";
        String full = (nombre + " " + apellidos).trim();
        return full.isEmpty() ? null : full;
    }
}
