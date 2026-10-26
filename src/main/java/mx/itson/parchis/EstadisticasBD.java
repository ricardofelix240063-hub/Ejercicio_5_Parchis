package mx.itson.parchis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EstadisticasBD {

    private static final String URL = "jdbc:mysql://localhost:3306/parchis?createDatabaseIfNotExist=true&connectTimeout=2000";
    private static final String USUARIO = "root";
    private static final String CLAVE = "";
    private boolean disponible;

    public EstadisticasBD() {
        try (Connection conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("CREATE TABLE IF NOT EXISTS estadisticas ("
                    + "color VARCHAR(10) PRIMARY KEY, "
                    + "jugadas INT NOT NULL DEFAULT 0, "
                    + "ganadas INT NOT NULL DEFAULT 0)");
            disponible = true;
        } catch (SQLException e) {
            disponible = false;
            System.out.println("Sin base de datos: " + e.getMessage());
        }
    }

    public boolean estaDisponible() {
        return disponible;
    }

    public String cargar() {
        if (!disponible) {
            return "<html>Estadísticas: sin base de datos</html>";
        }
        String texto = "<html><b>Estadísticas</b>";
        try (Connection conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(
                        "SELECT color, jugadas, ganadas FROM estadisticas ORDER BY color DESC")) {
            while (resultado.next()) {
                texto += "<br>" + resultado.getString("color") + ": " + resultado.getInt("ganadas")
                        + " ganadas de " + resultado.getInt("jugadas");
            }
        } catch (SQLException e) {
            texto += "<br>No se pudieron leer: " + e.getMessage();
        }
        return texto + "</html>";
    }

    public boolean guardar(ColorJugador ganador) {
        if (!disponible) {
            return false;
        }
        String sql = "INSERT INTO estadisticas (color, jugadas, ganadas) VALUES (?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE jugadas = jugadas + 1, ganadas = ganadas + ?";
        try (Connection conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
                PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (ColorJugador color : ColorJugador.values()) {
                int gano = color == ganador ? 1 : 0;
                sentencia.setString(1, color.getNombre());
                sentencia.setInt(2, gano);
                sentencia.setInt(3, gano);
                sentencia.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("No se guardaron las estadísticas: " + e.getMessage());
            return false;
        }
    }
}
