package com.proyecto.despachos.repartidor.modelo;

/**
 * Estados del ciclo de vida de un despacho (contrato-datos.md):
 * PENDIENTE (Central) -> ACEPTADO (Repartidor) -> EN_CAMINO (Repartidor) -> ENTREGADO (Repartidor, final).
 */
public enum Estado {

    PENDIENTE,
    ACEPTADO,
    EN_CAMINO,
    ENTREGADO;

    /** Siguiente estado permitido según el diagrama de estados; null si es el final. */
    public Estado siguiente() {
        switch (this) {
            case PENDIENTE:
                return ACEPTADO;
            case ACEPTADO:
                return EN_CAMINO;
            case EN_CAMINO:
                return ENTREGADO;
            default:
                return null;
        }
    }

    /** Texto de etiqueta para la interfaz en español. */
    public String etiqueta() {
        switch (this) {
            case PENDIENTE:
                return "Pendiente";
            case ACEPTADO:
                return "Aceptado";
            case EN_CAMINO:
                return "En camino";
            default:
                return "Entregado";
        }
    }
}
