package com.ecolim.ecoregapp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receptor para notificaciones programadas por AlarmManager.
 */
public class NotificationReceiver extends BroadcastReceiver {
    public static final String EXTRA_TITULO  = "titulo";
    public static final String EXTRA_MENSAJE = "mensaje";

    @Override
    public void onReceive(Context context, Intent intent) {
        String titulo  = intent.getStringExtra(EXTRA_TITULO);
        String mensaje = intent.getStringExtra(EXTRA_MENSAJE);
        if (titulo != null && mensaje != null) {
            NotificationHelper.enviar(context, titulo, mensaje);
        }
    }
}
