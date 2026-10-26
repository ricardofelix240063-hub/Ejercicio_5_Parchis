package mx.itson.parchis;

import java.util.ArrayList;
import java.util.List;

public class ReglasMovimiento {

    private static final int TOTAL_CASILLAS = 68;
    private static final int META = 74;

    public boolean puedeMover(int posicion, int dado) {
        return (posicion == -1 && dado == 5)
                || (posicion >= 0 && posicion < META && posicion + dado <= META);
    }

    public boolean puedeMoverAlguna(Jugador jugador, int dado) {
        for (int posicion : jugador.getFichas()) {
            if (puedeMover(posicion, dado)) {
                return true;
            }
        }
        return false;
    }

    public List<Integer> mover(Jugador jugador, Jugador rival, int numero, int dado) {
        int[] fichas = jugador.getFichas();
        if (!puedeMover(fichas[numero], dado)) {
            throw new IllegalArgumentException("No se puede mover esa ficha");
        }

        fichas[numero] = fichas[numero] == -1 ? 0 : fichas[numero] + dado;
        List<Integer> capturas = new ArrayList<>();

        if (fichas[numero] <= 66) {
            int casilla = (jugador.getCasillaSalida() + fichas[numero]) % TOTAL_CASILLAS;
            int[] fichasRival = rival.getFichas();
            for (int i = 0; i < fichasRival.length; i++) {
                if (fichasRival[i] >= 0 && fichasRival[i] <= 66
                        && (rival.getCasillaSalida() + fichasRival[i]) % TOTAL_CASILLAS == casilla) {
                    fichasRival[i] = -1;
                    capturas.add(i);
                }
            }
        }
        return capturas;
    }

    public boolean llegoALaMeta(Jugador jugador) {
        for (int posicion : jugador.getFichas()) {
            if (posicion != META) {
                return false;
            }
        }
        return true;
    }
}
