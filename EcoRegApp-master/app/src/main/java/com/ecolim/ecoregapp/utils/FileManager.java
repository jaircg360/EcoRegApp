package com.ecolim.ecoregapp.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileManager {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DeviceRgb COLOR_HEADER = new DeviceRgb(15, 74, 48);
    private static final DeviceRgb COLOR_ROW_ALT = new DeviceRgb(234, 242, 238);

    // ═══════════════════════════════════════════════
    //  EXPORTAR CSV
    // ═══════════════════════════════════════════════
    public static File exportarCSV(Context ctx, List<Residuo> lista, String nombreArchivo) throws IOException {
        File dir = getExportDir(ctx);
        File file = new File(dir, nombreArchivo + "_" + timestamp() + ".csv");

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            // Encabezados
            writer.writeNext(new String[]{
                "ID", "Tipo", "Peso (kg)", "Volumen (m³)",
                "Fecha", "Ubicacion", "Zona",
                "Operario ID", "Operario Nombre",
                "Requiere EPP", "EPP Confirmado",
                "Observaciones", "Sincronizado"
            });
            // Filas
            for (Residuo r : lista) {
                writer.writeNext(new String[]{
                    String.valueOf(r.id),
                    r.tipo,
                    String.valueOf(r.pesoKg),
                    String.format("%.4f", r.volumenM3),
                    r.fecha != null ? r.fecha : "",
                    r.ubicacion != null ? r.ubicacion : "",
                    r.zona != null ? r.zona : "",
                    r.operarioId != null ? r.operarioId : "",
                    r.operarioNombre != null ? r.operarioNombre : "",
                    r.requiereEPP ? "Sí" : "No",
                    r.eppConfirmado ? "Sí" : "No",
                    r.observaciones != null ? r.observaciones : "",
                    r.sincronizado ? "Sí" : "No"
                });
            }
        }
        return file;
    }

    // ═══════════════════════════════════════════════
    //  IMPORTAR CSV
    // ═══════════════════════════════════════════════
    public static List<Residuo> importarCSV(Context ctx, Uri uri,
                                             String operarioId, String operarioNombre) throws Exception {
        List<Residuo> lista = new ArrayList<>();
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("No se pudo abrir el archivo");

        try (CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            String[] linea;
            boolean primeraLinea = true;
            String nombreArchivo = uri.getLastPathSegment();

            while ((linea = reader.readNext()) != null) {
                // Saltar encabezados
                if (primeraLinea) { primeraLinea = false; continue; }
                if (linea.length < 2) continue;

                try {
                    Residuo r = new Residuo();
                    r.tipo        = linea.length > 1 ? linea[1].trim() : "plastico";
                    r.pesoKg      = linea.length > 2 ? parseDouble(linea[2]) : 0;
                    r.fecha       = linea.length > 4 ? linea[4].trim() : fechaActual();
                    r.ubicacion   = linea.length > 5 ? linea[5].trim() : "";
                    r.zona        = linea.length > 6 ? linea[6].trim() : "";
                    r.operarioId  = operarioId;
                    r.operarioNombre = operarioNombre;
                    r.requiereEPP = r.tipo.equalsIgnoreCase("peligroso");
                    r.sincronizado = false;
                    r.archivoOrigen = nombreArchivo;
                    r.volumenM3 = r.calcularVolumen();
                    lista.add(r);
                } catch (Exception e) {
                    // Saltar fila con error
                }
            }
        }
        return lista;
    }

    // ═══════════════════════════════════════════════
    //  EXPORTAR PDF
    // ═══════════════════════════════════════════════
    public static File exportarPDF(Context ctx, List<Residuo> lista,
                                    String titulo, String periodo) throws Exception {
        File dir = getExportDir(ctx);
        File file = new File(dir, "reporte_" + timestamp() + ".pdf");

        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        // ─ Título ─
        doc.add(new Paragraph("ECOLIM S.A.C.")
                .setFontColor(COLOR_HEADER)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph(titulo)
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("Período: " + periodo + " | Generado: " + fechaActual())
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(12));

        // ─ Resumen ─
        double totalKg = 0;
        Map<String, Double> porTipo = new LinkedHashMap<>();
        for (Residuo r : lista) {
            totalKg += r.pesoKg;
            porTipo.merge(r.tipo, r.pesoKg, Double::sum);
        }

        doc.add(new Paragraph("Resumen")
                .setFontSize(12).setBold()
                .setFontColor(COLOR_HEADER)
                .setMarginBottom(6));

        Table resumen = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1})).useAllAvailableWidth();
        addHeaderCell(resumen, "Tipo");
        addHeaderCell(resumen, "Total (kg)");
        addHeaderCell(resumen, "% del total");
        for (Map.Entry<String, Double> e : porTipo.entrySet()) {
            resumen.addCell(new Cell().add(new Paragraph(capitalizar(e.getKey())).setFontSize(9)));
            resumen.addCell(new Cell().add(new Paragraph(String.format("%.2f", e.getValue())).setFontSize(9)));
            resumen.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", totalKg > 0 ? e.getValue() / totalKg * 100 : 0)).setFontSize(9)));
        }
        // Total
        addHeaderCell(resumen, "TOTAL");
        Cell totalCell = new Cell().add(new Paragraph(String.format("%.2f kg", totalKg)).setFontSize(9).setBold());
        totalCell.setBackgroundColor(COLOR_HEADER).setFontColor(ColorConstants.WHITE);
        resumen.addCell(totalCell);
        resumen.addCell(new Cell().add(new Paragraph("100%").setFontSize(9)));
        doc.add(resumen);

        doc.add(new Paragraph("Detalle de registros (" + lista.size() + " entradas)")
                .setFontSize(12).setBold()
                .setFontColor(COLOR_HEADER)
                .setMarginTop(14).setMarginBottom(6));

        // ─ Tabla detalle ─
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{0.5f, 1.2f, 0.8f, 0.8f, 1.5f, 1.2f, 1.5f})).useAllAvailableWidth();
        addHeaderCell(tabla, "#");
        addHeaderCell(tabla, "Tipo");
        addHeaderCell(tabla, "Peso kg");
        addHeaderCell(tabla, "Vol. m³");
        addHeaderCell(tabla, "Fecha");
        addHeaderCell(tabla, "Zona");
        addHeaderCell(tabla, "Operario");

        boolean alt = false;
        for (Residuo r : lista) {
            DeviceRgb bg = alt ? COLOR_ROW_ALT : new DeviceRgb(255, 255, 255);
            addDataCell(tabla, String.valueOf(r.id), bg);
            addDataCell(tabla, capitalizar(r.tipo), bg);
            addDataCell(tabla, String.format("%.2f", r.pesoKg), bg);
            addDataCell(tabla, String.format("%.4f", r.volumenM3), bg);
            addDataCell(tabla, r.fecha != null ? r.fecha : "-", bg);
            addDataCell(tabla, r.zona != null ? r.zona : "-", bg);
            addDataCell(tabla, r.operarioNombre != null ? r.operarioNombre : "-", bg);
            alt = !alt;
        }
        doc.add(tabla);

        doc.add(new Paragraph("\nEcoRegApp · ECOLIM S.A.C. · NTP 900.058")
                .setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(16));

        doc.close();
        return file;
    }

    // ═══════════════════════════════════════════════
    //  IMPORTAR PDF (leer texto plano)
    //  Nota: importar PDF real requiere parseo complejo.
    //  Esta versión importa desde PDF con texto estructurado.
    // ═══════════════════════════════════════════════
    public static List<Residuo> importarDesdeTexto(Context ctx, Uri uri,
                                                     String operarioId, String operarioNombre) throws Exception {
        // Para PDFs que no sean nuestro propio formato, leemos como texto
        // y parseamos líneas con formato: tipo,peso,fecha,ubicacion,zona
        List<Residuo> lista = new ArrayList<>();
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("No se pudo abrir el archivo");

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String linea;
        String nombreArchivo = uri.getLastPathSegment();
        while ((linea = br.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) continue;
            String[] partes = linea.split(",");
            if (partes.length < 2) continue;
            try {
                Residuo r = new Residuo();
                r.tipo = partes[0].trim();
                r.pesoKg = parseDouble(partes[1]);
                r.fecha = partes.length > 2 ? partes[2].trim() : fechaActual();
                r.ubicacion = partes.length > 3 ? partes[3].trim() : "";
                r.zona = partes.length > 4 ? partes[4].trim() : "";
                r.operarioId = operarioId;
                r.operarioNombre = operarioNombre;
                r.requiereEPP = r.tipo.equalsIgnoreCase("peligroso");
                r.archivoOrigen = nombreArchivo;
                r.volumenM3 = r.calcularVolumen();
                lista.add(r);
            } catch (Exception ignored) {}
        }
        br.close();
        return lista;
    }

    // ─ Helpers ─
    private static File getExportDir(Context ctx) {
        File dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "EcoRegApp");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    public static String fechaActual() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim().replace(",", ".")); }
        catch (Exception e) { return 0; }
    }

    private static String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private static void addHeaderCell(Table t, String text) {
        t.addHeaderCell(new Cell()
                .add(new Paragraph(text).setFontSize(9).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(COLOR_HEADER)
                .setPadding(5));
    }

    private static void addDataCell(Table t, String text, DeviceRgb bg) {
        t.addCell(new Cell()
                .add(new Paragraph(text).setFontSize(8))
                .setBackgroundColor(bg)
                .setPadding(4));
    }
}
