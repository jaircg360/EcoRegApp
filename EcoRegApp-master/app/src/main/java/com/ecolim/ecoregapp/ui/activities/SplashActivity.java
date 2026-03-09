package com.ecolim.ecoregapp.ui.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.ecolim.ecoregapp.R;
import com.ecolim.ecoregapp.utils.NotificationHelper;
import com.ecolim.ecoregapp.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private SessionManager session;
    private Handler handler;
    private View ivLogo;
    private TextView tvNombre, tvSubtitulo, tvEmpresa, tvEstado;
    private ProgressBar progressSplash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Crear canales de notificación al iniciar la app
        NotificationHelper.crearCanales(this);

        session = new SessionManager(this);
        handler = new Handler(Looper.getMainLooper());
        bindViews();
        iniciarAnimaciones();
    }

    private void bindViews() {
        ivLogo         = findViewById(R.id.iv_splash_logo);
        tvNombre       = findViewById(R.id.tv_splash_nombre);
        tvSubtitulo    = findViewById(R.id.tv_splash_subtitulo);
        tvEmpresa      = findViewById(R.id.tv_splash_empresa);
        tvEstado       = findViewById(R.id.tv_splash_estado);
        progressSplash = findViewById(R.id.progress_splash);
    }

    private void iniciarAnimaciones() {
        // Logo entra con rebote
        ivLogo.setScaleX(0.3f); ivLogo.setScaleY(0.3f);
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
            ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.3f, 1f),
            ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.3f, 1f));
        logoAnim.setDuration(600);
        logoAnim.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnim.start();

        // Textos aparecen escalonados
        handler.postDelayed(() -> fadeIn(tvNombre, 400), 400);
        handler.postDelayed(() -> fadeIn(tvSubtitulo, 350), 650);
        handler.postDelayed(() -> fadeIn(tvEmpresa, 300), 850);

        // Barra de progreso
        handler.postDelayed(() -> {
            fadeIn(tvEstado, 300);
            fadeIn(progressSplash, 300);
            animarProgreso();
        }, 1000);
    }

    private void fadeIn(View v, long dur) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(dur)
            .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void animarProgreso() {
        String[] estados = {"Cargando base de datos...", "Verificando sesión...", "Listo ✓"};
        int[] progresos  = {35, 70, 100};
        for (int i = 0; i < estados.length; i++) {
            final int idx = i;
            handler.postDelayed(() -> {
                if (tvEstado != null)    tvEstado.setText(estados[idx]);
                if (progressSplash != null) progressSplash.setProgress(progresos[idx]);
                if (idx == estados.length - 1) {
                    handler.postDelayed(this::navegar, 500);
                }
            }, i * 500L);
        }
    }

    private void navegar() {
        Intent intent;
        if (session.isLoggedIn()) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}
