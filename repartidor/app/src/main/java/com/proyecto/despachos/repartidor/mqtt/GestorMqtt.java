package com.proyecto.despachos.repartidor.mqtt;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.proyecto.despachos.repartidor.datos.Repositorio;
import com.proyecto.despachos.repartidor.modelo.Despacho;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capa de comunicación MQTT (RNF-04: separada de la interfaz).
 * - Cliente Eclipse Paho operando en un hilo secundario (RNF-01: no bloquea la UI).
 * - Publicación y suscripción con QoS 1 (RNF-02).
 * - Payload JSON UTF-8 (RNF-03).
 * - Reconexión automática con re-suscripción tras caída de red (RNF-07, ERR-04, FA-02, FA-05).
 *
 * Suscripciones del Repartidor:
 *  - despachos/puerto-montt/{zona}/nuevos  (RF-08: solo despachos de su zona)
 *  - despachos/+/estado                    (sincroniza estados aceptados por otros repartidores, ERR-06)
 *
 * Tópico de publicación de estados: despachos/{idDespacho}/estado (RF-11, RF-12).
 */
public class GestorMqtt {

    private static final String ETIQUETA = "GestorMqtt";
    public static final String RAIZ_ZONA = "despachos/puerto-montt";
    public static final String TOPICO_ESTADOS = "despachos/+/estado";

    /** Eventos que la capa MQTT entrega a la pantalla activa (siempre en el hilo principal). */
    public interface Escuchador {
        void alCambiarConexion(boolean conectado, String detalle);

        void alRecibirNuevoDespacho(Despacho despacho);

        void alRecibirCambioEstado(Despacho despacho);

        void alError(String mensaje);
    }

    private static GestorMqtt instancia;

    public static synchronized GestorMqtt obtener() {
        if (instancia == null) {
            instancia = new GestorMqtt();
        }
        return instancia;
    }

    private final ExecutorService hiloRed = Executors.newSingleThreadExecutor();
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private final List<String[]> suscripciones = new ArrayList<>();

    private MqttClient cliente;
    private volatile boolean conectado;
    private Escuchador escuchador;

    private GestorMqtt() {
    }

    public void fijarEscuchador(Escuchador nuevo) {
        escuchador = nuevo;
        if (nuevo != null) {
            nuevo.alCambiarConexion(conectado, conectado ? "Conectado al broker" : "Desconectado");
        }
    }

    public boolean estaConectado() {
        return conectado;
    }

    /** Construye el tópico de despachos nuevos para una zona: despachos/puerto-montt/{zona}/nuevos */
    public static String topicoNuevos(String zona) {
        return RAIZ_ZONA + "/" + zona + "/nuevos";
    }

    /** Construye el tópico de estado para un despacho: despachos/{id}/estado */
    public static String topicoEstado(String id) {
        return "despachos/" + id + "/estado";
    }

    /** Borra las suscripciones registradas (se usa antes de reconfigurar la zona de trabajo). */
    public void limpiarSuscripciones() {
        synchronized (suscripciones) {
            suscripciones.clear();
        }
    }

    /**
     * Registra una suscripción QoS 1. Si ya hay conexión se suscribe de inmediato;
     * si no, se aplicará al completar la conexión (connectComplete).
     */
    public void suscribir(String topico, int qos) {
        synchronized (suscripciones) {
            boolean yaRegistrada = false;
            for (String[] s : suscripciones) {
                if (s[0].equals(topico)) {
                    yaRegistrada = true;
                    break;
                }
            }
            if (!yaRegistrada) {
                suscripciones.add(new String[]{topico, String.valueOf(qos)});
            }
        }
        hiloRed.execute(() -> {
            try {
                if (cliente != null && cliente.isConnected()) {
                    cliente.subscribe(topico, qos);
                }
            } catch (MqttException e) {
                Log.w(ETIQUETA, "Suscripción pendiente para " + topico + ": " + e.getMessage());
            }
        });
    }

    /** Publica un payload JSON UTF-8 con QoS 1 en el tópico indicado. */
    public void publicar(String topico, String payload) {
        hiloRed.execute(() -> {
            try {
                if (cliente == null || !cliente.isConnected()) {
                    avisarError("Broker no disponible (ERR-01): no se pudo publicar.");
                    return;
                }
                MqttMessage mensaje = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                mensaje.setQos(1);
                cliente.publish(topico, mensaje);
            } catch (MqttException e) {
                Log.e(ETIQUETA, "Error al publicar: " + e.getMessage());
                avisarError("No se pudo publicar en el broker (ERR-01).");
            }
        });
    }

