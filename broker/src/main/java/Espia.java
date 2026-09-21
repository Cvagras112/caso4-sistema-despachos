import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

/**
 * Suscriptor de diagnóstico: registra todo el trafico de despachos/# en consola.
 * Uso: java -cp "broker\build\install\broker\lib\*" Espia
 */
public class Espia {

    public static void main(String[] args) throws Exception {
        MqttClient cliente = new MqttClient("tcp://127.0.0.1:1883",
                "espia-" + System.currentTimeMillis(), new MemoryPersistence());
        cliente.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable causa) {
                System.out.println("[ESPIA] conexion perdida: " + causa);
            }

            @Override
            public void messageArrived(String topico, MqttMessage mensaje) {
                String texto = new String(mensaje.getPayload(), StandardCharsets.UTF_8);
                System.out.println("[ESPIA] " + topico + " -> " + texto);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        cliente.connect();
        cliente.subscribe("despachos/#", 1);
        System.out.println("[ESPIA] suscrito a despachos/# ...");
        Thread.currentThread().join();
    }
}
