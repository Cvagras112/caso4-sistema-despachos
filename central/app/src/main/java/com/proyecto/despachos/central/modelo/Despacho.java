package com.proyecto.despachos.central.modelo;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Entidad Despacho. Contrato de datos (contrato-datos.md):
 * - Payload nuevo despacho: {"id","destino","descripcion","zona","estado","publicadoEn"}
 * - Payload cambio de estado: {"id","estado","estadoAnterior","zona","repartidorId","actualizadoEn"}
 */
public class Despacho {

    public String id;
    public String destino;
    public String descripcion;
    public String zona;
    public String estado;
    public String publicadoEn;
    public String repartidorId;
    public String actualizadoEn;
    public List<String> historial = new ArrayList<>();

    /** Decodifica el payload JSON de un despacho nuevo publicado por la Central. */
    public static Despacho desdeJsonNuevo(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        Despacho d = new Despacho();
        d.id = o.getString("id");
        d.destino = o.optString("destino", "");
        d.descripcion = o.optString("descripcion", "");
        d.zona = o.optString("zona", "");
        d.estado = o.optString("estado", Estado.PENDIENTE.name());
        d.publicadoEn = o.optString("publicadoEn", "");
        d.historial.add(d.estado + " · " + formatoCorto(d.publicadoEn) + " · Central");
        return d;
    }

    /** Decodifica el payload JSON de una actualización de estado publicada por un Repartidor. */
    public static Despacho desdeJsonEstado(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        Despacho d = new Despacho();
        d.id = o.getString("id");
        d.estado = o.getString("estado");
        d.zona = o.optString("zona", "");
        d.repartidorId = o.optString("repartidorId", "");
        d.actualizadoEn = o.optString("actualizadoEn", "");
        return d;
    }

    /** Serializa el despacho completo para publicarlo en despachos/puerto-montt/{zona}/nuevos. */
    public String aJsonNuevo() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("destino", destino);
            o.put("descripcion", descripcion);
            o.put("zona", zona);
            o.put("estado", estado);
            o.put("publicadoEn", publicadoEn);
        } catch (JSONException ignorado) {
            // No puede ocurrir con claves fijas y valores String
        }
        return o.toString();
    }

    /** Serializa la actualización de estado para publicarla en despachos/{id}/estado. */
    public String aJsonEstado(String estadoAnterior) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("estado", estado);
            o.put("estadoAnterior", estadoAnterior);
            o.put("zona", zona);
            o.put("repartidorId", repartidorId);
            o.put("actualizadoEn", actualizadoEn);
        } catch (JSONException ignorado) {
            // No puede ocurrir con claves fijas y valores String
        }
        return o.toString();
    }

    /** Marca temporal ISO 8601 con zona horaria, por ejemplo 2026-09-02T16:30:00-04:00. */
    public static String ahora() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        return formato.format(new Date());
    }

    /** Convierte una marca ISO 8601 a milisegundos para ordenar listados. */
    public static long aEpoch(String iso) {
        if (iso == null || iso.isEmpty()) {
            return 0;
        }
        try {
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            formato.setTimeZone(TimeZone.getDefault());
            Date fecha = formato.parse(iso);
            return fecha == null ? 0 : fecha.getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Versión corta "2026-09-02 16:30" para mostrar en pantalla. */
    public static String formatoCorto(String iso) {
        if (iso == null || iso.length() < 16) {
            return "";
        }
        return iso.substring(0, 16).replace('T', ' ');
    }
}
