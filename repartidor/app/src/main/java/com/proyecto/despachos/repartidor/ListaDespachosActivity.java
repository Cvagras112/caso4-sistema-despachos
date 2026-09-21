package com.proyecto.despachos.repartidor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.proyecto.despachos.repartidor.datos.Repositorio;
import com.proyecto.despachos.repartidor.modelo.Despacho;
import com.proyecto.despachos.repartidor.mqtt.GestorMqtt;
import com.proyecto.despachos.repartidor.ui.DespachoAdapter;
import com.proyecto.despachos.repartidor.util.Prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * P-RE02: Listado dinámico de despachos de la zona (CU-RE02, CU-RE05).
 * Se actualiza automáticamente al llegar un despacho nuevo por MQTT (RF-09)
 * y permite filtrar por estado (CU-RE05). Tap abre el detalle (P-RE03).
 */
public class ListaDespachosActivity extends AppCompatActivity implements Repositorio.Escuchador, GestorMqtt.Escuchador {

    private static final String[] FILTROS = {"TODOS", "PENDIENTE", "ACEPTADO", "EN_CAMINO", "ENTREGADO"};

    private DespachoAdapter adaptador;
    private String filtro = "TODOS";

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.activity_lista_despachos);

        TextView tvTitulo = findViewById(R.id.tvTitulo);
        tvTitulo.setText("Despachos · zona " + Prefs.obtenerZona(this));

        Spinner spFiltro = findViewById(R.id.spFiltro);
        spFiltro.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, FILTROS));
        spFiltro.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> padre, View vista, int posicion, long idFila) {
                filtro = FILTROS[posicion];
                refrescar();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> padre) {
            }
        });

        RecyclerView rvDespachos = findViewById(R.id.rvDespachos);
        rvDespachos.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new DespachoAdapter(despacho -> {
            Intent detalle = new Intent(this, DetalleDespachoActivity.class);
            detalle.putExtra(DetalleDespachoActivity.EXTRA_ID, despacho.id);
            startActivity(detalle);
        });
        rvDespachos.setAdapter(adaptador);

        Button btnVolverInicio = findViewById(R.id.btnVolverInicio);
        btnVolverInicio.setOnClickListener(v -> volverAlMenuPrincipal());
    }

    /** Regresa al menú principal (P-RE01) limpiando la pila. */
    private void volverAlMenuPrincipal() {
        Intent menu = new Intent(this, ConexionActivity.class);
        menu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(menu);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Repositorio.obtener().registrarEscuchador(this);
        GestorMqtt.obtener().fijarEscuchador(this);
    }

    @Override
    protected void onPause() {
        Repositorio.obtener().quitarEscuchador(this);
        GestorMqtt.obtener().fijarEscuchador(null);
        super.onPause();
    }

    @Override
    public void alCambiarLista() {
        refrescar();
    }

    /** El botón de retroceso siempre lleva al menú principal (P-RE01). */
    @Override
    public void onBackPressed() {
        volverAlMenuPrincipal();
    }

    @Override
    public void alCambiarConexion(boolean conectado, String detalle) {
        // El indicador principal vive en P-RE01; el listado solo refleja datos.
    }

    @Override
    public void alRecibirNuevoDespacho(Despacho despacho) {
        Toast.makeText(this, "Nuevo despacho recibido: " + despacho.id, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void alRecibirCambioEstado(Despacho despacho) {
        Toast.makeText(this, "Despacho " + despacho.id + ": " + despacho.estado, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void alError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void refrescar() {
        List<Despacho> filtrados = new ArrayList<>();
        for (Despacho d : Repositorio.obtener().obtenerLista()) {
            if (filtro.equals("TODOS") || filtro.equals(d.estado)) {
                filtrados.add(d);
            }
        }
        adaptador.setItems(filtrados);
        TextView tvVacio = findViewById(R.id.tvVacio);
        tvVacio.setText("Sin despachos que coincidan con el filtro \"" + filtro + "\".");
        tvVacio.setVisibility(filtrados.isEmpty() ? TextView.VISIBLE : TextView.GONE);
    }
}
