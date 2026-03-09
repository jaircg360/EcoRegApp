package com.ecolim.ecoregapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OperariosManager {

    private static final String PREFS_NAME    = "operarios_prefs";
    private static final String KEY_OPERARIOS = "lista_operarios";

    private final SharedPreferences prefs;

    public OperariosManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Modelo ───────────────────────────────────────────────────────────────
    public static class Operario {
        public String  id;
        public String  nombre;
        public String  password;
        public String  turno;
        public String  planta;
        public boolean activo;

        public Operario(String id, String nombre, String password,
                        String turno, String planta) {
            this.id       = id;
            this.nombre   = nombre;
            this.password = password;
            this.turno    = turno;
            this.planta   = planta;
            this.activo   = true;
        }
    }

    // Operarios por defecto hardcoded
    private static final String[][] DEFAULTS = {
            {"OP-01", "Operario OP-01", "ecolim2026", "Mañana", "Planta A"},
            {"OP-42", "Operario OP-42", "ecolim2026", "Tarde",  "Planta A"},
            {"OP-10", "Operario OP-10", "ecolim2026", "Noche",  "Planta B"},
    };

    // ── Todos (default + creados por ADMIN) ──────────────────────────────────
    public List<Operario> getTodos() {
        List<Operario> lista = new ArrayList<>();
        for (String[] d : DEFAULTS)
            lista.add(new Operario(d[0], d[1], d[2], d[3], d[4]));
        lista.addAll(getCreados());
        return lista;
    }

    // ── Solo los creados por ADMIN ────────────────────────────────────────────
    public List<Operario> getCreados() {
        List<Operario> lista = new ArrayList<>();
        String json = prefs.getString(KEY_OPERARIOS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Operario op = new Operario(
                        obj.getString("id"),
                        obj.getString("nombre"),
                        obj.getString("password"),
                        obj.getString("turno"),
                        obj.getString("planta")
                );
                op.activo = obj.optBoolean("activo", true);
                lista.add(op);
            }
        } catch (JSONException ignored) {}
        return lista;
    }

    // ── Validar credenciales al hacer login ───────────────────────────────────
    // Retorna el Operario si es válido, null si no
    public Operario validarLogin(String id, String password) {
        for (Operario op : getTodos()) {
            if (op.id.equalsIgnoreCase(id)
                    && op.password.equals(password)
                    && op.activo) {
                return op;
            }
        }
        return null;
    }

    // ── Crear nuevo operario ──────────────────────────────────────────────────
    // Retorna false si el ID ya existe
    public boolean crear(Operario nuevo) {
        for (Operario op : getTodos()) {
            if (op.id.equalsIgnoreCase(nuevo.id)) return false;
        }
        List<Operario> creados = getCreados();
        creados.add(nuevo);
        guardarLista(creados);
        return true;
    }

    // ── Eliminar operario creado por ID ───────────────────────────────────────
    public void eliminar(String id) {
        List<Operario> creados = getCreados();
        creados.removeIf(op -> op.id.equalsIgnoreCase(id));
        guardarLista(creados);
    }

    // ── Genera el siguiente ID disponible ─────────────────────────────────────
    public String generarSiguienteId() {
        int max = 10;
        for (Operario op : getTodos()) {
            try {
                int num = Integer.parseInt(op.id.replace("OP-", "").trim());
                if (num > max) max = num;
            } catch (NumberFormatException ignored) {}
        }
        return String.format("OP-%02d", max + 1);
    }

    // ── Guardar lista en JSON ─────────────────────────────────────────────────
    private void guardarLista(List<Operario> lista) {
        try {
            JSONArray arr = new JSONArray();
            for (Operario op : lista) {
                JSONObject obj = new JSONObject();
                obj.put("id",       op.id);
                obj.put("nombre",   op.nombre);
                obj.put("password", op.password);
                obj.put("turno",    op.turno);
                obj.put("planta",   op.planta);
                obj.put("activo",   op.activo);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_OPERARIOS, arr.toString()).apply();
        } catch (JSONException ignored) {}
    }
}