    /** Conecta al broker en el hilo secundario con reconexión automática habilitada. */
    public void conectar(String host, int puerto, String idCliente) {
        hiloRed.execute(() -> {
            try {
                cerrarClienteSilencioso();
                MqttConnectOptions opciones = new MqttConnectOptions();
                opciones.setCleanSession(true);
                opciones.setAutomaticReconnect(true);
                opciones.setConnectionTimeout(10);
                opciones.setKeepAliveInterval(60);

                cliente = new MqttClient("tcp://" + host + ":" + puerto, idCliente, new MemoryPersistence());
                cliente.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconexion, String servidor) {
                        conectado = true;
                        resuscribirTodo();
                        avisoPrincipal(() -> {
                            if (escuchador != null) {
                                escuchador.alCambiarConexion(true, reconexion ? "Reconectado al broker" : "Conectado al broker");
                            }
                        });
                    }

                    @Override
                    public void connectionLost(Throwable causa) {
                        conectado = false;
                        avisoPrincipal(() -> {
                            if (escuchador != null) {
                                escuchador.alCambiarConexion(false, "Conexión perdida. Reintentando automáticamente… (ERR-04)");
                            }
                        });
                    }

                    @Override
                    public void messageArrived(String topico, MqttMessage mensaje) {
                        procesarMensaje(topico, mensaje);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // Confirmación implícita del broker (QoS 1); no se requiere acción.
                    }
                });
                cliente.connect(opciones);
            } catch (Exception e) {
                conectado = false;
                Log.e(ETIQUETA, "No se pudo conectar: " + e.getMessage());
                avisoPrincipal(() -> {
                    if (escuchador != null) {
                        escuchador.alCambiarConexion(false, "Desconectado");
                        escuchador.alError("No se pudo conectar al broker (ERR-01). Verifique host y puerto.");
                    }
                });
            }
        });
    }

    /** Desconecta y libera el cliente (se usa al salir de la app). */
    public void desconectar() {
        hiloRed.execute(this::cerrarClienteSilencioso);
        conectado = false;
    }

    private void cerrarClienteSilencioso() {
        if (cliente == null) {
            return;
        }
        try {
            if (cliente.isConnected()) {
                cliente.disconnect();
            }
        } catch (MqttException ignorado) {
            // El cliente ya estaba desconectado
        }
        try {
            cliente.close();
        } catch (MqttException ignorado) {
            // Cliente ya cerrado
        }
        cliente = null;
        conectado = false;
    }

    /** Re-suscribe todos los tópicos registrados tras (re)conectarse, porque clean session los borra. */
    private void resuscribirTodo() {
        List<String[]> copia;
        synchronized (suscripciones) {
            copia = new ArrayList<>(suscripciones);
        }
        for (String[] s : copia) {
            try {
                cliente.subscribe(s[0], Integer.parseInt(s[1]));
            } catch (MqttException e) {
                Log.w(ETIQUETA, "No se pudo re-suscribir a " + s[0] + ": " + e.getMessage());
            }
        }
    }

    /**
     * Encamina un mensaje MQTT según su tópico y actualiza el repositorio local.
     * ERR-05: si el payload no es JSON válido se ignora con registro en el log.
     */
    private void procesarMensaje(String topico, MqttMessage mensaje) {
        String texto = new String(mensaje.getPayload(), StandardCharsets.UTF_8);
        try {
            if (topico.endsWith("/estado")) {
                Despacho despacho = Despacho.desdeJsonEstado(texto);
                Repositorio.obtener().actualizarEstado(
                        despacho.id, despacho.estado, despacho.repartidorId, despacho.actualizadoEn);
                avisoPrincipal(() -> {
                    if (escuchador != null) {
                        escuchador.alRecibirCambioEstado(despacho);
                    }
                });
            } else if (topico.endsWith("/nuevos")) {
                Despacho despacho = Despacho.desdeJsonNuevo(texto);
                Repositorio.obtener().agregar(despacho);
                avisoPrincipal(() -> {
                    if (escuchador != null) {
                        escuchador.alRecibirNuevoDespacho(despacho);
                    }
                });
            }
        } catch (Exception e) {
            Log.w(ETIQUETA, "JSON inválido en " + topico + "; mensaje ignorado (ERR-05): " + texto);
        }
    }

    private void avisoPrincipal(Runnable accion) {
        hiloPrincipal.post(accion);
    }

    private void avisarError(String mensaje) {
        avisoPrincipal(() -> {
            if (escuchador != null) {
                escuchador.alError(mensaje);
            }
        });
    }
}
