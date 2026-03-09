package com.ecolim.ecoregapp.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.ecolim.ecoregapp.ui.views.BarChartView;
import com.ecolim.ecoregapp.ui.views.DonutChartView;
import com.ecolim.ecoregapp.utils.FileManager;
import com.ecolim.ecoregapp.utils.NotificationHelper;
import com.ecolim.ecoregapp.viewmodel.ResiduoViewModel;
import com.google.android.material.chip.ChipGroup;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportesFragment extends Fragment {

    private ResiduoViewModel viewModel;

    // Tabs de período (TextView visibles)
    private TextView tabHoy, tabSemana, tabMes, tabTodos;
    private String periodoActual = "hoy"; // hoy | semana | mes | todos

    // Chips de tipo (ChipGroup)
    private ChipGroup chipTipo;

    // Stats
    private TextView tvTotalKg, tvTotalReg, tvTotalTipos;

    // Exportar
    private View btnExportarPDF, btnExportarCSV;
    private ProgressBar progressExport;

    // Gráficos
    private DonutChartView chartDona;
    private BarChartView chartBarras;
    private LinearLayout layoutLeyenda;

    private List<Residuo> listaActual = new ArrayList<>();

    private static final Map<String, Integer> COLORES = new LinkedHashMap<>();
    static {
        COLORES.put("plastico",  Color.parseColor("#2E9E6B"));
        COLORES.put("organico",  Color.parseColor("#FFB547"));
        COLORES.put("papel",     Color.parseColor("#4A9EFF"));
        COLORES.put("peligroso", Color.parseColor("#FF5A5A"));
        COLORES.put("metal",     Color.parseColor("#8A8A8A"));
        COLORES.put("vidrio",    Color.parseColor("#64CFFF"));
        COLORES.put("otros",     Color.parseColor("#B0B0B0"));
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inf.inflate(R.layout.fragment_reportes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ResiduoViewModel.class);

        // Tabs de período
        tabHoy    = view.findViewById(R.id.chip_hoy);
        tabSemana = view.findViewById(R.id.chip_semana);
        tabMes    = view.findViewById(R.id.chip_mes);
        tabTodos  = view.findViewById(R.id.chip_todos);

        // Chips tipo
        chipTipo       = view.findViewById(R.id.chip_group_tipo_reporte);

        // Stats
        tvTotalKg      = view.findViewById(R.id.tv_total_kg);
        tvTotalReg     = view.findViewById(R.id.tv_total_reg);
        tvTotalTipos   = view.findViewById(R.id.tv_total_tipos);

        // Exportar
        btnExportarPDF = view.findViewById(R.id.btn_exportar_pdf);
        btnExportarCSV = view.findViewById(R.id.btn_exportar_csv);
        progressExport = view.findViewById(R.id.progress_export);

        // Gráficos
        chartDona     = view.findViewById(R.id.chart_dona);
        chartBarras   = view.findViewById(R.id.chart_barras);
        layoutLeyenda = view.findViewById(R.id.layout_leyenda);

        // Clicks tabs
        tabHoy.setOnClickListener(v    -> seleccionarPeriodo("hoy"));
        tabSemana.setOnClickListener(v -> seleccionarPeriodo("semana"));
        tabMes.setOnClickListener(v    -> seleccionarPeriodo("mes"));
        tabTodos.setOnClickListener(v  -> seleccionarPeriodo("todos"));

        chipTipo.setOnCheckedStateChangeListener((g, ids) -> aplicarFiltros());
        btnExportarPDF.setOnClickListener(v -> exportarPDF());
        btnExportarCSV.setOnClickListener(v -> exportarCSV());

        // Activar tab inicial
        actualizarEstiloTabs();
        observeData();
    }

    // ── Selección de período ────────────────────────────────────────────
    private void seleccionarPeriodo(String periodo) {
        periodoActual = periodo;
        actualizarEstiloTabs();
        aplicarFiltros();
    }

    private void actualizarEstiloTabs() {
        resetTab(tabHoy);
        resetTab(tabSemana);
        resetTab(tabMes);
        resetTab(tabTodos);

        TextView activo = periodoActual.equals("hoy")    ? tabHoy
                        : periodoActual.equals("semana") ? tabSemana
                        : periodoActual.equals("mes")    ? tabMes
                        : tabTodos;
        activarTab(activo);
    }

    private void resetTab(TextView tv) {
        tv.setBackgroundResource(android.R.color.transparent);
        tv.setTextColor(Color.parseColor("#CCFFFFFF"));
        tv.setTypeface(null, Typeface.NORMAL);
    }

    private void activarTab(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_tab_active);
        tv.setTextColor(Color.parseColor("#1B5E20"));
        tv.setTypeface(null, Typeface.BOLD);
    }

    // ── Datos ────────────────────────────────────────────────────────────
    private void observeData() {
        viewModel.todosLosResiduos.observe(getViewLifecycleOwner(), lista -> {
            listaActual = lista != null ? lista : new ArrayList<>();
            aplicarFiltros();
        });
    }

    private void aplicarFiltros() {
        List<Residuo> filtrada = new ArrayList<>(listaActual);
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Calendar cal = Calendar.getInstance();

        switch (periodoActual) {
            case "hoy":
                filtrada.removeIf(r -> r.fecha == null || !r.fecha.startsWith(hoy));
                break;
            case "semana":
                cal.add(Calendar.DAY_OF_YEAR, -7);
                String hace7 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                filtrada.removeIf(r -> r.fecha == null || r.fecha.compareTo(hace7) < 0);
                break;
            case "mes":
                filtrada.removeIf(r -> r.fecha == null || !r.fecha.startsWith(hoy.substring(0, 7)));
                break;
        }

        int tId = chipTipo.getCheckedChipId();
        if (tId != R.id.chip_tipo_todos && tId != View.NO_ID) {
            String t = getTipoDeChip(tId);
            if (t != null) filtrada.removeIf(r -> !t.equalsIgnoreCase(r.tipo));
        }

        actualizarResumen(filtrada);
        actualizarGraficos(filtrada);
    }

    private String getTipoDeChip(int id) {
        if (id == R.id.chip_tipo_plastico)  return "plastico";
        if (id == R.id.chip_tipo_organico)  return "organico";
        if (id == R.id.chip_tipo_papel)     return "papel";
        if (id == R.id.chip_tipo_peligroso) return "peligroso";
        return null;
    }

    private void actualizarResumen(List<Residuo> lista) {
        double totalKg = 0;
        for (Residuo r : lista) totalKg += r.pesoKg;
        Set<String> tipos = new HashSet<>();
        for (Residuo r : lista) if (r.tipo != null) tipos.add(r.tipo);
        tvTotalKg.setText(String.format("%.1f", totalKg));
        tvTotalReg.setText(String.valueOf(lista.size()));
        tvTotalTipos.setText(String.valueOf(tipos.size()));
    }

    private void actualizarGraficos(List<Residuo> lista) {
        Map<String, Float> kgPorTipo = new LinkedHashMap<>();
        for (String tipo : COLORES.keySet()) kgPorTipo.put(tipo, 0f);
        for (Residuo r : lista) {
            if (r.tipo == null) continue;
            String t = r.tipo.toLowerCase();
            kgPorTipo.put(t, kgPorTipo.getOrDefault(t, 0f) + (float) r.pesoKg);
        }

        List<DonutChartView.Segmento> segmentos = new ArrayList<>();
        List<BarChartView.Barra> barras = new ArrayList<>();
        double total = 0;
        for (Map.Entry<String, Float> e : kgPorTipo.entrySet()) total += e.getValue();

        for (Map.Entry<String, Float> e : kgPorTipo.entrySet()) {
            if (e.getValue() <= 0) continue;
            int color = COLORES.getOrDefault(e.getKey(), Color.GRAY);
            String label = capitalize(e.getKey());
            segmentos.add(new DonutChartView.Segmento(label, e.getValue(), color));
            barras.add(new BarChartView.Barra(
                label.substring(0, Math.min(4, label.length())), e.getValue(), color));
        }

        chartDona.setDatos(segmentos,
            String.format("%.0f kg", total), lista.size() + " reg.");
        chartBarras.setDatos(barras);

        // Leyenda al costado
        layoutLeyenda.removeAllViews();
        if (segmentos.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("Sin datos\npara este\nperíodo");
            tv.setTextColor(Color.parseColor("#AAAAAA"));
            tv.setTextSize(11f);
            tv.setGravity(android.view.Gravity.CENTER);
            layoutLeyenda.addView(tv);
            return;
        }
        for (DonutChartView.Segmento s : segmentos) {
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 10);
            fila.setLayoutParams(lp);

            View dot = new View(requireContext());
            LinearLayout.LayoutParams lpDot = new LinearLayout.LayoutParams(10, 10);
            lpDot.setMargins(0, 0, 8, 0);
            dot.setLayoutParams(lpDot);
            dot.setBackgroundColor(s.color);

            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvN = new TextView(requireContext());
            tvN.setText(capitalize(s.label));
            tvN.setTextSize(11f);
            tvN.setTextColor(Color.parseColor("#333333"));
            tvN.setTypeface(null, Typeface.BOLD);

            float pct = total > 0 ? (s.valor / (float) total * 100f) : 0f;
            TextView tvP = new TextView(requireContext());
            tvP.setText(String.format("%.0f%%  ·  %.1f kg", pct, s.valor));
            tvP.setTextSize(10f);
            tvP.setTextColor(Color.parseColor("#888888"));

            col.addView(tvN);
            col.addView(tvP);
            fila.addView(dot);
            fila.addView(col);
            layoutLeyenda.addView(fila);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // ── Exportar ─────────────────────────────────────────────────────────
    private void exportarPDF() {
        if (listaActual.isEmpty()) {
            Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        progressExport.setVisibility(View.VISIBLE);
        btnExportarPDF.setEnabled(false);
        List<Residuo> copia = new ArrayList<>(listaActual);
        final int total = copia.size();
        new Thread(() -> {
            try {
                String periodo = obtenerPeriodoTexto();
                File pdf = FileManager.exportarPDF(requireContext(), copia, "Reporte de Residuos", periodo);
                requireActivity().runOnUiThread(() -> {
                    progressExport.setVisibility(View.GONE);
                    btnExportarPDF.setEnabled(true);
                    NotificationHelper.enviar(requireContext(), "📄 PDF exportado",
                        "Reporte con " + total + " registros.");
                    compartirArchivo(pdf, "application/pdf");
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressExport.setVisibility(View.GONE);
                    btnExportarPDF.setEnabled(true);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void exportarCSV() {
        if (listaActual.isEmpty()) {
            Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        progressExport.setVisibility(View.VISIBLE);
        btnExportarCSV.setEnabled(false);
        List<Residuo> copia = new ArrayList<>(listaActual);
        final int total = copia.size();
        new Thread(() -> {
            try {
                File csv = FileManager.exportarCSV(requireContext(), copia, "residuos_ecolim");
                requireActivity().runOnUiThread(() -> {
                    progressExport.setVisibility(View.GONE);
                    btnExportarCSV.setEnabled(true);
                    NotificationHelper.enviar(requireContext(), "📊 CSV exportado",
                        "Archivo con " + total + " registros listo.");
                    compartirArchivo(csv, "text/csv");
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressExport.setVisibility(View.GONE);
                    btnExportarCSV.setEnabled(true);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void compartirArchivo(File file, String mimeType) {
        android.net.Uri uri = FileProvider.getUriForFile(requireContext(),
            requireContext().getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Reporte EcoRegApp - ECOLIM S.A.C.");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Compartir reporte"));
    }

    private String obtenerPeriodoTexto() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        switch (periodoActual) {
            case "hoy":    return "Hoy " + sdf.format(new Date());
            case "semana":
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -7);
                return sdf.format(cal.getTime()) + " - " + sdf.format(new Date());
            case "mes":
                Calendar c2 = Calendar.getInstance();
                c2.set(Calendar.DAY_OF_MONTH, 1);
                return sdf.format(c2.getTime()) + " - " + sdf.format(new Date());
            default: return "Todo el historial";
        }
    }
}
