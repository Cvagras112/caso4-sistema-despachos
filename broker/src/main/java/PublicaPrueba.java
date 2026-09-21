import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

/**
 * Publica un despacho de prueba en la zona centro (misma estructura que la App Central).
 * Uso: java -cp "broker\build\install\broker\lib\*" PublicaPrueba
 */
public class PublicaPrueba {

    public static void main(String[] args) throws Exception {
        String zona = (args.length > 0) ? args[0] : "centro";
        String id = (args.length > 1) ? args[1] : "DES-TEST-001";
        MqttClient cliente = new MqttClient("tcp://127.0.0.1:1883",
                "prueba-" + System.currentTimeMillis(), new MemoryPersistence());
        cliente.connect();

        String json = "{\"id\":\"" + id + "\",\"destino\":\"Av. Alemania 1080\","
                + "\"descripcion\":\"Despacho de prueba desde el host\","
                + "\"zona\":\"" + zona + "\",\"estado\":\"PENDIENTE\","
                + "\"publicadoEn\":\"2026-09-19T04:30:00-04:00\"}";

        MqttMessage mensaje = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        mensaje.setQos(1);
        cliente.publish("despachos/puerto-montt/" + zona + "/nuevos", mensaje);
        System.out.println("[PRUEBA] despacho publicado en despachos/puerto-montt/" + zona + "/nuevos");
        cliente.disconnect();
        System.exit(0);
    }
}
