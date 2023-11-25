import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class LoginHandler implements HttpHandler {
    private static final String USERS_FILE = "/home/lago/Tareas/Ukranio/proyecto4/BD/USUARIOS.txt";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Método no permitido
            return;
        }

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String formData = br.readLine();
        Map<String, String> parsedForm = parseFormData(formData);

        boolean isAuthenticated = authenticate(parsedForm.get("usuario"), parsedForm.get("contraseña"));

        String response = isAuthenticated ? "Login successful" : "Login failed";
        sendResponse(exchange, response.getBytes());
    }

    private Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());
            map.put(key, value);
        }
        return map;
    }

    private boolean authenticate(String username, String password) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(USERS_FILE), StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] credentials = line.split(" ");
            if (credentials[0].equals(username) && credentials[1].equals(password)) {
                return true; // Usuario autenticado con éxito
            }
        }
        return false; // Falla de autenticación
    }

    private void sendResponse(HttpExchange exchange, byte[] responseBytes) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }
}
