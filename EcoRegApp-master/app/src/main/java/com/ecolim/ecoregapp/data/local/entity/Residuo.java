package com.ecolim.ecoregapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "residuos")
public class Residuo {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String tipo;          // plastico, organico, papel, metal, vidrio, peligroso
    public double pesoKg;
    public double volumenM3;
    public String fecha;         // "yyyy-MM-dd HH:mm:ss"
    public String ubicacion;
    public String zona;
    public String operarioId;
    public String operarioNombre;
    public String observaciones;
    public boolean requiereEPP;
    public boolean eppConfirmado;
    public boolean sincronizado;
    public String archivoOrigen; // nombre del PDF/CSV si fue importado

    public Residuo() {}

    public Residuo(String tipo, double pesoKg, String fecha,
                   String ubicacion, String zona,
                   String operarioId, String operarioNombre) {
        this.tipo = tipo;
        this.pesoKg = pesoKg;
        this.fecha = fecha;
        this.ubicacion = ubicacion;
        this.zona = zona;
        this.operarioId = operarioId;
        this.operarioNombre = operarioNombre;
        this.sincronizado = false;
        this.requiereEPP = tipo.equalsIgnoreCase("peligroso");
    }

    // Calcula volumen estimado según tipo
    public double calcularVolumen() {
        double densidad;
        switch (tipo.toLowerCase()) {
            case "plastico":  densidad = 0.05; break;
            case "organico":  densidad = 0.30; break;
            case "papel":     densidad = 0.08; break;
            case "metal":     densidad = 0.45; break;
            case "vidrio":    densidad = 0.40; break;
            case "peligroso": densidad = 0.20; break;
            default:          densidad = 0.15; break;
        }
        return pesoKg / (densidad * 1000);
    }
}
