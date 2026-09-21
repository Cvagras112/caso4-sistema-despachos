package com.proyecto.despachos.repartidor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.proyecto.despachos.repartidor.datos.Repositorio;
import com.proyecto.despachos.repartidor.modelo.Despacho;
import com.proyecto.despachos.repartidor.modelo.Estado;
import com.proyecto.despachos.repartidor.mqtt.GestorMqtt;
import com.proyecto.despachos.repartidor.ui.DespachoAdapter;
import com.proyecto.despachos.repartidor.util.Prefs;

/**
 * P-RE03: Detalle del despacho y acciones de estado (CU-RE03, CU-RE04).
 * Muestra todos los campos y habilita según el estado:
 *   PENDIENTE -> "Aceptar" (publica ACEPTADO)
 *   ACEPTADO  -> "Iniciar ruta" (publica EN_CAMINO)
 *   EN_CAMINO -> "Confirmar entrega" (publica ENTREGADO)
 * ERR-06: si el despacho ya fue aceptado por otro repartidor se avisa y se bloquea.
 */
public class DetalleDespachoActivity extends AppCompatActivity implements Repositorio.Escuchador {

    public static final String EXTRA_ID = "id";

    private String id;
    private TextView tvId, tvDestino, tvDescripcion, tvZona, tvEstado, tvPublicadoEn,
            tvRepartidor, tvActualizadoEn, tvHistorial;
    private Button btnAceptar, btnIniciarRuta, btnConfirmarEntrega;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.activity_detalle_despacho_repartidor);

        tvId = findViewById(R.id.tvDetalleId);
        tvDestino = findViewById(R.id.tvDetalleDestino);
        tvDescripcion = findViewById(R.id.tvDetalleDescripcion);
        tvZona = findViewById(R.id.tvDetalleZona);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvPublicadoEn = findViewById(R.id.tvDetallePublicadoEn);
        tvRepartidor = findViewById(R.id.tvDetalleRepartidor);
        tvActualizadoEn = findViewById(R.id.tvDetalleActualizadoEn);
        tvHistorial = findViewById(R.id.tvDetalleHistorial);
        btnAceptar = findViewById(R.id.btnAceptar);
        btnIniciarRuta = findViewById(R.id.btnIniciarRuta);
        btnConfirmarEntrega = findViewById(R.id.btnConfirmarEntrega);

        id = getIntent().getStringExtra(EXTRA_ID);
        if (id == null || Repositorio.obtener().obtenerPorId(id) == null) {
            Toast.makeText(this, "El despacho ya no está disponible.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnAceptar.setOnClickListener(v -> avanzarEstado(Estado.ACEPTADO));
        btnIniciarRuta.setOnClickListener(v -> avanzarEstado(Estado.EN_CAMINO));
        btnConfirmarEntrega.setOnClickListener(v -> avanzarEstado(Estado.ENTREGADO));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Repositorio.obtener().registrarEscuchador(this);
    }

    @Override
    protected void onPause() {
        Repositorio.obtener().quitarEscuchador(this);
        super.onPause();
    }

    /** Publica el cambio de estado (RF-11, RF-12) validando la secuencia obligatoria. */
    private void avanzarEstado(Estado nuevo) {
        Despacho d = Repositorio.obtener().obtenerPorId(id);
        if (d == null) {
            Toast.makeText(this, "El despacho ya no está disponible.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Estado actual;
        try {
            actual = Estado.valueOf(d.estado);
        } catch (Exception e) {
            Toast.makeText(this, "Estado desconocido: " + d.estado, Toast.LENGTH_SHORT).show();
            return;
        }
        // La secuencia es obligatoria: solo se avanza al siguiente estado del diagrama (RF-12).
        if (actual.siguiente() != nuevo) {
            Toast.makeText(this, "No se puede pasar de " + actual.etiqueta()
                    + " a " + nuevo.etiqueta() + ".", Toast.LENGTH_SHORT).show();
            refrescar();
            return;
        }
        // ERR-06: un despacho solo puede ser aceptado por un repartidor (RF-15).
        if (actual == Estado.PENDIENTE && !"PENDIENTE".equals(d.estado)) {
            Toast.makeText(this, "Despacho no disponible (ERR-06).", Toast.LENGTH_SHORT).show();
            return;
        }

        String anterior = d.estado;
        String repartidorId = Prefs.obtenerRepartidorId(this);
        d.estado = nuevo.name();
        d.repartidorId = repartidorId;
        d.actualizadoEn = Despacho.ahora();

        GestorMqtt.obtener().publicar(GestorMqtt.topicoEstado(d.id),
                d.aJsonEstado(nuevo.name(), anterior));

        // Actualización local inmediata (el eco del broker revalidará el estado).
        Repositorio.obtener().actualizarEstado(d.id, nuevo.name(), repartidorId, d.actualizadoEn);

        Toast.makeText(this, "Estado actualizado: " + nuevo.etiqueta(), Toast.LENGTH_SHORT).show();
        refrescar();
    }

    /** El botón de retroceso siempre lleva al menú principal (P-RE01). */
    @Override
    public void onBackPressed() {
        Intent menu = new Intent(this, ConexionActivity.class);
        menu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(menu);
        finish();
    }

    @Override
    public void alCambiarLista() {
        refrescar();
    }

    private void refrescar() {
        Despacho d = Repositorio.obtener().obtenerPorId(id);
        if (d == null) {
            Toast.makeText(this, "El despacho ya no está disponible.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvId.setText(d.id);
        tvDestino.setText(d.destino);
        tvDescripcion.setText(d.descripcion);
        tvZona.setText(d.zona);
        tvEstado.setText(etiqueta(d.estado));
        tvEstado.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(DespachoAdapter.colorEstado(this, d.estado)));
        tvPublicadoEn.setText(Despacho.formatoCorto(d.publicadoEn));
        tvRepartidor.setText(d.repartidorId == null || d.repartidorId.isEmpty() ? "—" : d.repartidorId);
        tvActualizadoEn.setText(d.actualizadoEn == null || d.actualizadoEn.isEmpty()
                ? "—" : Despacho.formatoCorto(d.actualizadoEn));
        tvHistorial.setText(android.text.TextUtils.join("\n", d.historial));

        // Solo la acción correspondiente al estado actual está disponible (CU-RE04).
        btnAceptar.setVisibility(Estado.PENDIENTE.name().equals(d.estado) ? Button.VISIBLE : Button.GONE);
        btnIniciarRuta.setVisibility(Estado.ACEPTADO.name().equals(d.estado) ? Button.VISIBLE : Button.GONE);
        btnConfirmarEntrega.setVisibility(Estado.EN_CAMINO.name().equals(d.estado) ? Button.VISIBLE : Button.GONE);
    }

    private static String etiqueta(String estado) {
        try {
            return Estado.valueOf(estado).etiqueta();
        } catch (Exception e) {
            return estado == null ? "" : estado;
        }
    }
}
