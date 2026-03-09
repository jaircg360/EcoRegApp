package com.ecolim.ecoregapp.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.ecolim.ecoregapp.R;

public class NotificationHelper {

    public static final String CANAL_ACCIONES     = "canal_acciones";
    public static final String CANAL_RECORDATORIO = "canal_recordatorio";
    public static final String CANAL_ALERTAS      = "canal_alertas";

    // ── Crear canales (llamar desde SplashActivity.onCreate) ─────────────────
    public static void crearCanales(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        NotificationChannel acciones = new NotificationChannel(
            CANAL_ACCIONES, "Acciones del sistema", NotificationManager.IMPORTANCE_DEFAULT);
        acciones.setDescription("Cambios en perfil, registros y datos");
        nm.createNotificationChannel(acciones);

        NotificationChannel recordatorio = new NotificationChannel(
            CANAL_RECORDATORIO, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT);
        recordatorio.setDescription("Metas diarias y avisos de turno");
        nm.createNotificationChannel(recordatorio);

        NotificationChannel alertas = new NotificationChannel(
            CANAL_ALERTAS, "Alertas importantes", NotificationManager.IMPORTANCE_HIGH);
        alertas.setDescription("Residuos peligrosos y alertas críticas");
        alertas.enableVibration(true);
        nm.createNotificationChannel(alertas);
    }

    // ── Enviar notificación real ──────────────────────────────────────────────
    // Solo envía si el switch de notificaciones está ON
    public static void enviar(Context ctx, String titulo, String mensaje) {
        enviarEnCanal(ctx, CANAL_ACCIONES, titulo, mensaje, false);
    }

    public static void enviarAlerta(Context ctx, String titulo, String mensaje) {
        enviarEnCanal(ctx, CANAL_ALERTAS, titulo, mensaje, true);
    }

    private static void enviarEnCanal(Context ctx, String canal,
                                      String titulo, String mensaje, boolean ignorarSwitch) {
        // Verificar switch — solo se salta para alertas críticas
        if (!ignorarSwitch) {
            SharedPreferences prefs = ctx.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
            if (!prefs.getBoolean("notif", true)) return;
        }

        // Android 13+ requiere permiso POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
        }

        try {
            Class<?> mainClass = Class.forName("com.ecolim.ecoregapp.ui.activities.MainActivity");
            Intent intent = new Intent(ctx, mainClass)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, canal)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(canal.equals(CANAL_ALERTAS)
                    ? NotificationCompat.PRIORITY_HIGH
                    : NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 150})
                .setContentIntent(pi);

            NotificationManagerCompat.from(ctx)
                .notify((int) System.currentTimeMillis(), b.build());
        } catch (Exception ignored) {}
    }

    // ── Métodos legacy usados por RegistroFragment ────────────────────────────
    private final Context ctx;
    public NotificationHelper(Context ctx) { this.ctx = ctx; }

    public void notificarResiduoPeligroso(String zona, double pesoKg) {
        enviarAlerta(ctx,
            "⚠️ Residuo peligroso registrado",
            "Se registró " + pesoKg + " kg de residuo peligroso en " + zona +
            ". Verifica que el área esté correctamente señalizada.");
    }

    public void notificarMetaAlcanzada(double totalKg) {
        enviarEnCanal(ctx, CANAL_RECORDATORIO,
            "🎯 Meta diaria alcanzada",
            "Has registrado " + String.format("%.1f", totalKg) +
            " kg hoy, superando la meta de 63 kg. ¡Buen trabajo!",
            false);
    }
}
