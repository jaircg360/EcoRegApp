package com.ecolim.ecoregapp.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.ecolim.ecoregapp.ui.adapters.ResiduoAdapter;
import com.ecolim.ecoregapp.utils.SafeNav;
import com.ecolim.ecoregapp.utils.SessionManager;
import com.ecolim.ecoregapp.viewmodel.ResiduoViewModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class HistorialFragment extends Fragment {

    private ResiduoViewModel viewModel;
    private SessionManager session;
    private ResiduoAdapter adapter;
    private RecyclerView recyclerView;
    private ChipGroup chipGroupFiltro;
    private EditText etBuscar;
    private TextView tvVacio;
    private List<Residuo> listaCompleta = new ArrayList<>();

    private boolean esAdmin;
    private String opId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());
        esAdmin = session.isAdmin();
        opId    = session.getOperarioId();

        viewModel = new ViewModelProvider(requireActivity()).get(ResiduoViewModel.class);

        recyclerView      = view.findViewById(R.id.rv_historial);
        chipGroupFiltro   = view.findViewById(R.id.chip_group_filtro);
        etBuscar          = view.findViewById(R.id.et_buscar);
        tvVacio           = view.findViewById(R.id.tv_vacio);



        adapter = new ResiduoAdapter(this::mostrarDetalle);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Swipe eliminar solo para ADMIN
        if (esAdmin) {
            setupSwipeEliminar();
        }

        chipGroupFiltro.setOnCheckedStateChangeListener((g, ids) -> filtrar());

        etBuscar.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            public void onTextChanged(CharSequence s, int i, int b, int c) { filtrar(); }
            public void afterTextChanged(android.text.Editable s) {}
        });

        viewModel.todosLosResiduos.observe(getViewLifecycleOwner(), lista -> {
            if (lista == null) { listaCompleta = new ArrayList<>(); filtrar(); return; }

            if (esAdmin) {
                listaCompleta = lista;
            } else {
                List<Residuo> soloSuyos = new ArrayList<>();
                for (Residuo r : lista) {
                    if (opId != null && opId.equalsIgnoreCase(r.operarioId)) {
                        soloSuyos.add(r);
                    }
                }
                listaCompleta = soloSuyos;
            }
            filtrar();
        });
    }

    private void filtrar() {
        List<Residuo> resultado = new ArrayList<>(listaCompleta);
        String query = etBuscar.getText().toString().toLowerCase().trim();

        int chipId = chipGroupFiltro.getCheckedChipId();
        if      (chipId == R.id.chip_f_plastico)  filtrarPorTipo(resultado, "plastico");
        else if (chipId == R.id.chip_f_organico)  filtrarPorTipo(resultado, "organico");
        else if (chipId == R.id.chip_f_papel)     filtrarPorTipo(resultado, "papel");
        else if (chipId == R.id.chip_f_metal)     filtrarPorTipo(resultado, "metal");
        else if (chipId == R.id.chip_f_peligroso) filtrarPorTipo(resultado, "peligroso");
        else if (chipId == R.id.chip_f_pendiente) resultado.removeIf(r -> r.sincronizado);

        if (!query.isEmpty()) {
            resultado.removeIf(r ->
                (r.tipo == null           || !r.tipo.toLowerCase().contains(query)) &&
                (r.zona == null           || !r.zona.toLowerCase().contains(query)) &&
                (r.ubicacion == null      || !r.ubicacion.toLowerCase().contains(query)) &&
                (r.operarioNombre == null || !r.operarioNombre.toLowerCase().contains(query))
            );
        }

        adapter.submitList(resultado);

        if (resultado.isEmpty()) {
            tvVacio.setText(esAdmin
                ? "Sin registros\nIntenta con otro filtro"
                : "No tienes registros aún\nUsa 'Nuevo Registro' para comenzar");
            tvVacio.setVisibility(View.VISIBLE);
        } else {
            tvVacio.setVisibility(View.GONE);
        }
    }

    private void filtrarPorTipo(List<Residuo> lista, String tipo) {
        lista.removeIf(r -> r.tipo == null || !r.tipo.equalsIgnoreCase(tipo));
    }

    // ── Swipe eliminar (solo ADMIN) ──
    private void setupSwipeEliminar() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                List<Residuo> listaActual = new ArrayList<>(adapter.getCurrentList());
                if (pos < 0 || pos >= listaActual.size()) return;

                Residuo eliminado = listaActual.get(pos);
                viewModel.eliminar(eliminado);

                Snackbar.make(recyclerView, "Registro eliminado", Snackbar.LENGTH_LONG)
                    .setAction("DESHACER", v -> viewModel.guardarResiduo(eliminado))
                    .setActionTextColor(getResources().getColor(R.color.accent, null))
                    .show();
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isActive) {
                android.graphics.Paint paint = new android.graphics.Paint();
                paint.setColor(android.graphics.Color.parseColor("#FFEBEE"));
                android.view.View itemView = viewHolder.itemView;
                if (dX < 0) {
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                               itemView.getRight(), itemView.getBottom(), paint);
                } else {
                    c.drawRect(itemView.getLeft(), itemView.getTop(),
                               itemView.getLeft() + dX, itemView.getBottom(), paint);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isActive);
            }
        }).attachToRecyclerView(recyclerView);
    }

    // ── Detalle del registro — con botón EDITAR ──
    private void mostrarDetalle(Residuo r) {
        boolean puedeEditar = esAdmin || (opId != null && opId.equalsIgnoreCase(r.operarioId));

        // Inflar layout personalizado con ImageView para la foto
        android.view.View dv = android.view.LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_detalle_residuo, null);

        // Rellenar datos de texto
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_tipo))
            .setText("🗂 Tipo: " + r.tipo);
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_peso))
            .setText("⚖️ Peso: " + r.pesoKg + " kg  |  Vol: "
                + String.format("%.4f m³", r.volumenM3));
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_fecha))
            .setText("📅 Fecha: " + r.fecha);
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_ubicacion))
            .setText("📍 Ubicación: " + (r.ubicacion != null && !r.ubicacion.isEmpty()
                ? r.ubicacion : "—"));
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_zona))
            .setText("🏭 Zona: " + (r.zona != null && !r.zona.isEmpty() ? r.zona : "—"));
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_operario))
            .setText("👷 Operario: " + r.operarioNombre + " (" + r.operarioId + ")");
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_epp))
            .setText("🦺 EPP: " + (r.eppConfirmado ? "✓ Confirmado" : "N/A"));
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_obs))
            .setText("📝 Obs: " + (r.observaciones != null && !r.observaciones.isEmpty()
                ? r.observaciones : "—"));
        ((android.widget.TextView) dv.findViewById(R.id.tv_detalle_sync))
            .setText("🔄 Sync: " + (r.sincronizado ? "✓ Sincronizado" : "⏳ Pendiente"));

        // Mostrar foto si existe en archivoOrigen
        android.view.View frameFoto = dv.findViewById(R.id.frame_foto_detalle);
        android.widget.ImageView ivFoto = dv.findViewById(R.id.iv_foto_detalle);
        if (r.archivoOrigen != null && r.archivoOrigen.startsWith("content://")) {
            frameFoto.setVisibility(android.view.View.VISIBLE);
            // Cargar en hilo secundario para no trabar UI
            new Thread(() -> {
                try {
                    Uri uri = Uri.parse(r.archivoOrigen);
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 2;
                    java.io.InputStream is = requireContext()
                        .getContentResolver().openInputStream(uri);
                    Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
                    if (is != null) is.close();
                    if (bmp != null) {
                        requireActivity().runOnUiThread(() -> ivFoto.setImageBitmap(bmp));
                    }
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() ->
                        frameFoto.setVisibility(android.view.View.GONE));
                }
            }).start();
        }

        android.app.AlertDialog.Builder builder =
            new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Detalle del Registro")
                .setView(dv)
                .setPositiveButton("Cerrar", null);

        // Botón EDITAR (disponible si puede editar)
        if (puedeEditar) {
            builder.setNeutralButton("✏️ Editar", (d, w) -> {
                // Navegar a EditarRegistroFragment pasando el ID
                Bundle args = new Bundle();
                args.putInt(EditarRegistroFragment.ARG_RESIDUO_ID, r.id);
                Navigation.findNavController(requireView())
                        .navigate(R.id.editarRegistroFragment, args);
            });
        }

        // Botón ELIMINAR solo para ADMIN
        if (esAdmin) {
            builder.setNegativeButton("🗑 Eliminar", (d, w) ->
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Seguro que quieres eliminar este registro?\nEsta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar", (d2, w2) -> {
                        viewModel.eliminar(r);
                        Toast.makeText(requireContext(), "Registro eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show());
        }

        builder.show();
    }
}
