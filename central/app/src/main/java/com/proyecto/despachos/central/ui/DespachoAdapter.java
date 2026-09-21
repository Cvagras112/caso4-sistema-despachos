package com.proyecto.despachos.central.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.proyecto.despachos.central.R;
import com.proyecto.despachos.central.modelo.Despacho;
import com.proyecto.despachos.central.modelo.Estado;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador del RecyclerView de despachos (RF-05 / RF-09).
 * Muestra id, destino, zona, estado con color (RF-13) y hora.
 */
public class DespachoAdapter extends RecyclerView.Adapter<DespachoAdapter.Holder> {

    /** Acción al tocar un elemento de la lista. */
    public interface AlSeleccionar {
        void seleccionar(Despacho despacho);
    }

    private final List<Despacho> items = new ArrayList<>();
    private final AlSeleccionar accion;

    public DespachoAdapter(AlSeleccionar accion) {
        this.accion = accion;
    }

    public void setItems(List<Despacho> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup padre, int tipoVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_despacho, padre, false);
        return new Holder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int posicion) {
        Despacho d = items.get(posicion);
        holder.tvId.setText(d.id);
        holder.tvDestino.setText(d.destino);
        holder.tvZona.setText("Zona: " + d.zona);
        holder.tvEstado.setText(etiquetaEstado(d.estado));
        holder.tvEstado.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        colorEstado(holder.itemView.getContext(), d.estado)));
        String hora = d.actualizadoEn != null && !d.actualizadoEn.isEmpty()
                ? d.actualizadoEn : d.publicadoEn;
        holder.tvHora.setText(Despacho.formatoCorto(hora));
        holder.itemView.setOnClickListener(v -> {
            if (accion != null) {
                accion.seleccionar(d);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String etiquetaEstado(String estado) {
        try {
            return Estado.valueOf(estado).etiqueta();
        } catch (Exception e) {
            return estado == null ? "" : estado;
        }
    }

    /** Colores por estado para distinguirlos visualmente (RF-13). */
    public static int colorEstado(android.content.Context contexto, String estado) {
        int recurso;
        switch (estado == null ? "" : estado) {
            case "ACEPTADO":
                recurso = R.color.estado_aceptado;
                break;
            case "EN_CAMINO":
                recurso = R.color.estado_en_camino;
                break;
            case "ENTREGADO":
                recurso = R.color.estado_entregado;
                break;
            default:
                recurso = R.color.estado_pendiente;
                break;
        }
        return ContextCompat.getColor(contexto, recurso);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvId;
        final TextView tvDestino;
        final TextView tvZona;
        final TextView tvEstado;
        final TextView tvHora;

        Holder(@NonNull View vista) {
            super(vista);
            tvId = vista.findViewById(R.id.tvId);
            tvDestino = vista.findViewById(R.id.tvDestino);
            tvZona = vista.findViewById(R.id.tvZona);
            tvEstado = vista.findViewById(R.id.tvEstado);
            tvHora = vista.findViewById(R.id.tvHora);
        }
    }
}
