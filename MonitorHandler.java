import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MonitorHandler implements HttpHandler {
    private static final String LOG_FILE = "/home/lago/Tareas/Ukranio/proyecto4/BD/LOG.txt";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Método no permitido
            return;
        }

        String response = "Server is alive\n";
        appendLog("Health check passed");

        sendResponse(exchange, response.getBytes());
    }

    private void appendLog(String text) throws IOException {
        Files.write(
            Paths.get(LOG_FILE),
            (text + "\n").getBytes(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    private void sendResponse(HttpExchange exchange, byte[] responseBytes) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }
}
