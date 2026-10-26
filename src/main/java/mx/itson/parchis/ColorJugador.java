package mx.itson.parchis;

import java.awt.Color;

public enum ColorJugador {
    ROJO("rojo", 1, new Color(0xC23B2E)),
    AZUL("azul", 18, new Color(0x2563A8));

    private final String nombre;
    private final int casillaSalida;
    private final Color colorPantalla;

    ColorJugador(String nombre, int casillaSalida, Color colorPantalla) {
        this.nombre = nombre;
        this.casillaSalida = casillaSalida;
        this.colorPantalla = colorPantalla;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCasillaSalida() {
        return casillaSalida;
    }

    public Color getColorPantalla() {
        return colorPantalla;
    }
}
