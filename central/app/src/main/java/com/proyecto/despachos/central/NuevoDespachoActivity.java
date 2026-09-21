package com.proyecto.despachos.central;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.proyecto.despachos.central.datos.Repositorio;
import com.proyecto.despachos.central.mqtt.GestorMqtt;
import com.proyecto.despachos.central.modelo.Despacho;
import com.proyecto.despachos.central.modelo.Estado;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * P-CE02: Formulario de nuevo despacho (CU-CE03).
 * Valida campos (ERR-02), publica el JSON en despachos/puerto-montt/{zona}/nuevos
 * con QoS 1 y agrega el despacho al listado local (RF-03, RF-04, RF-05, RF-14, RF-16).
 */
public class NuevoDespachoActivity extends AppCompatActivity {

    private static final String[] ZONAS = {"centro", "alerce", "tepual"};

    private EditText etId;
    private EditText etDestino;
    private EditText etDescripcion;
    private Spinner spZona;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.activity_nuevo_despacho);

        etId = findViewById(R.id.etId);
        etDestino = findViewById(R.id.etDestino);
        etDescripcion = findViewById(R.id.etDescripcion);
        spZona = findViewById(R.id.spZona);
        Button btnPublicar = findViewById(R.id.btnPublicar);
        Button btnCancelar = findViewById(R.id.btnCancelar);

        etId.setText(generarId());
        spZona.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ZONAS));

        btnPublicar.setOnClickListener(v -> publicarDespacho());
        btnCancelar.setOnClickListener(v -> volverAlMenuPrincipal());
    }

    /** El botón de retroceso siempre lleva al menú principal (P-CE01). */
    @Override
    public void onBackPressed() {
        volverAlMenuPrincipal();
    }

    private void volverAlMenuPrincipal() {
        Intent menu = new Intent(this, MainActivity.class);
        menu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(menu);
        finish();
    }

    /** Identificador sugerido automáticamente, editable por el operador. */
    private String generarId() {
        String fecha = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        int aleatorio = new Random().nextInt(900) + 100;
        return "DES-" + fecha + "-" + aleatorio;
    }

    private void publicarDespacho() {
        String id = etId.getText().toString().trim();
        String destino = etDestino.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String zona = ZONAS[spZona.getSelectedItemPosition()];

        if (id.isEmpty() || destino.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Hay campos obligatorios vacíos (ERR-02).", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Repositorio.obtener().obtenerPorId(id) != null) {
            Toast.makeText(this, "Ya existe un despacho con ese id.", Toast.LENGTH_SHORT).show();
            return;
        }

        Despacho despacho = new Despacho();
        despacho.id = id;
        despacho.destino = destino;
        despacho.descripcion = descripcion;
        despacho.zona = zona;
        despacho.estado = Estado.PENDIENTE.name();
        despacho.publicadoEn = Despacho.ahora();
        despacho.historial.add(Estado.PENDIENTE.name() + " · "
                + Despacho.formatoCorto(despacho.publicadoEn) + " · Central");

        GestorMqtt.obtener().publicar(GestorMqtt.topicoNuevos(zona), despacho.aJsonNuevo());
        Repositorio.obtener().agregar(despacho);

        Toast.makeText(this, "Despacho publicado en zona " + zona + " (PENDIENTE).", Toast.LENGTH_SHORT).show();
        finish();
    }
}
