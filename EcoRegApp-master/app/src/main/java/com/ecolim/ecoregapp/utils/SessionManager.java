package com.ecolim.ecoregapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS      = "session_prefs";
    private static final String KEY_LOGGED = "is_logged_in";
    private static final String KEY_OP_ID  = "operario_id";
    private static final String KEY_NOMBRE = "operario_nombre";
    private static final String KEY_TURNO  = "turno";
    private static final String KEY_PLANTA = "planta";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void iniciarSesion(String opId, String nombre, String turno, String planta) {
        prefs.edit()
            .putBoolean(KEY_LOGGED, true)
            .putString(KEY_OP_ID, opId)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_TURNO, turno)
            .putString(KEY_PLANTA, planta)
            .apply();
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn()           { return prefs.getBoolean(KEY_LOGGED, false); }
    public String  getOperarioId()        { return prefs.getString(KEY_OP_ID, ""); }
    public String  getNombre()            { return prefs.getString(KEY_NOMBRE, ""); }
    public String  getOperarioNombre()    { return getNombre(); } // alias
    public String  getTurno()             { return prefs.getString(KEY_TURNO, ""); }
    public String  getPlanta()            { return prefs.getString(KEY_PLANTA, ""); }
    public boolean isAdmin()              { return "ADMIN".equalsIgnoreCase(getOperarioId()); }
}
