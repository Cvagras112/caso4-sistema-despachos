package com.proyecto.despachos.repartidor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.proyecto.despachos.repartidor.mqtt.GestorMqtt;
import com.proyecto.despachos.repartidor.util.Prefs;

import java.util.UUID;

/**
 * P-RE01: Conexión y selección de zona (CU-RE01).
 * - Configura broker (RNF-06) y zona (RF-07); sin zona se bloquea la conexión (ERR-03).
 * - Se suscribe a despachos/puerto-montt/{zona}/nuevos (RF-08) y a despachos/+/estado
 *   para sincronizar estados cambiados por otros repartidores (ERR-06).
 * - Indicador de conexión (RF-17) y reintento ante fallo (ERR-01).
 */
public class ConexionActivity extends AppCompatActivity implements GestorMqtt.Escuchador {

    /** Zonas de trabajo (RF-07). La primera entrada es el placeholder para ERR-03. */
    private static final String[] ZONAS_NOMBRES = {"— Seleccione zona —", "Centro", "Alerce", "Tepual"};
    private static final String[] ZONAS_VALORES = {"", "centro", "alerce", "tepual"};

    private EditText etHost;
    private EditText etPuerto;
    private Spinner spZona;
    private TextView tvEstadoConexion;
    private Button btnConectar;
    private boolean esperandoConexion;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.activity_conexion);

        etHost = findViewById(R.id.etHost);
        etPuerto = findViewById(R.id.etPuerto);
        spZona = findViewById(R.id.spZona);
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);
        btnConectar = findViewById(R.id.btnConectar);

        etHost.setText(Prefs.obtenerHost(this));
        etPuerto.setText(String.valueOf(Prefs.obtenerPuerto(this)));

        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ZONAS_NOMBRES);
        spZona.setAdapter(adaptador);

        String zonaGuardada = Prefs.obtenerZona(this);
        for (int i = 0; i < ZONAS_VALORES.length; i++) {
            if (ZONAS_VALORES[i].equals(zonaGuardada)) {
                spZona.setSelection(i);
                break;
            }
        }

        btnConectar.setOnClickListener(v -> conectar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        GestorMqtt.obtener().fijarEscuchador(this);
    }

    @Override
    protected void onPause() {
        GestorMqtt.obtener().fijarEscuchador(null);
        super.onPause();
    }

    private void conectar() {
        String host = etHost.getText().toString().trim();
        String textoPuerto = etPuerto.getText().toString().trim();
        int posicion = spZona.getSelectedItemPosition();
        String zona = posicion >= 0 ? ZONAS_VALORES[posicion] : "";

        if (zona.isEmpty()) {
            Toast.makeText(this, "Seleccione una zona de trabajo (ERR-03).", Toast.LENGTH_SHORT).show();
            return;
        }
        if (host.isEmpty() || textoPuerto.isEmpty()) {
            Toast.makeText(this, "Complete host y puerto del broker (ERR-02).", Toast.LENGTH_SHORT).show();
            return;
        }
        int puerto;
        try {
            puerto = Integer.parseInt(textoPuerto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "El puerto debe ser un número (ERR-02).", Toast.LENGTH_SHORT).show();
            return;
        }

        Prefs.guardarConfig(this, host, puerto, zona);

        GestorMqtt gestor = GestorMqtt.obtener();
        gestor.limpiarSuscripciones();
        gestor.suscribir(GestorMqtt.topicoNuevos(zona), 1);
        gestor.suscribir(GestorMqtt.TOPICO_ESTADOS, 1);

        esperandoConexion = true;
        Toast.makeText(this, "Conectando a " + host + ":" + puerto + "…", Toast.LENGTH_SHORT).show();
        gestor.conectar(host, puerto, "repartidor-" + UUID.randomUUID());
    }

    @Override
    public void alCambiarConexion(boolean conectado, String detalle) {
        tvEstadoConexion.setText(detalle);
        int color = ContextCompat.getColor(this,
                conectado ? R.color.estado_entregado : R.color.estado_pendiente);
        tvEstadoConexion.setTextColor(color);

        if (conectado && esperandoConexion) {
            esperandoConexion = false;
            startActivity(new Intent(this, ListaDespachosActivity.class));
        }
    }

    @Override
    public void alRecibirNuevoDespacho(com.proyecto.despachos.repartidor.modelo.Despacho despacho) {
        // Se informa en el listado (P-RE02); aquí no hay nada que hacer.
    }

    @Override
    public void alRecibirCambioEstado(com.proyecto.despachos.repartidor.modelo.Despacho despacho) {
        // El repositorio ya sincronizó el estado; evita aceptar despachos ajenos (ERR-06).
    }

    @Override
    public void alError(String mensaje) {
        esperandoConexion = false;
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
