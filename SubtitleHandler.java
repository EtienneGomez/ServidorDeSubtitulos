import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;



public class SubtitleHandler implements HttpHandler {
    private static final String SUBTITLE_DIR = "/home/lago/Tareas/Ukranio/proyecto4/BD/PELICULAS";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Método no permitido
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String[] params = query.split("&");
        String movie = params[0].split("=")[1];
        int subtitleNumber = Integer.parseInt(params[1].split("=")[1]);

        // Normaliza el título para manejar posibles discrepancias en mayúsculas y minúsculas
        movie = movie.toLowerCase();

        String subtitleContent = getSubtitle(movie, subtitleNumber);

        if (subtitleContent != null) {
            sendResponse(exchange, subtitleContent.getBytes());
        } else {
            sendResponse(exchange, "Subtitle not found".getBytes());
        }
    }

  private String getSubtitle(String movie, int number) {
    Path subtitleFilePath = Paths.get(SUBTITLE_DIR, movie + ".srt");

    if (!Files.exists(subtitleFilePath)) {
        System.out.println("Archivo no encontrado: " + subtitleFilePath.toString());
        return null;
    }

    try (BufferedReader reader = Files.newBufferedReader(subtitleFilePath, StandardCharsets.UTF_8)) {
        String line;
        StringBuilder subtitleBuilder = new StringBuilder();
        boolean readingSubtitle = false;

        while ((line = reader.readLine()) != null) {
            if (readingSubtitle) {
                // Agregar líneas del subtítulo, incluyendo la línea de tiempo.
                subtitleBuilder.append(line).append("\n");
                if (line.trim().isEmpty()) {
                    // Una línea vacía significa el final del subtítulo actual.
                    break;
                }
            } else if (isNumeric(line.trim())) {
                // Si la línea es numérica, verificamos si es el número de subtítulo que buscamos.
                int currentNumber = Integer.parseInt(line.trim());
                if (currentNumber == number) {
                    readingSubtitle = true; // Marcar para empezar a leer el subtítulo.
                    subtitleBuilder.append(currentNumber).append("\n"); // Agrega el número de subtítulo.
                }
            }
        }

        if (subtitleBuilder.length() > 0) {
            // Retorna todo el subtítulo, incluyendo número y tiempo.
            return subtitleBuilder.toString().trim();
        } else {
            System.out.println("Subtítulo número " + number + " no encontrado.");
            return null;
        }
    } catch (IOException e) {
        System.out.println("Error al leer el archivo: " + subtitleFilePath.toString());
        return null;
    }
}


// Función auxiliar para verificar si una cadena es numérica.
private boolean isNumeric(String strNum) {
    try {
        Integer.parseInt(strNum);
        return true;
    } catch (NumberFormatException e) {
        return false;
    }
}






    private void sendResponse(HttpExchange exchange, byte[] responseBytes) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }
}
