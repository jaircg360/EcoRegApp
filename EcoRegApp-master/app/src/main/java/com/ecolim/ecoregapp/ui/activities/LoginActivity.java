package com.ecolim.ecoregapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.utils.OperariosManager;
import com.ecolim.ecoregapp.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etOperarioId, etPassword;
    private Button btnIngresar;
    private ProgressBar progressBar;
    private SessionManager session;
    private OperariosManager operariosManager;

    private static final String PASSWORD_ADMIN = "ecolim2026";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        operariosManager = new OperariosManager(this);

        if (session.isLoggedIn()) { irAlMain(); return; }

        setContentView(R.layout.activity_login);
        etOperarioId = findViewById(R.id.et_operario_id);
        etPassword   = findViewById(R.id.et_password);
        btnIngresar  = findViewById(R.id.btn_ingresar);
        progressBar  = findViewById(R.id.progress_bar);

        btnIngresar.setOnClickListener(v -> intentarLogin());
    }

    private void intentarLogin() {
        String id       = etOperarioId.getText().toString().trim().toUpperCase();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(id)) {
            etOperarioId.setError("Ingresa tu ID de operario"); return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña"); return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnIngresar.setEnabled(false);

        // ── 1. Verificar si es ADMIN ─────────────────────────────────────
        if ("ADMIN".equalsIgnoreCase(id) && password.equals(PASSWORD_ADMIN)) {
            session.iniciarSesion("ADMIN", "Administrador", "Todos", "ECOLIM S.A.C.");
            limpiarPrefsPerfilAnterior();
            irAlMain();
            return;
        }

        // ── 2. Verificar operarios (hardcoded + creados por admin) ───────
        OperariosManager.Operario op = operariosManager.validarLogin(id, password);
        if (op != null) {
            session.iniciarSesion(op.id, op.nombre, op.turno, op.planta);
            limpiarPrefsPerfilAnterior();
            irAlMain();
            return;
        }

        // ── 3. Credenciales inválidas ────────────────────────────────────
        progressBar.setVisibility(View.GONE);
        btnIngresar.setEnabled(true);
        Toast.makeText(this, "ID o contraseña incorrectos", Toast.LENGTH_SHORT).show();
    }

    // Limpia el nombre personalizado guardado en perfil para que no
    // quede el nombre del usuario anterior al cambiar de cuenta
    private void limpiarPrefsPerfilAnterior() {
        getSharedPreferences("profile_prefs", MODE_PRIVATE)
            .edit().remove("nombre_usuario").apply();
    }

    private void irAlMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
