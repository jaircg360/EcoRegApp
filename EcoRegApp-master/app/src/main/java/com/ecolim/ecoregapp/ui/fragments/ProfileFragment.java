package com.ecolim.ecoregapp.ui.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.ui.activities.LoginActivity;
import com.ecolim.ecoregapp.utils.NotificationHelper;
import com.ecolim.ecoregapp.utils.NotificationScheduler;
import com.ecolim.ecoregapp.utils.OperariosManager;
import com.ecolim.ecoregapp.utils.SessionManager;
import com.ecolim.ecoregapp.viewmodel.ResiduoViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.Activity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ProfileFragment extends Fragment {

    private SessionManager    session;
    private ResiduoViewModel  viewModel;
    private SharedPreferences prefs;
    private OperariosManager  operariosManager;

    private de.hdodenhof.circleimageview.CircleImageView ivFotoPerfil;
    private TextView tvNombre, tvRol, tvOperarioId, tvTurno, tvPlanta;
    private TextView tvRegistros, tvKgTotal, tvDiasActivo;
    private SwitchMaterial switchNotificaciones, switchWifi;

    private Uri photoUri;
    private static final String PREFS_PROFILE = "profile_prefs";
    private static final String KEY_FOTO_PATH = "foto_path_interna";
    private static final String KEY_NOMBRE    = "nombre_usuario";
    private static final String KEY_TELEFONO  = "telefono";
    private static final String KEY_EMAIL     = "email";

    // ── Cámara (sin pedir permiso — MediaStore lo maneja) ────────────────────
    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && photoUri != null)
                procesarYGuardarFoto(photoUri);
        });

    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK
                    && result.getData() != null && result.getData().getData() != null)
                procesarYGuardarFoto(result.getData().getData());
        });

    private final ActivityResultLauncher<String> notifPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_profile, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session          = new SessionManager(requireContext());
        viewModel        = new ViewModelProvider(requireActivity()).get(ResiduoViewModel.class);
        prefs            = requireContext().getSharedPreferences(PREFS_PROFILE, 0);
        operariosManager = new OperariosManager(requireContext());

        pedirPermisoNotificaciones();
        bindViews(view);
        cargarDatosPerfil();
        cargarFotoGuardada();

        
        view.findViewById(R.id.btn_editar_perfil).setOnClickListener(v -> mostrarOpcionesEditar());

        setupSwitches();
        configurarSeccionAdmin();
        setupOpciones(view);
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void bindViews(View v) {
        ivFotoPerfil         = v.findViewById(R.id.iv_foto_perfil);
        tvNombre             = v.findViewById(R.id.tv_nombre_perfil);
        tvRol                = v.findViewById(R.id.tv_rol_perfil);
        tvOperarioId         = v.findViewById(R.id.tv_badge_id);
        tvTurno              = v.findViewById(R.id.tv_badge_turno);
        tvPlanta             = v.findViewById(R.id.tv_badge_planta);
        tvRegistros          = v.findViewById(R.id.tv_stat_registros);
        tvKgTotal            = v.findViewById(R.id.tv_stat_kg);
        tvDiasActivo         = v.findViewById(R.id.tv_stat_dias);
        switchNotificaciones = v.findViewById(R.id.switch_notificaciones);
        switchWifi           = v.findViewById(R.id.switch_wifi);
    }

    private void cargarDatosPerfil() {
        // Nombre: siempre partir del nombre de sesión del usuario actual
        String nombreSesion = session.getOperarioNombre();
        if (nombreSesion == null || nombreSesion.isEmpty()) nombreSesion = session.getOperarioId();
        // Solo usar el nombre personalizado si fue guardado para este mismo usuario
        String nombreGuardado = prefs.getString(KEY_NOMBRE, "");
        String nombre = (nombreGuardado != null && !nombreGuardado.isEmpty())
            ? nombreGuardado : nombreSesion;
        tvNombre.setText(nombre);

        // Rol según tipo de cuenta
        tvRol.setText(session.isAdmin() ? "Administrador · ECOLIM S.A.C." : "Operario · ECOLIM S.A.C.");
        tvOperarioId.setText(session.getOperarioId());
        String turno = session.getTurno();
        tvTurno.setText("Turno " + (turno.isEmpty() ? "M" : turno.substring(0, 1).toUpperCase()));
        tvPlanta.setText(session.getPlanta().isEmpty() ? "Planta A" : session.getPlanta());

        TextView tvIniciales = requireView().findViewById(R.id.tv_iniciales);
        if (tvIniciales != null) {
            // Usar el nombre ya resuelto arriba
            String n = nombre.isEmpty() ? session.getOperarioId() : nombre;
            // Para "Operario OP-01" mostrar "OP", para nombre propio mostrar primeras 2 letras
            String iniciales;
            String[] partes = n.split(" ");
            if (partes.length >= 2) {
                iniciales = (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
            } else {
                iniciales = n.length() >= 2 ? n.substring(0, 2).toUpperCase() : n.toUpperCase();
            }
            tvIniciales.setText(iniciales);
        }
        switchNotificaciones.setChecked(prefs.getBoolean("notif", true));
        switchWifi.setChecked(prefs.getBoolean("wifi", false));
        cargarStatsReales();
    }

    private void cargarStatsReales() {
        viewModel.cargarStatsOperario(session.getOperarioId());
        viewModel.getStatsOperario().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null || !isAdded()) return;
            if (tvRegistros != null) tvRegistros.setText(String.valueOf(stats[0]));
            if (tvKgTotal   != null) tvKgTotal.setText(stats[1] + " kg");
            if (tvDiasActivo!= null) tvDiasActivo.setText(String.valueOf(stats[2]));
        });
    }

    // ── Foto — SIN pedir permiso (la cámara del sistema no lo necesita) ───────
    private void cargarFotoGuardada() {
        String path = prefs.getString(KEY_FOTO_PATH, null);
        if (path == null) return;
        File f = new File(path);
        if (!f.exists()) { prefs.edit().remove(KEY_FOTO_PATH).apply(); return; }
        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
        if (bmp != null) mostrarFotoEnImageView(bmp);
    }

    private void mostrarFotoEnImageView(Bitmap bmp) {
        ivFotoPerfil.setImageBitmap(bmp);
        View tv = requireView().findViewById(R.id.tv_iniciales);
        if (tv != null) tv.setVisibility(View.GONE);
    }

    private void procesarYGuardarFoto(Uri uri) {
        new Thread(() -> {
            try {
                Bitmap bmp = decodeBitmapFromUri(uri, 512);
                if (bmp == null) throw new Exception("null bitmap");
                File dest = new File(requireContext().getFilesDir(), "profile_photo.jpg");
                FileOutputStream fos = new FileOutputStream(dest);
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.close();
                prefs.edit().putString(KEY_FOTO_PATH, dest.getAbsolutePath()).apply();
                Bitmap fb = bmp;
                requireActivity().runOnUiThread(() -> {
                    mostrarFotoEnImageView(fb);
                    Toast.makeText(requireContext(), "✅ Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                    NotificationHelper.enviar(requireContext(),
                        "Foto actualizada",
                        "Tu foto de perfil fue guardada correctamente.");
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Error al procesar la foto", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private Bitmap decodeBitmapFromUri(Uri uri, int maxSize) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) return null;
            BitmapFactory.decodeStream(is, null, opts); is.close();
            int s = 1;
            while (opts.outWidth / s > maxSize || opts.outHeight / s > maxSize) s *= 2;
            opts.inJustDecodeBounds = false; opts.inSampleSize = s;
            is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap b = BitmapFactory.decodeStream(is, null, opts); is.close();
            return b;
        } catch (Exception e) { return null; }
    }

    private void mostrarOpcionesFoto() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Foto de perfil")
            .setItems(new String[]{"📷 Tomar foto", "🖼️ Elegir de galería", "Cancelar"},
                (d, w) -> { if (w == 0) abrirCamara(); else if (w == 1) abrirGaleria(); })
            .show();
    }

    private void mostrarOpcionesEditar() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Editar perfil")
            .setItems(new String[]{"✏️ Datos personales", "📷 Cambiar foto", "🔑 Contraseña", "Cancelar"},
                (d, w) -> {
                    if (w == 0) mostrarDialogEditarDatos();
                    else if (w == 1) mostrarOpcionesFoto();
                    else if (w == 2) mostrarDialogCambiarPassword();
                })
            .show();
    }

    // ── Cámara SIN permiso explícito (la app del sistema tiene el permiso) ────
    private void abrirCamara() {
        try {
            File temp = File.createTempFile("FOTO_PERFIL_", ".jpg", requireContext().getCacheDir());
            photoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", temp);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            // Sin addFlags — FileProvider otorga acceso automáticamente (evita SecurityException)
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error al abrir cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleria() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(Intent.createChooser(i, "Seleccionar foto"));
    }

    // ── Dialogs de edición ────────────────────────────────────────────────────
    private void mostrarDialogEditarDatos() {
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_datos, null);
        EditText etNombre   = dv.findViewById(R.id.et_dialog_nombre);
        EditText etTelefono = dv.findViewById(R.id.et_dialog_telefono);
        EditText etEmail    = dv.findViewById(R.id.et_dialog_email);
        etNombre.setText(prefs.getString(KEY_NOMBRE, session.getOperarioNombre()));
        etTelefono.setText(prefs.getString(KEY_TELEFONO, ""));
        etEmail.setText(prefs.getString(KEY_EMAIL, ""));

        new AlertDialog.Builder(requireContext()).setTitle("Datos personales").setView(dv)
            .setPositiveButton("Guardar", (d, w) -> {
                String n = etNombre.getText().toString().trim();
                if (!n.isEmpty()) {
                    prefs.edit()
                        .putString(KEY_NOMBRE, n)
                        .putString(KEY_TELEFONO, etTelefono.getText().toString().trim())
                        .putString(KEY_EMAIL, etEmail.getText().toString().trim())
                        .apply();
                    tvNombre.setText(n);
                    Toast.makeText(requireContext(), "✅ Datos actualizados", Toast.LENGTH_SHORT).show();
                    NotificationHelper.enviar(requireContext(),
                        "Perfil actualizado",
                        "Tus datos personales fueron guardados correctamente.");
                }
            }).setNegativeButton("Cancelar", null).show();
    }

    private void mostrarDialogCambiarPassword() {
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cambiar_password, null);
        EditText etActual  = dv.findViewById(R.id.et_password_actual);
        EditText etNueva   = dv.findViewById(R.id.et_password_nueva);
        EditText etConfirm = dv.findViewById(R.id.et_password_confirmar);

        new AlertDialog.Builder(requireContext()).setTitle("Cambiar contraseña").setView(dv)
            .setPositiveButton("Cambiar", (d, w) -> {
                String nueva = etNueva.getText().toString();
                String conf  = etConfirm.getText().toString();
                if (nueva.length() < 6) {
                    Toast.makeText(requireContext(), "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show();
                } else if (!nueva.equals(conf)) {
                    Toast.makeText(requireContext(), "❌ Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show();
                    NotificationHelper.enviar(requireContext(),
                        "Contraseña cambiada",
                        "Tu contraseña fue actualizada. Si no fuiste tú, contacta al administrador.");
                }
            }).setNegativeButton("Cancelar", null).show();
    }

    // ── Switch notificaciones — ON activa, OFF silencia ───────────────────────
    private void setupSwitches() {
        switchNotificaciones.setOnCheckedChangeListener((b, on) -> {
            prefs.edit()
                .putBoolean("notif", on)
                .putBoolean("notificaciones_enabled", on)
                .apply();
            NotificationScheduler scheduler = new NotificationScheduler(requireContext());
            if (on) {
                scheduler.programarRecordatorioDiario();
                scheduler.programarResumenTurno();
                Toast.makeText(requireContext(), "🔔 Notificaciones activadas", Toast.LENGTH_SHORT).show();
            } else {
                scheduler.cancelarTodas();
                Toast.makeText(requireContext(), "🔕 Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
            }
        });
        switchWifi.setOnCheckedChangeListener((b, on) ->
            prefs.edit().putBoolean("wifi", on).apply());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────
    private void configurarSeccionAdmin() {
        if (!session.isAdmin()) return;
        requireView().findViewById(R.id.tv_seccion_admin).setVisibility(View.VISIBLE);
        requireView().findViewById(R.id.card_admin).setVisibility(View.VISIBLE);
    }

    private void mostrarListaOperarios() {
        List<OperariosManager.Operario> lista = operariosManager.getCreados();
        if (lista.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                .setTitle("👥 Operarios")
                .setMessage("No hay operarios creados aún.")
                .setPositiveButton("➕ Crear", (d, w) -> mostrarDialogCrearOperario())
                .setNegativeButton("Cerrar", null).show();
            return;
        }
        String[] items = new String[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            OperariosManager.Operario op = lista.get(i);
            items[i] = op.id + "  ·  " + op.nombre + "  ·  " + op.turno;
        }
        new AlertDialog.Builder(requireContext())
            .setTitle("👥 Operarios (" + lista.size() + ")")
            .setItems(items, (d, w) -> mostrarOpcionesOperario(lista.get(w)))
            .setPositiveButton("➕ Crear nuevo", (d, w) -> mostrarDialogCrearOperario())
            .setNegativeButton("Cerrar", null).show();
    }

    private void mostrarOpcionesOperario(OperariosManager.Operario op) {
        new AlertDialog.Builder(requireContext())
            .setTitle("👤 " + op.nombre)
            .setMessage("ID: " + op.id + "\nTurno: " + op.turno + "\nPlanta: " + op.planta +
                        "\nEstado: " + (op.activo ? "✅ Activo" : "❌ Inactivo"))
            .setNeutralButton("🗑 Eliminar", (d, w) -> confirmarEliminar(op))
            .setPositiveButton("Cerrar", null).show();
    }

    private void confirmarEliminar(OperariosManager.Operario op) {
        new AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Eliminar operario")
            .setMessage("¿Estás seguro de que deseas eliminar la cuenta de:\n\n"
                + op.nombre + " (" + op.id + ")?\n\n"
                + "⚠️ Esta acción no se puede deshacer.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sí, eliminar", (d, w) -> {
                operariosManager.eliminar(op.id);
                Toast.makeText(requireContext(),
                    "✅ Cuenta de " + op.id + " eliminada", Toast.LENGTH_SHORT).show();
                NotificationHelper.enviar(requireContext(),
                    "Operario eliminado",
                    "La cuenta de " + op.nombre + " (" + op.id + ") fue eliminada del sistema.");
            })
            .setNegativeButton("Cancelar", null).show();
    }

    private void mostrarDialogCrearOperario() {
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_crear_operario, null);
        EditText etId       = dv.findViewById(R.id.et_op_id);
        EditText etNombre   = dv.findViewById(R.id.et_op_nombre);
        EditText etPassword = dv.findViewById(R.id.et_op_password);
        Spinner  spTurno    = dv.findViewById(R.id.spinner_turno);
        Spinner  spPlanta   = dv.findViewById(R.id.spinner_planta);
        etId.setText(operariosManager.generarSiguienteId());
        String[] turnos  = {"Mañana", "Tarde", "Noche"};
        String[] plantas = {"Planta A", "Planta B", "Planta C"};
        spTurno.setAdapter(new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, turnos));
        spPlanta.setAdapter(new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, plantas));

        new AlertDialog.Builder(requireContext()).setTitle("➕ Crear operario").setView(dv)
            .setPositiveButton("Crear cuenta", (d, w) -> {
                String id   = etId.getText().toString().trim().toUpperCase();
                String nom  = etNombre.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();
                String tur  = turnos[spTurno.getSelectedItemPosition()];
                String pla  = plantas[spPlanta.getSelectedItemPosition()];
                if (TextUtils.isEmpty(id) || !id.matches("OP-\\d+")) {
                    Toast.makeText(requireContext(), "❌ Formato: OP-XX", Toast.LENGTH_SHORT).show(); return;
                }
                if (TextUtils.isEmpty(nom)) {
                    Toast.makeText(requireContext(), "❌ Ingresa nombre", Toast.LENGTH_SHORT).show(); return;
                }
                if (pass.length() < 6) {
                    Toast.makeText(requireContext(), "❌ Mínimo 6 caracteres", Toast.LENGTH_SHORT).show(); return;
                }
                if (operariosManager.crear(new OperariosManager.Operario(id, nom, pass, tur, pla))) {
                    Toast.makeText(requireContext(),
                        "✅ " + id + " creado exitosamente", Toast.LENGTH_LONG).show();
                    NotificationHelper.enviar(requireContext(),
                        "Nuevo operario creado",
                        nom + " (" + id + ") fue agregado al sistema. " + tur + " · " + pla + ".");
                } else {
                    Toast.makeText(requireContext(), "❌ El ID " + id + " ya existe", Toast.LENGTH_SHORT).show();
                }
            }).setNegativeButton("Cancelar", null).show();
    }

    // ── Opciones generales ────────────────────────────────────────────────────
    private void setupOpciones(View v) {
        v.findViewById(R.id.item_datos_personales)
            .setOnClickListener(x -> mostrarDialogEditarDatos());
        v.findViewById(R.id.item_cambiar_password)
            .setOnClickListener(x -> mostrarDialogCambiarPassword());
        v.findViewById(R.id.item_sincronizacion)
            .setOnClickListener(x ->
                Toast.makeText(requireContext(), "🔄 Sincronizando...", Toast.LENGTH_SHORT).show());

        v.findViewById(R.id.item_limpiar_cache).setOnClickListener(x ->
            new AlertDialog.Builder(requireContext())
                .setTitle("Limpiar caché")
                .setMessage("¿Estás seguro de que deseas eliminar los datos temporales de la app?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Sí, limpiar", (d, w) -> {
                    String path = prefs.getString(KEY_FOTO_PATH, null);
                    if (path != null) new File(path).delete();
                    prefs.edit().remove(KEY_FOTO_PATH).apply();
                    ivFotoPerfil.setImageResource(R.drawable.bg_avatar_circle);
                    View ini = requireView().findViewById(R.id.tv_iniciales);
                    if (ini != null) ini.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "✅ Caché limpiado", Toast.LENGTH_SHORT).show();
                    NotificationHelper.enviar(requireContext(),
                        "Caché limpiado",
                        "Los datos temporales de la app fueron eliminados correctamente.");
                })
                .setNegativeButton("Cancelar", null).show());

        if (session.isAdmin()) {
            v.findViewById(R.id.item_gestionar_operarios)
                .setOnClickListener(x -> mostrarListaOperarios());
            v.findViewById(R.id.item_crear_operario)
                .setOnClickListener(x -> mostrarDialogCrearOperario());
            v.findViewById(R.id.item_exportar_admin)
                .setOnClickListener(x ->
                    Toast.makeText(requireContext(), "📊 Generando reporte general...", Toast.LENGTH_SHORT).show());
        }

        v.findViewById(R.id.item_cerrar_sesion).setOnClickListener(x ->
            new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas cerrar tu sesión actual?")
                .setPositiveButton("Cerrar sesión", (d, w) -> {
                    session.cerrarSesion();
                    startActivity(new Intent(requireActivity(), LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    requireActivity().finish();
                }).setNegativeButton("Cancelar", null).show());
    }
}
