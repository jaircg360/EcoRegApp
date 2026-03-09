package com.ecolim.ecoregapp.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.ecolim.ecoregapp.utils.FileManager;
import com.ecolim.ecoregapp.utils.NotificationHelper;
import com.ecolim.ecoregapp.utils.SessionManager;
import com.ecolim.ecoregapp.viewmodel.ResiduoViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistroFragment extends Fragment {

    private ResiduoViewModel viewModel;
    private SessionManager session;
    private ChipGroup chipGroupTipo;
    private TextInputEditText etPeso, etUbicacion, etZona, etObservaciones;
    private LinearLayout layoutEPP, layoutFotoPreview;
    private CheckBox cbEppConfirmado;
    private Button btnGuardar;
    private ImageView ivFotoEvidencia;
    private TextView tvFotoNombre;
    private String tipoSeleccionado = "plastico";
    private Uri photoUri;
    private String rutaFoto = null;

    // ── Permiso POST_NOTIFICATIONS (Android 13+) ──────────────────────────
    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && photoUri != null)
                    procesarFotoEnThread(photoUri);
            });

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) abrirCamara();
                else Toast.makeText(requireContext(), "Permiso de cámara requerido", Toast.LENGTH_SHORT).show();
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session   = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(ResiduoViewModel.class);
        pedirPermisoNotificaciones();
        bindViews(view);
        setupChipsTipo();
        setupCamara(view);
        setupBotones(view);
        observeViewModel();

        // ── Botón atrás: usa btn_back_registro (ID real del XML) ──────────
        view.findViewById(R.id.btn_back_registro)
                .setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void bindViews(View v) {
        // ── IDs reales del fragment_registro.xml ─────────────────────────
        chipGroupTipo     = v.findViewById(R.id.chip_group_tipo);
        etPeso            = v.findViewById(R.id.et_peso);
        etUbicacion       = v.findViewById(R.id.et_ubicacion);
        etZona            = v.findViewById(R.id.et_zona);
        etObservaciones   = v.findViewById(R.id.et_observaciones);
        layoutEPP         = v.findViewById(R.id.layout_epp_alert);   // layout_epp_alert
        cbEppConfirmado   = v.findViewById(R.id.cb_epp_confirmado);  // un solo checkbox
        btnGuardar        = v.findViewById(R.id.btn_guardar_registro);// btn_guardar_registro
        ivFotoEvidencia   = v.findViewById(R.id.iv_foto_evidencia);
        layoutFotoPreview = v.findViewById(R.id.layout_foto_preview);
        tvFotoNombre      = v.findViewById(R.id.tv_foto_nombre);
    }

    private void setupChipsTipo() {
        // Seleccionar plástico por defecto al inicio
        Chip chipPlastico = chipGroupTipo.findViewById(R.id.chip_tipo_plastico);
        if (chipPlastico != null) chipPlastico.setChecked(true);

        chipGroupTipo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null) return;

            // Obtener tipo desde el tag; si no tiene tag, deducir del ID
            Object tag = chip.getTag();
            if (tag != null) {
                tipoSeleccionado = tag.toString();
            } else {
                tipoSeleccionado = tipoDesdeId(chip.getId());
            }

            boolean esPeligroso = tipoSeleccionado.equals("peligroso");
            layoutEPP.setVisibility(esPeligroso ? View.VISIBLE : View.GONE);
            if (!esPeligroso && cbEppConfirmado != null) {
                cbEppConfirmado.setChecked(false);
            }
        });
    }

    /** Deduce el tipo de residuo a partir del ID del chip (fallback sin tag) */
    private String tipoDesdeId(int id) {
        if (id == R.id.chip_tipo_plastico)  return "plastico";
        if (id == R.id.chip_tipo_organico)  return "organico";
        if (id == R.id.chip_tipo_papel)     return "papel";
        if (id == R.id.chip_tipo_peligroso) return "peligroso";
        if (id == R.id.chip_tipo_otros)     return "otros";
        return "plastico";
    }

    private void setupCamara(View v) {
        v.findViewById(R.id.btn_tomar_foto).setOnClickListener(x -> verificarPermisoCamara());
        View btnQuitar = v.findViewById(R.id.btn_quitar_foto);
        if (btnQuitar != null) {
            btnQuitar.setOnClickListener(x -> {
                rutaFoto = null; photoUri = null;
                layoutFotoPreview.setVisibility(View.GONE);
            });
        }
    }

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            permLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            // Usar getFilesDir() en vez de getCacheDir() — evita SecurityException en Android 12+
            File dir = new File(requireContext().getFilesDir(), "evidencias");
            if (!dir.exists()) dir.mkdirs();
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File fotoFile = new File(dir, "EVIDENCIA_" + ts + ".jpg");
            photoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", fotoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error al abrir la cámara: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void procesarFotoEnThread(Uri uri) {
        new Thread(() -> {
            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("No se pudo abrir la imagen");
                BitmapFactory.decodeStream(is, null, opts); is.close();
                int sample = 1;
                while (opts.outWidth / sample > 800 || opts.outHeight / sample > 800) sample *= 2;
                opts.inJustDecodeBounds = false; opts.inSampleSize = sample;
                is = requireContext().getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
                if (is != null) is.close();
                if (bmp == null) throw new Exception("Bitmap nulo");
                rutaFoto = uri.toString();
                requireActivity().runOnUiThread(() -> {
                    ivFotoEvidencia.setImageBitmap(bmp);
                    tvFotoNombre.setText("📷 Foto de evidencia adjunta");
                    layoutFotoPreview.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "✅ Foto capturada", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error al procesar la foto", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean validar() {
        String pesoStr = etPeso.getText() != null ? etPeso.getText().toString().trim() : "";
        if (TextUtils.isEmpty(pesoStr)) {
            etPeso.setError("Ingresa el peso");
            return false;
        }
        etPeso.setError(null);
        try {
            if (Double.parseDouble(pesoStr.replace(",", ".")) <= 0) {
                etPeso.setError("El peso debe ser mayor a 0");
                return false;
            }
        } catch (NumberFormatException e) {
            etPeso.setError("Peso inválido");
            return false;
        }
        if (tipoSeleccionado.equals("peligroso") && cbEppConfirmado != null
                && !cbEppConfirmado.isChecked()) {
            Toast.makeText(requireContext(), "Confirma el uso de EPP para residuo peligroso",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void guardarResiduo() {
        double peso = Double.parseDouble(etPeso.getText().toString().trim().replace(",", "."));
        String zona = etZona.getText() != null ? etZona.getText().toString().trim() : "";
        String ubic = etUbicacion.getText() != null ? etUbicacion.getText().toString().trim() : "";
        String obs  = etObservaciones.getText() != null ? etObservaciones.getText().toString().trim() : "";

        Residuo r = new Residuo(tipoSeleccionado, peso, FileManager.fechaActual(),
                ubic, zona, session.getOperarioId(), session.getOperarioNombre());
        r.observaciones = obs;
        r.eppConfirmado = tipoSeleccionado.equals("peligroso")
                && cbEppConfirmado != null && cbEppConfirmado.isChecked();

        if (rutaFoto != null) {
            r.archivoOrigen = rutaFoto; // URI persistida para ver foto en historial
            if (!r.observaciones.contains("📷"))
                r.observaciones += (r.observaciones.isEmpty() ? "" : " | ") + "📷 Con foto evidencia";
        }

        viewModel.guardarResiduo(r);

        NotificationHelper helper = new NotificationHelper(requireContext());
        if ("peligroso".equalsIgnoreCase(r.tipo)) {
            helper.notificarResiduoPeligroso(zona.isEmpty() ? "zona no especificada" : zona, peso);
        } else {
            NotificationHelper.enviar(requireContext(),
                    "✅ Nuevo registro guardado",
                    "Se registró " + peso + " kg de " + tipoSeleccionado.toUpperCase()
                            + (ubic.isEmpty() ? "" : " en " + ubic) + ".");
        }
        verificarMetaDiaria(peso, helper);
    }

    private void setupBotones(View view) {
        btnGuardar.setOnClickListener(x -> { if (validar()) guardarResiduo(); });

        // Botón "Guardar y nuevo" — opcional, puede no existir en este layout
        View btnNuevo = view.findViewById(R.id.btn_guardar_registro);
        if (btnNuevo != null) {
            btnNuevo.setOnClickListener(x -> {
                if (validar()) {
                    guardarResiduo();
                    limpiarFormulario();
                }
            });
        }
    }

    private void limpiarFormulario() {
        etPeso.setText(""); etUbicacion.setText(""); etZona.setText(""); etObservaciones.setText("");
        // Seleccionar primer chip (plástico) de nuevo
        Chip chipPlastico = chipGroupTipo.findViewById(R.id.chip_tipo_plastico);
        if (chipPlastico != null) chipPlastico.setChecked(true);
        layoutEPP.setVisibility(View.GONE);
        layoutFotoPreview.setVisibility(View.GONE);
        if (cbEppConfirmado != null) cbEppConfirmado.setChecked(false);
        tipoSeleccionado = "plastico"; rutaFoto = null; photoUri = null;
    }

    private void verificarMetaDiaria(double pesoNuevo, NotificationHelper helper) {
        final double META_KG = 63.0;
        final String hoy = FileManager.fechaActual();
        viewModel.todosLosResiduos.observe(getViewLifecycleOwner(), lista -> {
            if (lista == null) return;
            double totalHoy = 0;
            for (Residuo item : lista)
                if (item.fecha != null && item.fecha.startsWith(hoy)) totalHoy += item.pesoKg;
            boolean yaNotif = requireContext()
                    .getSharedPreferences("notif_prefs", 0)
                    .getBoolean("meta_notificada_" + hoy, false);
            if (totalHoy >= META_KG && !yaNotif) {
                helper.notificarMetaAlcanzada(totalHoy);
                requireContext().getSharedPreferences("notif_prefs", 0)
                        .edit().putBoolean("meta_notificada_" + hoy, true).apply();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getGuardadoExitoso().observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                viewModel.resetGuardado();
                Navigation.findNavController(requireView()).navigate(R.id.successFragment);
            }
        });
    }
}