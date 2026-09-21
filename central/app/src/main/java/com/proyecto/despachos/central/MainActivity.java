package com.proyecto.despachos.central;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.proyecto.despachos.central.mqtt.GestorMqtt;
import com.proyecto.despachos.central.util.Prefs;

import java.util.UUID;

/**
 * P-CE01: Pantalla principal / conexión (CU-CE01, CU-CE02).
 * Permite configurar host y puerto del broker, conectarse (ERR-01 con reintento),
 * ver el indicador de conexión (RF-17) y navegar a Nuevo despacho o al listado.
 */
public class MainActivity extends AppCompatActivity implements GestorMqtt.Escuchador {

    private EditText etHost;
    private EditText etPuerto;
    private TextView tvEstadoConexion;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.activity_main);

        etHost = findViewById(R.id.etHost);
        etPuerto = findViewById(R.id.etPuerto);
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);
        Button btnConectar = findViewById(R.id.btnConectar);
        Button btnNuevoDespacho = findViewById(R.id.btnNuevoDespacho);
        Button btnVerDespachos = findViewById(R.id.btnVerDespachos);

        etHost.setText(Prefs.obtenerHost(this));
        etPuerto.setText(String.valueOf(Prefs.obtenerPuerto(this)));

        btnConectar.setOnClickListener(v -> conectar());
        btnNuevoDespacho.setOnClickListener(v -> {
            if (!GestorMqtt.obtener().estaConectado()) {
                Toast.makeText(this, "Conéctese al broker antes de crear despachos (ERR-01).", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, NuevoDespachoActivity.class));
        });
        btnVerDespachos.setOnClickListener(v ->
                startActivity(new Intent(this, ListaDespachosActivity.class)));
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
        Prefs.guardarBroker(this, host, puerto);
        Toast.makeText(this, "Conectando a " + host + ":" + puerto + "…", Toast.LENGTH_SHORT).show();
        // RF-06: la Central se suscribe al patrón despachos/+/estado para recibir
        // las actualizaciones que publican los repartidores.
        GestorMqtt gestor = GestorMqtt.obtener();
        gestor.limpiarSuscripciones();
        gestor.suscribir(GestorMqtt.TOPICO_ESTADOS, 1);
        gestor.conectar(host, puerto, "central-" + UUID.randomUUID());
    }

    @Override
    public void alCambiarConexion(boolean conectado, String detalle) {
        tvEstadoConexion.setText(detalle);
        int color = ContextCompat.getColor(this,
                conectado ? R.color.estado_entregado : R.color.estado_pendiente);
        tvEstadoConexion.setTextColor(color);
    }

    @Override
    public void alRecibirNuevoDespacho(com.proyecto.despachos.central.modelo.Despacho despacho) {
        // La Central no se suscribe a despachos nuevos; solo los publica.
    }

    @Override
    public void alRecibirCambioEstado(com.proyecto.despachos.central.modelo.Despacho despacho) {
        Toast.makeText(this, "Despacho " + despacho.id + ": " + despacho.estado, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void alError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
