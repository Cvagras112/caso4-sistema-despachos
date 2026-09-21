import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;

import java.io.File;
import java.util.Properties;

/**
 * Broker MQTT (Moquette) para la demo del Caso 4.
 * Sustituye a Mosquitto sin requerir instalación: escucha en 0.0.0.0:1883,
 * acepta conexiones anónimas y opera sin persistencia (clean session).
 * Se detiene en forma controlada creando un archivo STOP junto al proyecto.
 */
public class Broker {

    public static void main(String[] args) throws Exception {
        Server servidor = new Server();
        Properties propiedades = new Properties();
        propiedades.setProperty(IConfig.PORT_PROPERTY_NAME, "1883");
        propiedades.setProperty(IConfig.HOST_PROPERTY_NAME, "0.0.0.0");
        propiedades.setProperty(IConfig.ALLOW_ANONYMOUS_PROPERTY_NAME, "true");
        propiedades.setProperty(IConfig.PERSISTENCE_ENABLED_PROPERTY_NAME, "false");

        servidor.startServer(new MemoryConfig(propiedades));
        System.out.println("Broker MQTT (Moquette) escuchando en 0.0.0.0:1883");
        System.out.println("Topicos: despachos/puerto-montt/{centro|alerce|tepual}/nuevos y despachos/{id}/estado");
        System.out.println("Para detenerlo: cree el archivo broker\\STOP");

        File marcador = new File("STOP");
        File carpeta = marcador.getAbsoluteFile().getParentFile();
        if (carpeta != null) {
            marcador = new File(carpeta, "STOP");
        }
        marcador.delete();

        // Funciona mientras NO exista el archivo STOP; al aparecer, se detiene.
        while (!marcador.exists()) {
            Thread.sleep(500);
        }
        System.out.println("Marcador STOP detectado: deteniendo el broker...");
        servidor.stopServer();
        System.out.println("Broker detenido.");
        System.exit(0);
    }
}

