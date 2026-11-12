package estructuraPrincipal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;

public class Ventana extends JFrame {
    // Paleta azul pastel
    private static final Color FONDO     = new Color(0xEAF4FC);
    private static final Color PANEL     = new Color(0xD6EAF8);
    private static final Color BOTON     = new Color(0xAED6F1);
    private static final Color BOTON_HOV = new Color(0x85C1E9);
    private static final Color TEXTO     = new Color(0x1B4F72);
    private static final Color SELECCION = new Color(0xBBDDF5);
    private static final Color FALTANTE  = new Color(0xFADBD8); // rosa pastel
    private static final Color DUPLICADO = new Color(0xFCF3CF); // amarillo pastel

    private Dataset datos;
    private int pagina = 0;
    private Set<Integer> duplicadas = new HashSet<>();

    private final JTable tabla = new JTable();
    private final JLabel estado = new JLabel("Abre un archivo CSV para comenzar");
    private final JLabel lblPagina = new JLabel("-");
    private final Modelo modelo = new Modelo();

    public Ventana(String rutaInicial) {
        super("DATAFORGE – Intelligent Data Processing & Cleaning");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignorar) { }
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout());

        add(barraSuperior(), BorderLayout.NORTH);
        add(centro(), BorderLayout.CENTER);
        add(barraInferior(), BorderLayout.SOUTH);

        if (rutaInicial != null) abrir(new File(rutaInicial));
    }

    // ---------- Construcción de la interfaz ----------
    private JPanel barraSuperior() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        p.setBackground(PANEL);
        p.add(boton("Abrir CSV", e -> elegirArchivo()));
        p.add(boton("Guardar", e -> guardar()));
        p.add(Box.createHorizontalStrut(16));
        p.add(boton("Datos faltantes", e -> verFaltantes()));
        p.add(boton("Buscar duplicados", e -> verDuplicados()));
        p.add(boton("Quitar duplicados", e -> quitarDuplicados()));
        p.add(boton("Eliminar fila", e -> eliminarFila()));
        return p;
    }

    private JScrollPane centro() {
        tabla.setModel(modelo);
        tabla.setRowHeight(28);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setGridColor(new Color(0xC5DDF0));
        tabla.setSelectionBackground(SELECCION);
        tabla.setSelectionForeground(TEXTO);
        tabla.setForeground(TEXTO);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setFillsViewportHeight(true);
        tabla.setBackground(Color.WHITE);
        tabla.setDefaultRenderer(Object.class, new Celda());

        JTableHeader h = tabla.getTableHeader();
        h.setBackground(BOTON);
        h.setForeground(TEXTO);
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setReorderingAllowed(false);

        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(new EmptyBorder(10, 10, 10, 10));
        sp.setBackground(FONDO);
        sp.getViewport().setBackground(FONDO);
        return sp;
    }

    private JPanel barraInferior() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(8, 12, 8, 12));

        estado.setForeground(TEXTO);
        lblPagina.setForeground(TEXTO);
        lblPagina.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nav.setOpaque(false);
        nav.add(boton("«", e -> irA(0)));
        nav.add(boton("‹ Anterior", e -> irA(pagina - 1)));
        nav.add(lblPagina);
        nav.add(boton("Siguiente ›", e -> irA(pagina + 1)));
        nav.add(boton("»", e -> irA(datos == null ? 0 : datos.totalPaginas() - 1)));

        p.add(estado, BorderLayout.WEST);
        p.add(nav, BorderLayout.EAST);
        return p;
    }

    private JButton boton(String texto, java.awt.event.ActionListener accion) {
        JButton b = new JButton(texto);
        b.setBackground(BOTON);
        b.setForeground(TEXTO);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(accion);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(BOTON_HOV); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(BOTON); }
        });
        return b;
    }

    // ---------- Acciones ----------
    private void elegirArchivo() {
        JFileChooser fc = new JFileChooser(new File("."));
        fc.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) abrir(fc.getSelectedFile());
    }

    private void abrir(File f) {
        try {
            datos = Dataset.cargar(f.toPath());
            pagina = 0;
            duplicadas.clear();
            modelo.fireTableStructureChanged();
            anchoColumnas();
            refrescar();
            setTitle("DATAFORGE – " + f.getName());
        } catch (Exception ex) {
            error("No se pudo abrir el archivo:\n" + ex.getMessage());
        }
    }

    private void guardar() {
        if (!hayDatos()) return;
        detenerEdicion();
        JFileChooser fc = new JFileChooser(datos.origen().toFile());
        fc.setSelectedFile(datos.origen().toFile());
        fc.setDialogTitle("Guardar como");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            datos.guardar(fc.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(this, "Archivo guardado.", "DATAFORGE", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            error("No se pudo guardar:\n" + ex.getMessage());
        }
    }

    private void verFaltantes() {
        if (!hayDatos()) return;
        int[] fal = datos.faltantesPorColumna();
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (int c = 0; c < fal.length; c++)
            if (fal[c] > 0) { sb.append(String.format("%-25s %d%n", datos.encabezado().get(c), fal[c])); total += fal[c]; }
        if (total == 0) sb.append("Sin datos faltantes.");
        else sb.append("\nTotal: ").append(total).append("\n(Las celdas vacías se ven en rosa en la tabla)");
        mostrarTexto("Datos faltantes", sb.toString());
    }

    private void verDuplicados() {
        if (!hayDatos()) return;
        detenerEdicion();
        List<List<Integer>> grupos = datos.duplicados();
        duplicadas.clear();
        int sobran = 0;
        StringBuilder sb = new StringBuilder();
        for (List<Integer> g : grupos) {
            duplicadas.addAll(g);
            sobran += g.size() - 1;
            sb.append("Filas: ");
            for (int i : g) sb.append(i + 1).append(' ');
            sb.append('\n');
        }
        tabla.repaint();
        if (grupos.isEmpty()) sb.append("No hay filas duplicadas.");
        else sb.append("\nFilas sobrantes: ").append(sobran).append("\n(Se marcan en amarillo en la tabla)");
        mostrarTexto("Duplicados", sb.toString());
    }

    private void quitarDuplicados() {
        if (!hayDatos()) return;
        detenerEdicion();
        int n = datos.duplicados().stream().mapToInt(g -> g.size() - 1).sum();
        if (n == 0) { JOptionPane.showMessageDialog(this, "No hay duplicados que quitar."); return; }
        if (JOptionPane.showConfirmDialog(this, "Se eliminarán " + n + " filas duplicadas (se conserva la primera). ¿Continuar?",
                "Quitar duplicados", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        datos.eliminarDuplicados();
        duplicadas.clear();
        pagina = Math.min(pagina, datos.totalPaginas() - 1);
        refrescar();
    }

    private void eliminarFila() {
        if (!hayDatos()) return;
        detenerEdicion();
        int sel = tabla.getSelectedRow();
        if (sel < 0) { JOptionPane.showMessageDialog(this, "Selecciona una fila primero."); return; }
        int real = pagina * Dataset.TAM_PAGINA + sel;
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar la fila " + (real + 1) + "?", "Eliminar fila",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        datos.eliminarFila(real);
        duplicadas.clear();
        pagina = Math.min(pagina, datos.totalPaginas() - 1);
        refrescar();
    }

    private void irA(int p) {
        if (!hayDatos()) return;
        detenerEdicion();
        pagina = Math.max(0, Math.min(p, datos.totalPaginas() - 1));
        refrescar();
    }

    // ---------- Utilidades ----------
    private void refrescar() {
        modelo.fireTableDataChanged();
        int ini = pagina * Dataset.TAM_PAGINA;
        int n = datos.pagina(pagina).size();
        lblPagina.setText("Página " + (pagina + 1) + " / " + datos.totalPaginas());
        int faltantes = Arrays.stream(datos.faltantesPorColumna()).sum();
        estado.setText(String.format("Filas %d–%d de %d  |  %d columnas  |  %d celdas faltantes",
                n == 0 ? 0 : ini + 1, ini + n, datos.totalFilas(), datos.totalColumnas(), faltantes));
    }

    private void anchoColumnas() {
        TableColumnModel cm = tabla.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) cm.getColumn(i).setPreferredWidth(i == 0 ? 50 : 130);
    }

    private boolean hayDatos() {
        if (datos == null) { JOptionPane.showMessageDialog(this, "Primero abre un archivo CSV."); return false; }
        return true;
    }

    private void detenerEdicion() { if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing(); }
    private void error(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }

    private void mostrarTexto(String titulo, String texto) {
        JTextArea a = new JTextArea(texto, 14, 40);
        a.setEditable(false);
        a.setFont(new Font("Monospaced", Font.PLAIN, 13));
        a.setBackground(FONDO);
        a.setForeground(TEXTO);
        JOptionPane.showMessageDialog(this, new JScrollPane(a), titulo, JOptionPane.PLAIN_MESSAGE);
    }

    // ---------- Modelo: muestra solo la página actual y edita directo sobre el Dataset ----------
    private class Modelo extends AbstractTableModel {
        public int getRowCount()    { return datos == null ? 0 : datos.pagina(pagina).size(); }
        public int getColumnCount() { return datos == null ? 0 : datos.totalColumnas() + 1; }
        public String getColumnName(int c) { return c == 0 ? "#" : datos.encabezado().get(c - 1); }
        public boolean isCellEditable(int r, int c) { return c > 0; }

        public Object getValueAt(int r, int c) {
            int real = pagina * Dataset.TAM_PAGINA + r;
            return c == 0 ? String.valueOf(real + 1) : datos.fila(real)[c - 1];
        }

        public void setValueAt(Object v, int r, int c) {
            datos.editar(pagina * Dataset.TAM_PAGINA + r, c - 1, v == null ? "" : v.toString());
            fireTableCellUpdated(r, c);
            refrescar();
        }
    }

    // ---------- Colores por celda ----------
    private class Celda extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foco, r, c);
            int real = pagina * Dataset.TAM_PAGINA + r;
            setBorder(new EmptyBorder(0, 8, 0, 8));
            setForeground(TEXTO);
            if (c == 0) { setBackground(PANEL); setHorizontalAlignment(CENTER); return this; }
            setHorizontalAlignment(LEFT);
            if (sel) setBackground(SELECCION);
            else if (datos.celdaFaltante(real, c - 1)) setBackground(FALTANTE);
            else if (duplicadas.contains(real)) setBackground(DUPLICADO);
            else setBackground(Color.WHITE);
            return this;
        }
    }
}
