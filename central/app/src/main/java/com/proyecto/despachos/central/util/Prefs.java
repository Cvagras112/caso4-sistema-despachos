package com.proyecto.despachos.central.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuración del broker persistida en SharedPreferences (RNF-06).
 */
public final class Prefs {

    private static final String ARCHIVO = "config_despachos";
    private static final String CLAVE_HOST = "host";
    private static final String CLAVE_PUERTO = "puerto";

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

    public static void guardarBroker(Context contexto, String host, int puerto) {
        preferencias(contexto).edit()
                .putString(CLAVE_HOST, host)
                .putInt(CLAVE_PUERTO, puerto)
                .apply();
    }
}
