import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import java.net.URLEncoder;


public class StreamingClient {
    private static final String AUTH_SERVER = "http://localhost:8082/login";
    private static final String SUBTITLE_SERVER = "http://localhost:8081/subtitles";

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // Autenticación del usuario
        if (!login(reader)) {
            System.out.println("Autenticación fallida.");
            return;
        }

        // Selección de la película
        System.out.println("Por favor, ingrese el nombre de la película:");
        String movie = reader.readLine();

        // Mostrar subtítulos
        displaySubtitles(movie, reader);
    }

    private static boolean login(BufferedReader reader) throws IOException {
    System.out.println("Ingrese su usuario:");
    String username = reader.readLine();
    System.out.println("Ingrese su contraseña:");
    String password = reader.readLine();

    // Preparar los datos para enviar
    String urlParameters  = "usuario=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                            "&contraseña=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

    byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
    int    postDataLength = postData.length;
    String request        = AUTH_SERVER;
    URL    url            = new URL( request );
    HttpURLConnection conn= (HttpURLConnection) url.openConnection();           
    conn.setDoOutput( true );
    conn.setInstanceFollowRedirects( false );
    conn.setRequestMethod( "POST" );
    conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
    conn.setRequestProperty( "charset", "utf-8");
    conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
    conn.setUseCaches( false );
    try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
       wr.write( postData );
    }

    // Leer la respuesta
    int responseCode = conn.getResponseCode();
    System.out.println("POST Response Code :: " + responseCode);

    if (responseCode == HttpURLConnection.HTTP_OK) { //success
        BufferedReader in = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // print result
        System.out.println(response.toString());
        return response.toString().contains("Login successful");
    } else {
        System.out.println("POST request not worked");
        return false;
    }
}


   private static void displaySubtitles(String movie, BufferedReader reader) {
    int subtitleNumber = 1; // Comienza desde el primer subtítulo
    boolean keepDisplaying = true;

    while (keepDisplaying) {
        try {
            String subtitleData = requestSubtitle(movie, subtitleNumber);
            if (subtitleData == null || subtitleData.isEmpty()) {
                keepDisplaying = false; // No hay más subtítulos para mostrar
                break; // Salir del bucle si no hay datos de subtítulos
            }

            // Separa las líneas del subtítulo
            
            String[] lines = subtitleData.split("\n");
            // La primera línea debe ser la numeración, la segunda la línea de tiempo y las siguientes el texto del subtítulo.
            if (lines.length >= 3) {
                String timeLine = lines[1]; // Esto debe contener los tiempos de inicio y fin
                long duration = calculateDuration(timeLine); // Calcula la duración basada en la línea de tiempo

                // Asumiendo que el resto de las líneas son el texto del subtítulo
                StringBuilder subtitleTextBuilder = new StringBuilder();
                for (int i = 2; i < lines.length; i++) {
                    subtitleTextBuilder.append(lines[i]).append("\n");
                }

                // Muestra el texto del subtítulo
                String subtitleText = subtitleTextBuilder.toString().trim();
                saveLastViewedSubtitle(username, movie, subtitleNumber);
                System.out.println(subtitleText);

                // Espera la duración del subtítulo antes de mostrar el siguiente
                if (duration > 0) {
                    Thread.sleep(duration);
                } else {
                    System.out.println("Duración inválida para el subtítulo número: " + subtitleNumber);
                    keepDisplaying = false;
                }
            } else {
                System.out.println("Formato de subtítulo incorrecto para el número: " + subtitleNumber);
                keepDisplaying = false;
            }

            subtitleNumber++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            keepDisplaying = false;
        } catch (IOException e) {
            e.printStackTrace();
            keepDisplaying = false;
        }
    }
}




// Función auxiliar para calcular la duración en milisegundos entre los tiempos de inicio y fin
// Función auxiliar para calcular la duración en milisegundos entre los tiempos de inicio y fin
private static long calculateDuration(String timeData) {
    try {
        // Asumiendo que timeData tiene el formato "HH:mm:ss,SSS --> HH:mm:ss,SSS"
        String[] times = timeData.split(" --> ");
        if (times.length != 2) {
            throw new IllegalArgumentException("Formato de tiempo incorrecto");
        }
        String startTimeStr = times[0];
        String endTimeStr = times[1];

        // Convierte los tiempos a milisegundos
        long startTime = convertToMilliseconds(startTimeStr);
        long endTime = convertToMilliseconds(endTimeStr);

        // Devuelve la duración
        return endTime - startTime;
    } catch (Exception e) {
        System.out.println("Error al calcular la duración: " + e.getMessage());
        return -1; // Devuelve un valor negativo o maneja la excepción adecuadamente
    }
}



// Función auxiliar para convertir un tiempo de formato "HH:mm:ss,SSS" a milisegundos
// Función auxiliar para convertir un tiempo de formato "HH:mm:ss,SSS" a milisegundos
private static long convertToMilliseconds(String timeStr) {
    try {
        // Asegúrate de que la cadena de tiempo tenga el formato correcto
        Pattern p = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");
        Matcher m = p.matcher(timeStr);
        if (!m.matches()) {
            throw new IllegalArgumentException("Formato de tiempo incorrecto: " + timeStr);
        }

        long hours = Long.parseLong(m.group(1));
        long minutes = Long.parseLong(m.group(2));
        long seconds = Long.parseLong(m.group(3));
        long milliseconds = Long.parseLong(m.group(4));

        return (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + milliseconds;
    } catch (Exception e) {
        System.out.println("Error al convertir el tiempo a milisegundos: " + e.getMessage());
        return -1; // Devuelve un valor negativo o maneja la excepción adecuadamente
    }
}


// Función para guardar el último subtítulo visto por un usuario
private void saveLastViewedSubtitle(String username, String movie, int lastSubtitleNumber) {
    Path userStatePath = Paths.get("path/to/user_states", username + ".txt");
    String state = movie + ":" + lastSubtitleNumber;
    
    try {
        Files.write(userStatePath, state.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    } catch (IOException e) {
        e.printStackTrace(); // Manejo de excepción adecuado
    }
}

// Función para cargar el último subtítulo visto por un usuario
private int loadLastViewedSubtitle(String username, String movie) {
    Path userStatePath = Paths.get("path/to/user_states", username + ".txt");
    
    if (Files.exists(userStatePath)) {
        try {
            List<String> stateLines = Files.readAllLines(userStatePath, StandardCharsets.UTF_8);
            for (String stateLine : stateLines) {
                String[] parts = stateLine.split(":");
                if (parts[0].equals(movie)) {
                    return Integer.parseInt(parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Manejo de excepción adecuado
        }
    }
    
    return 1; // Si no hay estado guardado, comienza desde el primer subtítulo
}



    private static String requestSubtitle(String movie, int number) throws IOException {
        URL url = new URL(SUBTITLE_SERVER + "?movie=" + movie + "&number=" + number);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int status = con.getResponseCode();
        if (status == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
            in.close();
            return content.toString();
        } else {
            return "No se pudo obtener el subtítulo.";
        }
    }
}
