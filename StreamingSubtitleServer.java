import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import java.util.concurrent.Executors;

public class StreamingSubtitleServer {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Uso: java StreamingSubtitleServer [subtitles|auth|monitor]");
            return;
        }

        int serverPort = 8080; // Puerto por defecto
        switch (args[0].toLowerCase()) {
            case "subtitles":
                serverPort += 1; // Por ejemplo, 8081 para el servidor de subtítulos
                break;
            case "auth":
                serverPort += 2; // Por ejemplo, 8082 para el servidor de autenticación
                break;
            case "monitor":
                serverPort += 3; // Por ejemplo, 8083 para el servidor de monitoreo
                break;
            default:
                System.out.println("Argumento no reconocido: " + args[0]);
                return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

        switch (args[0].toLowerCase()) {
            case "subtitles":
                server.createContext("/subtitles", new SubtitleHandler());
                break;
            case "auth":
                server.createContext("/login", new LoginHandler());
                break;
            case "monitor":
                server.createContext("/monitor", new MonitorHandler());
                break;
        }

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Servidor " + args[0] + " iniciado en el puerto " + serverPort);
    }
}
