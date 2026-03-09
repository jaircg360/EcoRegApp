package com.ecolim.ecoregapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ecolim.ecoregapp.data.local.entity.Residuo;
import com.ecolim.ecoregapp.data.repository.ResiduoRepository;
import java.util.List;

public class ResiduoViewModel extends AndroidViewModel {

    private final ResiduoRepository repo;
    public  final LiveData<List<Residuo>> todosLosResiduos;

    private final MutableLiveData<long[]>   statsOperario    = new MutableLiveData<>();
    private final MutableLiveData<Boolean>  guardadoExitoso  = new MutableLiveData<>();
    private final MutableLiveData<Integer>  importadosCount  = new MutableLiveData<>();

    public ResiduoViewModel(@NonNull Application app) {
        super(app);
        repo = new ResiduoRepository(app);
        todosLosResiduos = repo.getTodos();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    public void insertar(Residuo r)   { repo.insertar(r); }
    public void eliminar(Residuo r)   { repo.eliminar(r); }
    public void actualizar(Residuo r) { repo.actualizar(r); }

    // ── Guardar con feedback (usado en RegistroFragment) ──────────────────────
    public void guardarResiduo(Residuo r) {
        repo.insertar(r);
        guardadoExitoso.postValue(true);
    }
    public LiveData<Boolean> getGuardadoExitoso() { return guardadoExitoso; }
    public void resetGuardado() { guardadoExitoso.setValue(null); }

    // ── Importar lista (usado en ImportarFragment) ────────────────────────────
    public void importarLista(List<Residuo> lista) {
        repo.insertarLista(lista, count -> importadosCount.postValue(count));
    }
    public LiveData<Integer> getImportadosCount() { return importadosCount; }

    // ── Stats operario (usado en ProfileFragment) ─────────────────────────────
    public void cargarStatsOperario(String operarioId) {
        repo.getStatsOperario(operarioId, stats -> statsOperario.postValue(stats));
    }
    public LiveData<long[]> getStatsOperario() { return statsOperario; }

    // ── Filtros ───────────────────────────────────────────────────────────────
    public LiveData<List<Residuo>> buscar(String q)            { return repo.buscar(q); }
    public LiveData<List<Residuo>> filtrarPorTipo(String tipo) { return repo.filtrarPorTipo(tipo); }
    public LiveData<List<Residuo>> filtrarPorFecha(String d, String h) {
        return repo.filtrarPorFecha(d, h);
    }
}
