package com.proyecto.despachos.central;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.proyecto.despachos.central.datos.Repositorio;
import com.proyecto.despachos.central.modelo.Despacho;
import com.proyecto.despachos.central.ui.DespachoAdapter;

/**
 * P-CE04: Detalle de un despacho (CU-CE05).
 * Muestra todos los campos, el historial de estados y la hora de la última
 * actualización (RF-06, RF-13). Se re-renderiza al llegar cambios del repartidor.
 */
public class DetalleDespachoActivity extends AppCompatActivity implements Repositorio.Escuchador {

    public static final String EXTRA_ID = "id";

    private String id;
    private TextView tvId, tvDestino, tvDescripcion, tvZona, tvEstado, tvPublicadoEn,
            tvRepartidor, tvActualizadoEn, tvHistorial;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        setContentView(R.layout.activity_detalle_despacho);

        tvId = findViewById(R.id.tvDetalleId);
        tvDestino = findViewById(R.id.tvDetalleDestino);
        tvDescripcion = findViewById(R.id.tvDetalleDescripcion);
        tvZona = findViewById(R.id.tvDetalleZona);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvPublicadoEn = findViewById(R.id.tvDetallePublicadoEn);
        tvRepartidor = findViewById(R.id.tvDetalleRepartidor);
        tvActualizadoEn = findViewById(R.id.tvDetalleActualizadoEn);
        tvHistorial = findViewById(R.id.tvDetalleHistorial);

        id = getIntent().getStringExtra(EXTRA_ID);
        if (id == null || Repositorio.obtener().obtenerPorId(id) == null) {
            Toast.makeText(this, "El despacho ya no está en el listado.", Toast.LENGTH_SHORT).show();
            finish();
        }
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

    /** El botón de retroceso siempre lleva al menú principal (P-CE01). */
    @Override
    public void onBackPressed() {
        Intent menu = new Intent(this, MainActivity.class);
        menu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(menu);
        finish();
    }

    @Override
    public void alCambiarLista() {
        Despacho d = Repositorio.obtener().obtenerPorId(id);
        if (d == null) {
            Toast.makeText(this, "El despacho ya no está en el listado.", Toast.LENGTH_SHORT).show();
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
    }

    private static String etiqueta(String estado) {
        try {
            return com.proyecto.despachos.central.modelo.Estado.valueOf(estado).etiqueta();
        } catch (Exception e) {
            return estado == null ? "" : estado;
        }
    }
}
