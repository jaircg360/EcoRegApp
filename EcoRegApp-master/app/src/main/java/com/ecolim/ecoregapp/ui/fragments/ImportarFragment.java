package com.ecolim.ecoregapp.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.ecolim.ecoregapp.utils.FileManager;
import com.ecolim.ecoregapp.utils.NotificationHelper;
import com.ecolim.ecoregapp.utils.SessionManager;
import com.ecolim.ecoregapp.viewmodel.ResiduoViewModel;
import java.util.List;

public class ImportarFragment extends Fragment {

    private ResiduoViewModel viewModel;
    private SessionManager session;

    private TextView tvArchivoSeleccionado, tvResultado;
    private Button btnSeleccionarCSV, btnSeleccionarPDF, btnImportar;
    private LinearLayout layoutVista;
    private ProgressBar progressImportar;

    private Uri uriSeleccionado;
    private String tipoArchivo;
    private List<Residuo> listaPreview;

    private final ActivityResultLauncher<Intent> csvLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                uriSeleccionado = result.getData().getData();
                tipoArchivo = "csv";
                mostrarArchivoSeleccionado();
            }
        });

    private final ActivityResultLauncher<Intent> pdfLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                uriSeleccionado = result.getData().getData();
                tipoArchivo = "pdf";
                mostrarArchivoSeleccionado();
            }
        });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_importar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session   = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(ResiduoViewModel.class);

        tvArchivoSeleccionado = view.findViewById(R.id.tv_archivo_seleccionado);
        tvResultado           = view.findViewById(R.id.tv_resultado_importacion);
        btnSeleccionarCSV     = view.findViewById(R.id.btn_seleccionar_csv);
        btnSeleccionarPDF     = view.findViewById(R.id.btn_seleccionar_pdf);
        btnImportar           = view.findViewById(R.id.btn_importar);
        progressImportar      = view.findViewById(R.id.progress_importar);
        layoutVista           = view.findViewById(R.id.layout_preview);

        btnSeleccionarCSV.setOnClickListener(v -> abrirSelectorCSV());
        btnSeleccionarPDF.setOnClickListener(v -> abrirSelectorPDF());
        btnImportar.setOnClickListener(v -> ejecutarImportacion());
        observeViewModel();
    }

    private void abrirSelectorCSV() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        csvLauncher.launch(Intent.createChooser(intent, "Seleccionar archivo CSV"));
    }

    private void abrirSelectorPDF() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
            new String[]{"application/pdf", "text/plain"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfLauncher.launch(Intent.createChooser(intent, "Seleccionar archivo PDF o TXT"));
    }

    private void mostrarArchivoSeleccionado() {
        if (uriSeleccionado == null) return;
        String nombre = uriSeleccionado.getLastPathSegment();
        tvArchivoSeleccionado.setText("📄 " + nombre);
        tvArchivoSeleccionado.setVisibility(View.VISIBLE);
        btnImportar.setVisibility(View.VISIBLE);
        tvResultado.setVisibility(View.GONE);
        previewArchivo();
    }

    private void previewArchivo() {
        progressImportar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String opId     = session.getOperarioId();
                String opNombre = session.getOperarioNombre();
                if ("csv".equals(tipoArchivo)) {
                    listaPreview = FileManager.importarCSV(requireContext(),
                        uriSeleccionado, opId, opNombre);
                } else {
                    listaPreview = FileManager.importarDesdeTexto(requireContext(),
                        uriSeleccionado, opId, opNombre);
                }
                requireActivity().runOnUiThread(() -> {
                    progressImportar.setVisibility(View.GONE);
                    int n = listaPreview.size();
                    if (n > 0) {
                        tvResultado.setText("Se encontraron " + n
                            + " registro(s). Toca 'Importar' para guardarlos.");
                        tvResultado.setVisibility(View.VISIBLE);
                        btnImportar.setText("⬇️ Importar " + n + " registros");
                    } else {
                        tvResultado.setText("No se encontraron registros válidos en el archivo.");
                        tvResultado.setVisibility(View.VISIBLE);
                        btnImportar.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressImportar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                        "Error al leer el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void ejecutarImportacion() {
        if (listaPreview == null || listaPreview.isEmpty()) {
            Toast.makeText(requireContext(), "Sin registros para importar", Toast.LENGTH_SHORT).show();
            return;
        }
        progressImportar.setVisibility(View.VISIBLE);
        btnImportar.setEnabled(false);
        viewModel.importarLista(listaPreview);
    }

    private void observeViewModel() {
        viewModel.getImportadosCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                progressImportar.setVisibility(View.GONE);
                btnImportar.setEnabled(true);
                tvResultado.setText("✅ " + count + " registros importados exitosamente.");
                tvResultado.setVisibility(View.VISIBLE);
                btnImportar.setVisibility(View.GONE);

                // ── Notificación real al importar ─────────────────────────
                String tipo = "csv".equals(tipoArchivo) ? "CSV" : "PDF/TXT";
                NotificationHelper.enviar(requireContext(),
                    "📥 Importación completada",
                    count + " registros importados desde archivo " + tipo
                    + " y guardados en la base de datos local.");

                uriSeleccionado = null;
                listaPreview    = null;
            }
        });
    }
}
