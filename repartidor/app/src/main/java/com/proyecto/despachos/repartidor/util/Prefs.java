package com.proyecto.despachos.repartidor.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Configuración persistida en SharedPreferences (RNF-06): broker, zona de trabajo
 * e identificador estable del repartidor.
 */
public final class Prefs {

    private static final String ARCHIVO = "config_despachos";
    private static final String CLAVE_HOST = "host";
    private static final String CLAVE_PUERTO = "puerto";
    private static final String CLAVE_ZONA = "zona";
    private static final String CLAVE_ID_REPARTIDOR = "repartidorId";

    private Prefs() {
    }

    private static SharedPreferences preferencias(Context contexto) {
        return contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public static String obtenerHost(Context contexto) {
        return preferencias(contexto).getString(CLAVE_HOST, "10.0.2.2");
    }

    public static int obtenerPuerto(Context contexto) {
        return preferencias(contexto).getInt(CLAVE_PUERTO, 1883);
    }

    /** Zona guardada; "" si aún no se seleccionó ninguna (ERR-03). */
    public static String obtenerZona(Context contexto) {
        return preferencias(contexto).getString(CLAVE_ZONA, "");
    }

    public static void guardarConfig(Context contexto, String host, int puerto, String zona) {
        preferencias(contexto).edit()
                .putString(CLAVE_HOST, host)
                .putInt(CLAVE_PUERTO, puerto)
                .putString(CLAVE_ZONA, zona)
                .apply();
    }

    /** Identificador estable del repartidor, generado la primera vez (ej. REP-7F3A). */
    public static String obtenerRepartidorId(Context contexto) {
        SharedPreferences p = preferencias(contexto);
        String id = p.getString(CLAVE_ID_REPARTIDOR, null);
        if (id == null || id.isEmpty()) {
            id = "REP-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            p.edit().putString(CLAVE_ID_REPARTIDOR, id).apply();
        }
        return id;
    }
}
