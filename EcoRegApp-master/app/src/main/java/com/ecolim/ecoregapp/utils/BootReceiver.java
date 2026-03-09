package com.ecolim.ecoregapp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Se ejecuta cuando el dispositivo reinicia.
 * Crea los canales de notificación nuevamente.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationHelper.crearCanales(context);
        }
    }
}
