package com.ecolim.ecoregapp.ui.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.ecolim.ecoregapp.utils.NotificationHelper;
import com.ecolim.ecoregapp.utils.SessionManager;
import com.ecolim.ecoregapp.viewmodel.ResiduoViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class EditarRegistroFragment extends Fragment {

    public static final String ARG_RESIDUO_ID = "residuo_id";

    private ResiduoViewModel viewModel;
    private SessionManager session;

    private ChipGroup chipGroupTipo;
    private EditText etPeso, etUbicacion, etObservaciones;
    private Spinner spinnerZona;
    private CardView cardEpp;
    private CheckBox cbGuantes, cbMascarilla, cbLentes;
    private TextView tvFecha, tvOperario, tvTurno;

    private Residuo residuoOriginal;
    private int residuoId = -1;

    private static final String[] ZONAS = {"Zona A", "Zona B", "Zona C", "Zona D", "Zona E"};

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editar_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session   = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(ResiduoViewModel.class);

        if (getArguments() != null) residuoId = getArguments().getInt(ARG_RESIDUO_ID, -1);
        if (residuoId == -1) {
            Toast.makeText(requireContext(), "Error: registro no encontrado", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        bindViews(view);
        setupSpinnerZona();
        setupChipListener();
        cargarRegistro();

        view.findViewById(R.id.btn_back_editar).setOnClickListener(v ->
            Navigation.findNavController(view).popBackStack());
        view.findViewById(R.id.btn_guardar_editar).setOnClickListener(v ->
            intentarGuardar(view));
    }

    private void bindViews(View v) {
        chipGroupTipo   = v.findViewById(R.id.chip_group_tipo_editar);
        etPeso          = v.findViewById(R.id.et_peso_editar);
        etUbicacion     = v.findViewById(R.id.et_ubicacion_editar);
        etObservaciones = v.findViewById(R.id.et_observaciones_editar);
        spinnerZona     = v.findViewById(R.id.spinner_zona_editar);
        cardEpp         = v.findViewById(R.id.card_epp_editar);
        cbGuantes       = v.findViewById(R.id.cb_guantes_editar);
        cbMascarilla    = v.findViewById(R.id.cb_mascarilla_editar);
        cbLentes        = v.findViewById(R.id.cb_lentes_editar);
        tvFecha         = v.findViewById(R.id.tv_fecha_editar);
        tvOperario      = v.findViewById(R.id.tv_operario_editar);
        tvTurno         = v.findViewById(R.id.tv_turno_editar);
    }

    private void setupSpinnerZona() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, ZONAS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZona.setAdapter(adapter);
    }

    private void setupChipListener() {
        chipGroupTipo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean esPeligroso = !checkedIds.isEmpty() && checkedIds.get(0) == R.id.chip_e_peligroso;
            cardEpp.setVisibility(esPeligroso ? View.VISIBLE : View.GONE);
        });
    }

    private void cargarRegistro() {
        viewModel.todosLosResiduos.observe(getViewLifecycleOwner(), lista -> {
            if (lista == null || residuoOriginal != null) return;
            for (Residuo r : lista) {
                if (r.id == residuoId) { residuoOriginal = r; rellenarFormulario(r); break; }
            }
            if (residuoOriginal == null) {
                Toast.makeText(requireContext(), "Registro no encontrado", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }

    private void rellenarFormulario(Residuo r) {
        seleccionarChipTipo(r.tipo);
        etPeso.setText(String.valueOf(r.pesoKg));
        for (int i = 0; i < ZONAS.length; i++)
            if (ZONAS[i].equalsIgnoreCase(r.zona)) { spinnerZona.setSelection(i); break; }
        if (r.ubicacion != null) etUbicacion.setText(r.ubicacion);
        if (r.observaciones != null) etObservaciones.setText(r.observaciones);
        if ("peligroso".equalsIgnoreCase(r.tipo)) {
            cardEpp.setVisibility(View.VISIBLE);
            if (r.eppConfirmado) {
                cbGuantes.setChecked(true); cbMascarilla.setChecked(true); cbLentes.setChecked(true);
            }
        }
        tvFecha.setText(r.fecha != null ? r.fecha : "—");
        tvOperario.setText(r.operarioNombre != null ? r.operarioNombre : r.operarioId);
        tvTurno.setText(r.zona != null ? r.zona : "—");
    }

    private void seleccionarChipTipo(String tipo) {
        if (tipo == null) return;
        int chipId;
        switch (tipo.toLowerCase()) {
            case "plastico":  chipId = R.id.chip_e_plastico;  break;
            case "organico":  chipId = R.id.chip_e_organico;  break;
            case "papel":     chipId = R.id.chip_e_papel;     break;
            case "metal":     chipId = R.id.chip_e_metal;     break;
            case "peligroso": chipId = R.id.chip_e_peligroso; break;
            case "vidrio":    chipId = R.id.chip_e_vidrio;    break;
            default:          chipId = R.id.chip_e_pendiente; break;
        }
        Chip chip = requireView().findViewById(chipId);
        if (chip != null) chip.setChecked(true);
    }

    private void intentarGuardar(View rootView) {
        String pesoStr = etPeso.getText().toString().trim();
        if (pesoStr.isEmpty()) { etPeso.setError("El peso es obligatorio"); etPeso.requestFocus(); return; }
        double peso;
        try { peso = Double.parseDouble(pesoStr); }
        catch (NumberFormatException e) { etPeso.setError("Peso inválido"); etPeso.requestFocus(); return; }
        if (peso <= 0) { etPeso.setError("El peso debe ser mayor a 0"); etPeso.requestFocus(); return; }

        int chipChecked = chipGroupTipo.getCheckedChipId();
        if (chipChecked == View.NO_ID) {
            Toast.makeText(requireContext(), "Selecciona el tipo de residuo", Toast.LENGTH_SHORT).show(); return;
        }

        String tipo = obtenerTipoSeleccionado(chipChecked);

        if ("peligroso".equalsIgnoreCase(tipo)) {
            if (!cbGuantes.isChecked() || !cbMascarilla.isChecked() || !cbLentes.isChecked()) {
                new AlertDialog.Builder(requireContext())
                    .setTitle("⚠️ EPP incompleto")
                    .setMessage("Para residuos peligrosos debes confirmar:\n• Guantes\n• Mascarilla\n• Lentes de seguridad")
                    .setPositiveButton("Entendido", null).show();
                return;
            }
        }

        Residuo actualizado = residuoOriginal;
        actualizado.tipo          = tipo;
        actualizado.pesoKg        = peso;
        actualizado.zona          = spinnerZona.getSelectedItem().toString();
        actualizado.ubicacion     = etUbicacion.getText().toString().trim();
        actualizado.observaciones = etObservaciones.getText().toString().trim();
        actualizado.eppConfirmado = "peligroso".equalsIgnoreCase(tipo)
            && cbGuantes.isChecked() && cbMascarilla.isChecked() && cbLentes.isChecked();
        actualizado.sincronizado  = false;
        actualizado.volumenM3     = calcularVolumen(tipo, peso);

        // Confirmación antes de guardar
        new AlertDialog.Builder(requireContext())
            .setTitle("Guardar cambios")
            .setMessage("¿Confirmas los cambios en este registro?\n\nTipo: " + tipo + "\nPeso: " + peso + " kg")
            .setPositiveButton("Guardar", (d, w) -> {
                viewModel.actualizar(actualizado);
                Toast.makeText(requireContext(), "✅ Registro actualizado", Toast.LENGTH_SHORT).show();
                NotificationHelper.enviar(requireContext(),
                    "Registro editado",
                    "El registro de " + tipo + " (" + peso + " kg) fue actualizado correctamente.");
                Navigation.findNavController(rootView).popBackStack();
            })
            .setNegativeButton("Cancelar", null).show();
    }

    private String obtenerTipoSeleccionado(int chipId) {
        if (chipId == R.id.chip_e_plastico)  return "plastico";
        if (chipId == R.id.chip_e_organico)  return "organico";
        if (chipId == R.id.chip_e_papel)     return "papel";
        if (chipId == R.id.chip_e_metal)     return "metal";
        if (chipId == R.id.chip_e_peligroso) return "peligroso";
        if (chipId == R.id.chip_e_vidrio)    return "vidrio";
        return "pendiente";
    }

    private double calcularVolumen(String tipo, double pesoKg) {
        double densidad;
        switch (tipo.toLowerCase()) {
            case "plastico":  densidad = 50;   break;
            case "organico":  densidad = 300;  break;
            case "papel":     densidad = 89;   break;
            case "metal":     densidad = 2700; break;
            case "vidrio":    densidad = 2500; break;
            case "peligroso": densidad = 800;  break;
            default:          densidad = 200;  break;
        }
        return pesoKg / densidad;
    }
}
