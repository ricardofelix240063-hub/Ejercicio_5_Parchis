package mx.itson.parchis;

import java.util.Arrays;

public class Jugador {

    private final String nombre;
    private final ColorJugador color;
    private final int[] fichas = {-1, -1, -1, -1};

    public Jugador(String nombre, ColorJugador color) {
        this.nombre = nombre;
        this.color = color;
    }

    public String getNombre() {
        return nombre;
    }

    public ColorJugador getColor() {
        return color;
    }

    public int getCasillaSalida() {
        return color.getCasillaSalida();
    }

    public int[] getFichas() {
        return fichas;
    }

    public void reiniciar() {
        Arrays.fill(fichas, -1);
    }
}
