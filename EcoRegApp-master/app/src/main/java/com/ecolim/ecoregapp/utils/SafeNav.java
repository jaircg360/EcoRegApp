package com.ecolim.ecoregapp.utils;

import android.os.SystemClock;
import android.view.View;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;

/**
 * Previene crashes por navegación doble (doble click / navegación rápida).
 * Uso: SafeNav.go(navController, R.id.destinoFragment)
 */
public class SafeNav {

    // Tiempo mínimo entre navegaciones (ms)
    private static final long MIN_INTERVAL_MS = 600;
    private static long ultimoClick = 0;

    /**
     * Navega solo si no hubo otra navegación en los últimos 600ms
     */
    public static void go(NavController nav, int destinoId) {
        long ahora = SystemClock.elapsedRealtime();
        if (ahora - ultimoClick < MIN_INTERVAL_MS) return;
        ultimoClick = ahora;
        try {
            nav.navigate(destinoId);
        } catch (Exception ignored) {
            // Ignora IllegalArgumentException si el destino ya no existe
        }
    }

    /**
     * Versión para usar directamente en setOnClickListener
     * Ejemplo: view.setOnClickListener(SafeNav.to(nav, R.id.destino))
     */
    public static View.OnClickListener to(NavController nav, int destinoId) {
        return v -> go(nav, destinoId);
    }
}
