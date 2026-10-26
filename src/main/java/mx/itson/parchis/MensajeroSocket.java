package mx.itson.parchis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MensajeroSocket implements Mensajero {

    private final String host;
    private final int puerto;

    public MensajeroSocket(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
    }

    @Override
    public boolean enviar(String mensaje) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 300);
            PrintWriter salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            salida.println(mensaje);
            return !salida.checkError();
        } catch (IOException e) {
            return false;
        }
    }

    public static void escuchar(int puerto) {
        System.out.println("Esperando jugadas en el puerto " + puerto + "...");
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            while (true) {
                try (Socket cliente = servidor.accept();
                        BufferedReader entrada = new BufferedReader(
                                new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = entrada.readLine()) != null) {
                        System.out.println("Llegó: " + linea);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
        }
    }
}
