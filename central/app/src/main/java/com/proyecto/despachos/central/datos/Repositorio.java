package com.proyecto.despachos.central.datos;

import android.os.Handler;
import android.os.Looper;

import com.proyecto.despachos.central.modelo.Despacho;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositorio en memoria de los despachos que la Central creó (RF-05).
 * Singleton: sobrevive mientras la app esté viva, igual que la conexión MQTT.
 * Notifica cambios a las pantallas registradas en el hilo principal.
 */
public class Repositorio {

    /** Avisado cada vez que la lista de despachos cambia (para refrescar RecyclerView o detalle). */
    public interface Escuchador {
        void alCambiarLista();
    }

    private static Repositorio instancia;

    public static synchronized Repositorio obtener() {
        if (instancia == null) {
            instancia = new Repositorio();
        }
        return instancia;
    }

    private final List<Despacho> despachos = new ArrayList<>();
    private final List<Escuchador> escuchadores = new ArrayList<>();
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());

    private Repositorio() {
    }

    /** Agrega un despacho nuevo; si el id ya existe no duplica (seguridad ante reenvíos). */
    public synchronized void agregar(Despacho despacho) {
        if (buscar(despacho.id) != null) {
            return;
        }
        despachos.add(despacho);
        notificar();
    }

    /**
     * Actualiza el estado de un despacho por id. Se ignora si el nuevo estado
     * es igual al actual (idempotencia: el propio mensaje del repartidor
     * reenviado por el broker no duplica el historial).
     */
    public synchronized void actualizarEstado(String id, String estado, String repartidorId, String actualizadoEn) {
        Despacho d = buscar(id);
        if (d == null || estado == null || estado.equals(d.estado)) {
            return;
        }
        d.estado = estado;
        d.repartidorId = repartidorId;
        d.actualizadoEn = actualizadoEn;
        d.historial.add(estado + " · " + Despacho.formatoCorto(actualizadoEn)
                + (repartidorId == null || repartidorId.isEmpty() ? "" : " · " + repartidorId));
        notificar();
    }

    public synchronized Despacho obtenerPorId(String id) {
        Despacho d = buscar(id);
        return d == null ? null : copia(d);
    }

    /** Copia de la lista ordenada por hora de publicación (más recientes primero). */
    public synchronized List<Despacho> obtenerLista() {
        List<Despacho> copia = new ArrayList<>();
        for (Despacho d : despachos) {
            copia.add(copia(d));
        }
        Collections.sort(copia, (a, b) -> Long.compare(
                Despacho.aEpoch(b.publicadoEn), Despacho.aEpoch(a.publicadoEn)));
        return copia;
    }

    public void registrarEscuchador(Escuchador e) {
        synchronized (escuchadores) {
            if (!escuchadores.contains(e)) {
                escuchadores.add(e);
            }
        }
        hiloPrincipal.post(e::alCambiarLista);
    }

    public void quitarEscuchador(Escuchador e) {
        synchronized (escuchadores) {
            escuchadores.remove(e);
        }
    }

    private Despacho buscar(String id) {
        if (id == null) {
            return null;
        }
        for (Despacho d : despachos) {
            if (id.equals(d.id)) {
                return d;
            }
        }
        return null;
    }

    private void notificar() {
        List<Escuchador> copia;
        synchronized (escuchadores) {
            copia = new ArrayList<>(escuchadores);
        }
        hiloPrincipal.post(() -> {
            for (Escuchador e : copia) {
                e.alCambiarLista();
            }
        });
    }

    private Despacho copia(Despacho origen) {
        Despacho c = new Despacho();
        c.id = origen.id;
        c.destino = origen.destino;
        c.descripcion = origen.descripcion;
        c.zona = origen.zona;
        c.estado = origen.estado;
        c.publicadoEn = origen.publicadoEn;
        c.repartidorId = origen.repartidorId;
        c.actualizadoEn = origen.actualizadoEn;
        c.historial = new ArrayList<>(origen.historial);
        return c;
    }
}
