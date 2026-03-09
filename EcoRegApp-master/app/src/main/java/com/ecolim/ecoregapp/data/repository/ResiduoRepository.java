package com.ecolim.ecoregapp.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.ecolim.ecoregapp.data.local.AppDatabase;
import com.ecolim.ecoregapp.data.local.dao.ResiduoDao;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResiduoRepository {

    public interface Callback<T> { void onResult(T result); }

    private final ResiduoDao      dao;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public ResiduoRepository(Context ctx) {
        dao = AppDatabase.getInstance(ctx).residuoDao();
    }

    public LiveData<List<Residuo>> getTodos()                      { return dao.getTodos(); }
    public LiveData<List<Residuo>> buscar(String q)                { return dao.buscar("%" + q + "%"); }
    public LiveData<List<Residuo>> filtrarPorTipo(String tipo)     { return dao.filtrarPorTipo(tipo); }
    public LiveData<List<Residuo>> filtrarPorFecha(String d, String h) { return dao.filtrarPorFecha(d, h); }

    public void insertar(Residuo r)   { executor.execute(() -> dao.insertar(r)); }
    public void eliminar(Residuo r)   { executor.execute(() -> dao.eliminar(r)); }
    public void actualizar(Residuo r) { executor.execute(() -> dao.actualizar(r)); }

    public void insertarLista(List<Residuo> lista, Callback<Integer> cb) {
        executor.execute(() -> {
            for (Residuo r : lista) dao.insertar(r);
            cb.onResult(lista.size());
        });
    }

    public void getStatsOperario(String operarioId, Callback<long[]> cb) {
        executor.execute(() -> {
            long   registros  = dao.getCountByOperario(operarioId);
            double pesoTotal  = dao.getTotalPesoByOperario(operarioId);
            long   diasActivo = dao.getDiasActivosByOperario(operarioId);
            cb.onResult(new long[]{registros, (long) pesoTotal, diasActivo});
        });
    }
}
