package com.ecolim.ecoregapp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import java.util.List;

@Dao
public interface ResiduoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(Residuo r);

    @Update
    void actualizar(Residuo r);

    @Delete
    void eliminar(Residuo r);

    @Query("SELECT * FROM residuos ORDER BY fecha DESC")
    LiveData<List<Residuo>> getTodos();

    @Query("SELECT * FROM residuos WHERE tipo LIKE :q OR ubicacion LIKE :q OR zona LIKE :q OR operarioNombre LIKE :q ORDER BY fecha DESC")
    LiveData<List<Residuo>> buscar(String q);

    @Query("SELECT * FROM residuos WHERE tipo = :tipo ORDER BY fecha DESC")
    LiveData<List<Residuo>> filtrarPorTipo(String tipo);

    @Query("SELECT * FROM residuos WHERE fecha >= :desde AND fecha <= :hasta ORDER BY fecha DESC")
    LiveData<List<Residuo>> filtrarPorFecha(String desde, String hasta);

    @Query("SELECT COUNT(*) FROM residuos WHERE operarioId = :opId")
    long getCountByOperario(String opId);

    @Query("SELECT COALESCE(SUM(pesoKg), 0) FROM residuos WHERE operarioId = :opId")
    double getTotalPesoByOperario(String opId);

    @Query("SELECT COUNT(DISTINCT substr(fecha,1,10)) FROM residuos WHERE operarioId = :opId")
    long getDiasActivosByOperario(String opId);
}
