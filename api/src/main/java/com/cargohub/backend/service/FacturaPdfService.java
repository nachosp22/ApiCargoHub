package com.cargohub.backend.service;

import com.cargohub.backend.entity.Cliente;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class FacturaPdfService {

    // Issuer constants
    private static final String ISSUER_NAME = "CargoHub S.L.";
    private static final String ISSUER_CIF = "B12345678";
    private static final String ISSUER_ADDRESS = "Calle Logística 1, 28001 Madrid";
    private static final String ISSUER_EMAIL = "facturacion@cargohub.es";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(33, 37, 41));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(33, 37, 41));
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(55, 65, 81));
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(107, 114, 128));
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(33, 37, 41));

    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);
    private static final Color LIGHT_BG = new Color(249, 250, 251);

    public byte[] generatePdf(Factura factura) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // === HEADER ===
            addHeader(document, factura);

            document.add(new Paragraph(" "));

            // === CLIENT DATA ===
            addClientSection(document, factura);

            document.add(new Paragraph(" "));

            // === INVOICE META ===
            addInvoiceMeta(document, factura);

            document.add(new Paragraph(" "));

            // === PORTE DETAILS TABLE ===
            addPorteDetails(document, factura);

            document.add(new Paragraph(" "));

            // === AMOUNTS TABLE ===
            addAmounts(document, factura);

            document.add(new Paragraph(" "));

            // === FOOTER ===
            addFooter(document, factura);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating invoice PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addHeader(Document document, Factura factura) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        // Left: issuer info
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph(ISSUER_NAME, TITLE_FONT));
        left.addElement(new Paragraph("CIF: " + ISSUER_CIF, NORMAL_FONT));
        left.addElement(new Paragraph(ISSUER_ADDRESS, NORMAL_FONT));
        left.addElement(new Paragraph(ISSUER_EMAIL, NORMAL_FONT));

        // Right: invoice number
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph facturaLabel = new Paragraph("FACTURA", new Font(Font.HELVETICA, 28, Font.BOLD, PRIMARY_COLOR));
        facturaLabel.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(facturaLabel);

        Paragraph numSerie = new Paragraph(factura.getNumeroSerie(), HEADER_FONT);
        numSerie.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(numSerie);

        headerTable.addCell(left);
        headerTable.addCell(right);
        document.add(headerTable);

        // Divider line
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingBefore(10);
        PdfPCell divCell = new PdfPCell();
        divCell.setBorder(Rectangle.BOTTOM);
        divCell.setBorderColor(PRIMARY_COLOR);
        divCell.setBorderWidth(2);
        divCell.setFixedHeight(1);
        divider.addCell(divCell);
        document.add(divider);
    }

    private void addClientSection(Document document, Factura factura) throws DocumentException {
        Porte porte = factura.getPorte();
        Cliente cliente = porte != null ? porte.getCliente() : null;

        Paragraph title = new Paragraph("DATOS DEL CLIENTE", new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setWidths(new float[]{50, 50});

        addInfoCell(table, "Empresa", cliente != null ? cliente.getNombreEmpresa() : "—");
        addInfoCell(table, "CIF", cliente != null ? cliente.getCif() : "—");
        addInfoCell(table, "Dirección Fiscal", cliente != null && cliente.getDireccionFiscal() != null ? cliente.getDireccionFiscal() : "—");
        addInfoCell(table, "Email", cliente != null ? cliente.getEmailContacto() : "—");

        document.add(table);
    }

    private void addInvoiceMeta(Document document, Factura factura) throws DocumentException {
        Paragraph title = new Paragraph("DATOS DE LA FACTURA", new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR));
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setWidths(new float[]{50, 50});

        addInfoCell(table, "Nº Serie", factura.getNumeroSerie());
        addInfoCell(table, "Fecha Emisión", factura.getFechaEmision() != null ? factura.getFechaEmision().format(DATE_FMT) : "—");
        addInfoCell(table, "Fecha Pago", factura.getFechaPago() != null ? factura.getFechaPago().format(DATE_FMT) : "Pendiente");
        addInfoCell(table, "Forma de Pago", factura.getFormaPago() != null ? factura.getFormaPago() : "—");
        addInfoCell(table, "Estado", factura.isPagada() ? "PAGADA" : "PENDIENTE");
        addInfoCell(table, "Condiciones", factura.getCondicionesPago() != null ? factura.getCondicionesPago() : "—");

        document.add(table);
    }

    private void addPorteDetails(Document document, Factura factura) throws DocumentException {
        Porte porte = factura.getPorte();
        if (porte == null) return;

        Paragraph title = new Paragraph("DETALLE DEL PORTE", new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR));
        document.add(title);

        PdfPTable table = new PdfPTable(new float[]{20, 20, 15, 15, 15, 15});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);

        // Headers
        String[] headers = {"Origen", "Destino", "F. Recogida", "F. Entrega", "Peso (kg)", "Volumen (m³)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Data row
        addTableCell(table, porte.getOrigen() != null ? porte.getOrigen() : "—");
        addTableCell(table, porte.getDestino() != null ? porte.getDestino() : "—");
        addTableCell(table, porte.getFechaRecogida() != null ? porte.getFechaRecogida().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
        addTableCell(table, porte.getFechaEntrega() != null ? porte.getFechaEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
        addTableCell(table, porte.getPesoTotalKg() != null ? String.format("%.2f", porte.getPesoTotalKg()) : "—");
        addTableCell(table, porte.getVolumenTotalM3() != null ? String.format("%.2f", porte.getVolumenTotalM3()) : "—");

        document.add(table);

        // Description
        if (porte.getDescripcionCliente() != null && !porte.getDescripcionCliente().isBlank()) {
            Paragraph desc = new Paragraph("Descripción: " + porte.getDescripcionCliente(), SMALL_FONT);
            desc.setSpacingBefore(5);
            document.add(desc);
        }
    }

    private void addAmounts(Document document, Factura factura) throws DocumentException {
        Paragraph title = new Paragraph("IMPORTES", new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR));
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(5);
        table.setWidths(new float[]{60, 40});

        addAmountRow(table, "Base Imponible", factura.getBaseImponible(), false);
        addAmountRow(table, "IVA (21%)", factura.getIva(), false);

        // Total row with highlight
        PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL", TOTAL_FONT));
        labelCell.setBackgroundColor(LIGHT_BG);
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setBorderColor(PRIMARY_COLOR);
        labelCell.setBorderWidth(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(
                factura.getImporteTotal() != null ? String.format("%.2f €", factura.getImporteTotal()) : "0.00 €",
                TOTAL_FONT));
        valueCell.setBackgroundColor(LIGHT_BG);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setBorderColor(PRIMARY_COLOR);
        valueCell.setBorderWidth(2);
        table.addCell(valueCell);

        document.add(table);
    }

    private void addFooter(Document document, Factura factura) throws DocumentException {
        // Divider
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell divCell = new PdfPCell();
        divCell.setBorder(Rectangle.TOP);
        divCell.setBorderColor(new Color(229, 231, 235));
        divCell.setBorderWidth(1);
        divCell.setFixedHeight(1);
        divider.addCell(divCell);
        document.add(divider);

        if (factura.getCondicionesPago() != null) {
            Paragraph cond = new Paragraph("Condiciones de pago: " + factura.getCondicionesPago(), SMALL_FONT);
            cond.setSpacingBefore(10);
            document.add(cond);
        }

        if (factura.getObservaciones() != null && !factura.getObservaciones().isBlank()) {
            Paragraph obs = new Paragraph("Observaciones: " + factura.getObservaciones(), SMALL_FONT);
            obs.setSpacingBefore(5);
            document.add(obs);
        }

        Paragraph legal = new Paragraph(
                "Este documento sirve como justificante de facturación. " + ISSUER_NAME + " — " + ISSUER_CIF,
                new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(156, 163, 175)));
        legal.setSpacingBefore(20);
        legal.setAlignment(Element.ALIGN_CENTER);
        document.add(legal);
    }

    // --- Helper methods ---

    private void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.addElement(new Paragraph(label, SMALL_FONT));
        cell.addElement(new Paragraph(value != null ? value : "—", HEADER_FONT));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(LIGHT_BG);
        table.addCell(cell);
    }

    private void addAmountRow(PdfPTable table, String label, Double value, boolean bold) {
        Font font = bold ? HEADER_FONT : NORMAL_FONT;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(
                value != null ? String.format("%.2f €", value) : "0.00 €", font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
}
