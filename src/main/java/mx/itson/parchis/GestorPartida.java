package mx.itson.parchis;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class GestorPartida extends JPanel implements MouseListener, ActionListener {

    private static final int PUERTO_RIVAL = 5000;
    private static final int CELDAS = 19;
    private static final int TOTAL_CASILLAS = 68;
    private static final int META = 74;

    private final Jugador jugadorRojo = new Jugador("Jugador 1", ColorJugador.ROJO);
    private final Jugador jugadorAzul = new Jugador("Jugador 2", ColorJugador.AZUL);
    private final ReglasMovimiento reglas = new ReglasMovimiento();
    private final Mensajero mensajero = new MensajeroSocket("localhost", PUERTO_RIVAL);
    private final EstadisticasBD estadisticas = new EstadisticasBD();

    private ColorJugador turno = ColorJugador.ROJO;
    private int dado = 0;
    private boolean yaTiro = false;
    private int seisSeguidos = 0;
    private boolean terminada = false;
    private boolean avisoSinRed = false;
    private final ArrayList<String> jugadas = new ArrayList<>();
    private final Random azar = new Random();

    private JLabel etiquetaTurno;
    private JLabel etiquetaDado;
    private JLabel etiquetaEstadisticas;
    private JButton botonDado;
    private JButton botonNueva;
    private JTextArea bitacora;

    private int tamCelda;
    private int origenX;
    private int origenY;

    public GestorPartida() {
        setPreferredSize(new Dimension(620, 620));
        setBackground(new Color(0xEFF4F1));
        addMouseListener(this);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("espectador")) {
            MensajeroSocket.escuchar(PUERTO_RIVAL);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            GestorPartida juego = new GestorPartida();
            JFrame ventana = new JFrame("Parchís - Ricardo Felix Valenzuela");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.add(juego, BorderLayout.CENTER);
            ventana.add(juego.crearPanelLateral(), BorderLayout.EAST);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            juego.etiquetaEstadisticas.setText(juego.estadisticas.cargar());
            juego.agregarBitacora("Empieza " + juego.jugadorEnTurno().getNombre()
                    + " (" + juego.jugadorEnTurno().getColor().getNombre() + ").");
            juego.actualizarEtiquetas();
            ventana.setVisible(true);
        });
    }

    
    public JPanel crearPanelLateral() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(new Color(0xF5F8F6));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(300, 620));

        JPanel arriba = new JPanel(new GridLayout(0, 1, 0, 8));
        JLabel titulo = new JLabel("Parchís - Ricardo");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        etiquetaTurno = new JLabel(" ");
        etiquetaTurno.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        etiquetaDado = new JLabel("Dado: -");
        etiquetaDado.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        botonDado = new JButton("Lanzar dado");
        botonDado.addActionListener(this);
        botonNueva = new JButton("Nueva partida");
        botonNueva.addActionListener(this);
        arriba.add(titulo);
        arriba.add(etiquetaTurno);
        arriba.add(etiquetaDado);
        arriba.add(botonDado);
        arriba.add(botonNueva);
        panel.add(arriba, BorderLayout.NORTH);

        bitacora = new JTextArea();
        bitacora.setEditable(false);
        bitacora.setLineWrap(true);
        bitacora.setWrapStyleWord(true);
        bitacora.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panel.add(new JScrollPane(bitacora), BorderLayout.CENTER);

        etiquetaEstadisticas = new JLabel(" ");
        etiquetaEstadisticas.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JPanel pie = new JPanel(new GridLayout(2, 1, 0, 6));
        pie.setBackground(new Color(0xF5F8F6));
        pie.add(etiquetaEstadisticas);
        JLabel autor = new JLabel("Ricardo Felix Valenzuela");
        autor.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        pie.add(autor);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == botonDado) {
            lanzarDado();
        } else if (e.getSource() == botonNueva) {
            nuevaPartida();
        }
    }

    private void lanzarDado() {
        if (terminada) {
            JOptionPane.showMessageDialog(this, "La partida terminó. Empieza una nueva.");
            return;
        }
        if (yaTiro) {
            JOptionPane.showMessageDialog(this, "Ya tiraste: elige una ficha.");
            return;
        }
        procesarDado(azar.nextInt(6) + 1);
    }

    
    void procesarDado(int valor) {
        Jugador jugador = jugadorEnTurno();
        dado = valor;
        yaTiro = true;
        if (dado == 6) {
            seisSeguidos++;
        } else {
            seisSeguidos = 0;
        }
        agregarBitacora(jugador.getNombre() + " sacó " + dado + ".");
        enviarAlRival("DADO;" + jugador.getColor().getNombre() + ";" + dado);

        if (!reglas.puedeMoverAlguna(jugador, dado)) {
            agregarBitacora("No puede mover ninguna ficha.");
            yaTiro = false;
            if (dado == 6 && seisSeguidos < 3) {
                agregarBitacora("Sacó 6: vuelve a tirar.");
            } else {
                cambiarTurno();
            }
        }
        actualizarEtiquetas();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (terminada || !yaTiro) {
            return;
        }
        Jugador jugador = jugadorEnTurno();
        int[] fichas = jugador.getFichas();
        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColor(), i, fichas[i]);
            int dx = e.getX() - centro[0];
            int dy = e.getY() - centro[1];
            int radio = tamCelda / 2;
            if (dx * dx + dy * dy <= radio * radio) {
                if (fichas[i] == META) {
                    JOptionPane.showMessageDialog(this, "Esa ficha ya llegó a la meta.");
                    return;
                }
                if (fichas[i] == -1 && dado != 5) {
                    JOptionPane.showMessageDialog(this, "Para sacar una ficha de la base necesitas un 5.");
                    return;
                }
                if (fichas[i] >= 0 && fichas[i] + dado > META) {
                    JOptionPane.showMessageDialog(this,
                            "Esa ficha necesita exactamente un " + (META - fichas[i]) + " para llegar a la meta.");
                    return;
                }
                moverFicha(jugador, i);
                return;
            }
        }
    }

    private void moverFicha(Jugador jugador, int numero) {
        Jugador rival = jugador == jugadorRojo ? jugadorAzul : jugadorRojo;
        int anterior = jugador.getFichas()[numero];
        java.util.List<Integer> capturadas = reglas.mover(jugador, rival, numero, dado);
        int posicion = jugador.getFichas()[numero];

        if (anterior == -1) {
            agregarBitacora(jugador.getNombre() + " sacó la ficha " + (numero + 1) + " de su base.");
        } else {
            agregarBitacora(jugador.getNombre() + " avanzó " + dado + (dado == 1 ? " casilla" : " casillas")
                    + " con la ficha " + (numero + 1) + ".");
        }
        jugadas.add(jugador.getColor().getNombre() + "," + numero + "," + posicion);

        for (int i : capturadas) {
            agregarBitacora("¡Capturó la ficha " + (i + 1) + " de " + rival.getNombre() + "! Regresa a su base.");
            enviarAlRival("CAPTURA;" + rival.getColor().getNombre() + ";" + i);
        }
        enviarAlRival("MOVIMIENTO;" + jugador.getColor().getNombre() + ";" + numero + ";" + posicion);

        if (posicion == META) {
            agregarBitacora("La ficha " + (numero + 1) + " de " + jugador.getNombre() + " llegó a la meta.");
        }
        yaTiro = false;
        if (reglas.llegoALaMeta(jugador)) {
            terminarPartida(jugador);
            return;
        }
        if (dado == 6 && seisSeguidos < 3) {
            agregarBitacora("Sacó 6: vuelve a tirar.");
        } else {
            cambiarTurno();
        }
        actualizarEtiquetas();
        repaint();
    }

    private Jugador jugadorEnTurno() {
        return turno == ColorJugador.ROJO ? jugadorRojo : jugadorAzul;
    }

    private void cambiarTurno() {
        turno = turno == ColorJugador.ROJO ? ColorJugador.AZUL : ColorJugador.ROJO;
        seisSeguidos = 0;
        agregarBitacora("Turno de " + jugadorEnTurno().getNombre() + " (" + turno.getNombre() + ").");
    }

    private void terminarPartida(Jugador ganador) {
        terminada = true;
        agregarBitacora("¡" + ganador.getNombre() + " ganó la partida!");
        enviarAlRival("FIN;" + ganador.getColor().getNombre());
        if (!estadisticas.guardar(ganador.getColor())) {
            agregarBitacora("Sin base de datos: no se guardaron las estadísticas.");
        }
        etiquetaEstadisticas.setText(estadisticas.cargar());
        actualizarEtiquetas();
        repaint();
        JOptionPane.showMessageDialog(this,
                "¡Ganó " + ganador.getNombre() + " (" + ganador.getColor().getNombre() + ")!");
    }

    private void nuevaPartida() {
        jugadorRojo.reiniciar();
        jugadorAzul.reiniciar();
        turno = ColorJugador.ROJO;
        dado = 0;
        yaTiro = false;
        seisSeguidos = 0;
        terminada = false;
        jugadas.clear();
        bitacora.setText("");
        agregarBitacora("Nueva partida. Empieza " + jugadorEnTurno().getNombre() + ".");
        actualizarEtiquetas();
        repaint();
    }

    private void actualizarEtiquetas() {
        Jugador jugador = jugadorEnTurno();
        if (terminada) {
            etiquetaTurno.setText("Partida terminada");
            etiquetaTurno.setForeground(Color.DARK_GRAY);
        } else {
            etiquetaTurno.setText("Turno: " + jugador.getNombre() + " (" + turno.getNombre() + ")");
            etiquetaTurno.setForeground(turno.getColorPantalla());
        }
        etiquetaDado.setText("Dado: " + (dado == 0 ? "-" : String.valueOf(dado)) + (yaTiro ? " · elige una ficha" : ""));
        botonDado.setEnabled(!terminada && !yaTiro);
    }

    private void agregarBitacora(String texto) {
        bitacora.append(texto + "\n");
        bitacora.setCaretPosition(bitacora.getDocument().getLength());
    }

    private void enviarAlRival(String mensaje) {
        if (!mensajero.enviar(mensaje) && !avisoSinRed) {
            avisoSinRed = true;
            agregarBitacora("Sin conexión con localhost:" + PUERTO_RIVAL
                    + ". Se sigue jugando en esta computadora.");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tamCelda = (Math.min(getWidth(), getHeight()) - 20) / CELDAS;
        origenX = (getWidth() - tamCelda * CELDAS) / 2;
        origenY = (getHeight() - tamCelda * CELDAS) / 2;

        dibujarBase(g2, 0, 0, ColorJugador.ROJO.getColorPantalla());
        dibujarBase(g2, 0, 11, ColorJugador.AZUL.getColorPantalla());
        dibujarBase(g2, 11, 0, new Color(0xD9DDD3));
        dibujarBase(g2, 11, 11, new Color(0xD9DDD3));

        for (int i = 0; i < TOTAL_CASILLAS; i++) {
            int[] celda = coordenadaCasilla(i);
            Color relleno = Color.WHITE;
            if (i == jugadorRojo.getCasillaSalida()) {
                relleno = aclarar(jugadorRojo.getColor().getColorPantalla());
            } else if (i == jugadorAzul.getCasillaSalida()) {
                relleno = aclarar(jugadorAzul.getColor().getColorPantalla());
            }
            pintarCelda(g2, celda[0], celda[1], relleno);
        }

        for (int i = 0; i < 7; i++) {
            int[] celdaRoja = coordenadaPasillo(jugadorRojo.getColor(), i);
            int[] celdaAzul = coordenadaPasillo(jugadorAzul.getColor(), i);
            pintarCelda(g2, celdaRoja[0], celdaRoja[1], aclarar(jugadorRojo.getColor().getColorPantalla()));
            pintarCelda(g2, celdaAzul[0], celdaAzul[1], aclarar(jugadorAzul.getColor().getColorPantalla()));

            pintarCelda(g2, 9, 17 - i, new Color(0xE7EAE3));
            pintarCelda(g2, 17 - i, 9, new Color(0xE7EAE3));
        }

        g2.setColor(new Color(0x14233A));
        g2.fillRect(origenX + 8 * tamCelda, origenY + 8 * tamCelda, 3 * tamCelda, 3 * tamCelda);
        g2.setColor(new Color(0xA9B6C6));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, tamCelda * 3 / 10)));
        FontMetrics medidas = g2.getFontMetrics();
        g2.drawString("META", origenX + 10 * tamCelda + tamCelda / 2 - medidas.stringWidth("META") / 2,
                origenY + 10 * tamCelda + tamCelda / 2 + medidas.getAscent() / 2 - 1);

        dibujarFichas(g2, jugadorRojo);
        dibujarFichas(g2, jugadorAzul);
    }

    private void dibujarBase(Graphics2D g2, int fila, int columna, Color color) {
        g2.setColor(color);
        g2.fillRect(origenX + columna * tamCelda, origenY + fila * tamCelda, 8 * tamCelda, 8 * tamCelda);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(origenX + (columna + 1) * tamCelda, origenY + (fila + 1) * tamCelda,
                6 * tamCelda, 6 * tamCelda, tamCelda, tamCelda);
    }

    private void pintarCelda(Graphics2D g2, int fila, int columna, Color relleno) {
        int x = origenX + columna * tamCelda;
        int y = origenY + fila * tamCelda;
        g2.setColor(relleno);
        g2.fillRect(x, y, tamCelda, tamCelda);
        g2.setColor(new Color(0xB9C0B5));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(x, y, tamCelda, tamCelda);
    }

    private void dibujarFichas(Graphics2D g2, Jugador jugador) {
        int[] fichas = jugador.getFichas();
        boolean esSuTurno = jugador.getColor() == turno;
        int radio = tamCelda * 2 / 5;
        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColor(), i, fichas[i]);

            boolean movible = esSuTurno && yaTiro && !terminada
                    && reglas.puedeMover(fichas[i], dado);
            if (movible) {
                g2.setColor(new Color(0x2A7A50));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(centro[0] - radio - 4, centro[1] - radio - 4, (radio + 4) * 2, (radio + 4) * 2);
            }

            g2.setColor(jugador.getColor().getColorPantalla());
            g2.fillOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, radio)));
            String numero = String.valueOf(i + 1);
            FontMetrics medidas = g2.getFontMetrics();
            g2.drawString(numero, centro[0] - medidas.stringWidth(numero) / 2,
                    centro[1] + medidas.getAscent() / 2 - 1);
        }
    }

    private static Color aclarar(Color color) {
        return new Color(
                (color.getRed() + 2 * 255) / 3,
                (color.getGreen() + 2 * 255) / 3,
                (color.getBlue() + 2 * 255) / 3);
    }

    
    private int[] coordenadaCasilla(int i) {
        if (i <= 7) return new int[] {8, i};
        if (i <= 15) return new int[] {15 - i, 8};
        if (i == 16) return new int[] {0, 9};
        if (i <= 24) return new int[] {i - 17, 10};
        if (i <= 32) return new int[] {8, i - 14};
        if (i == 33) return new int[] {9, 18};
        if (i <= 41) return new int[] {10, 52 - i};
        if (i <= 49) return new int[] {i - 31, 10};
        if (i == 50) return new int[] {18, 9};
        if (i <= 58) return new int[] {69 - i, 8};
        if (i <= 66) return new int[] {10, 66 - i};
        return new int[] {9, 0};
    }

    
    private int[] coordenadaPasillo(ColorJugador color, int i) {
        return color == ColorJugador.ROJO ? new int[] {9, 1 + i} : new int[] {1 + i, 9};
    }

    
    private int[] centroFicha(ColorJugador color, int numero, int posicion) {
        double fila;
        double columna;
        if (posicion == -1) {

            double[][] lugares = {{2.6, 2.6}, {2.6, 5.4}, {5.4, 2.6}, {5.4, 5.4}};
            int columnaBase = color == ColorJugador.ROJO ? 0 : 11;
            fila = lugares[numero][0];
            columna = columnaBase + lugares[numero][1];
        } else if (posicion == META) {

            int[][] lugares = color == ColorJugador.ROJO
                    ? new int[][] {{9, 8}, {10, 8}, {10, 9}, {9, 9}}
                    : new int[][] {{8, 9}, {8, 10}, {9, 10}, {8, 8}};
            fila = lugares[numero][0] + 0.5;
            columna = lugares[numero][1] + 0.5;
        } else {
            int[] celda = posicion <= 66
                    ? coordenadaCasilla((color.getCasillaSalida() + posicion) % TOTAL_CASILLAS)
                    : coordenadaPasillo(color, posicion - 67);

            double desplazamiento = (numero - 1.5) * 0.12;
            fila = celda[0] + 0.5 + desplazamiento;
            columna = celda[1] + 0.5 + desplazamiento;
        }
        return new int[] {(int) Math.round(origenX + columna * tamCelda), (int) Math.round(origenY + fila * tamCelda)};
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }
}
