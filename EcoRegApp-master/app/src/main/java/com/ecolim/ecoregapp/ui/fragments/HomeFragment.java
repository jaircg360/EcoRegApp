package com.ecolim.ecoregapp.ui.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.ecolim.ecoregapp.ui.adapters.ResiduoAdapter;
import com.ecolim.ecoregapp.utils.SessionManager;
import com.ecolim.ecoregapp.viewmodel.ResiduoViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private ResiduoViewModel viewModel;
    private SessionManager session;
    private ResiduoAdapter adapter;

    private TextView tvSaludo, tvNombre, tvPesoHoy, tvRegistrosHoy;
    private TextView tvPendientes, tvPeligrosos;
    private RecyclerView rvRecientes;
    private ProgressBar progressMeta;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session   = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(ResiduoViewModel.class);

        bindViews(view);
        setupGreeting();
        setupRecycler();
        observeData();
        setupQuickActions(view);
    }

    private void bindViews(View v) {
        tvSaludo       = v.findViewById(R.id.tv_saludo);
        tvNombre       = v.findViewById(R.id.tv_nombre_operario);
        tvPesoHoy      = v.findViewById(R.id.tv_peso_hoy);
        tvRegistrosHoy = v.findViewById(R.id.tv_registros_hoy);
        tvPendientes   = v.findViewById(R.id.tv_pendientes);
        tvPeligrosos   = v.findViewById(R.id.tv_peligrosos);
        rvRecientes    = v.findViewById(R.id.rv_recientes);
        progressMeta   = v.findViewById(R.id.progress_meta);
    }

    private void setupGreeting() {
        int hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String saludo = hora < 12 ? "Buenos días," : hora < 18 ? "Buenas tardes," : "Buenas noches,";
        tvSaludo.setText(saludo);

        // Mostrar nombre limpio: si es ADMIN → "Administrador", si es operario → su nombre o ID
        String nombre = session.getOperarioNombre();
        if (nombre == null || nombre.isEmpty()) nombre = session.getOperarioId();
        tvNombre.setText(nombre + " 👋");
    }

    private void setupRecycler() {
        adapter = new ResiduoAdapter(residuo -> { });
        rvRecientes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecientes.setAdapter(adapter);
        rvRecientes.setNestedScrollingEnabled(false);
    }

    private void observeData() {
        viewModel.todosLosResiduos.observe(getViewLifecycleOwner(), lista -> {
            int size = Math.min(lista.size(), 5);
            adapter.submitList(lista.subList(0, size));

            String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            double pesoHoy  = 0;
            int countHoy    = 0;
            int peligrosos  = 0;
            int pendientes  = 0;

            // Reemplazado "var" por tipo explícito "Residuo"
            for (Residuo r : lista) {
                if (r.fecha != null && r.fecha.startsWith(hoy)) {
                    pesoHoy += r.pesoKg;
                    countHoy++;
                }
                if (r.tipo != null && r.tipo.equalsIgnoreCase("peligroso")) peligrosos++;
                if (!r.sincronizado) pendientes++;
            }

            tvPesoHoy.setText(String.format("%.1f kg", pesoHoy));
            tvRegistrosHoy.setText(String.valueOf(countHoy));
            tvPendientes.setText(String.valueOf(pendientes));
            tvPeligrosos.setText(String.valueOf(peligrosos));

            int progreso = (int) Math.min((pesoHoy / 63.0) * 100, 100);
            progressMeta.setProgress(progreso);
        });
    }

    private void setupQuickActions(View v) {
        v.findViewById(R.id.card_nuevo_registro).setOnClickListener(x ->
                Navigation.findNavController(v).navigate(R.id.registroFragment));
        v.findViewById(R.id.card_importar).setOnClickListener(x ->
                Navigation.findNavController(v).navigate(R.id.importarFragment));
        v.findViewById(R.id.card_reportes).setOnClickListener(x ->
                Navigation.findNavController(v).navigate(R.id.reportesFragment));
        v.findViewById(R.id.card_historial).setOnClickListener(x ->
                Navigation.findNavController(v).navigate(R.id.historialFragment));
        v.findViewById(R.id.tv_ver_todos).setOnClickListener(x ->
                Navigation.findNavController(v).navigate(R.id.historialFragment));
    }
}
