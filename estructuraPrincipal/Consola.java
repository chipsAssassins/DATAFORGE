package estructuraPrincipal;

import java.nio.file.*;
import java.util.*;

public class Consola { // DATAFORGE
    private static final int ANCHO_MAX = 14;     // ancho máximo de celda en pantalla
    private static final int COLS_VISIBLES = 8;  // columnas por pantalla

    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);
        String ruta = args.length > 0 ? args[0] : pedir(in, "Ruta del CSV: ");
        Dataset d = Dataset.cargar(Paths.get(ruta));
        System.out.printf("Cargado: %d filas x %d columnas%n", d.totalFilas(), d.totalColumnas());

        int pag = 0, colIni = 0;
        while (true) {
            dibujar(d, pag, colIni);
            System.out.print("[n]sig [p]ant [g]ir a pág [>][<] columnas [e]ditar [b]orrar fila "
                    + "[f]altantes [d]uplicados [x]quitar dup [s]guardar [q]salir\n> ");
            if (!in.hasNextLine()) return;
            String op = in.nextLine().trim().toLowerCase();
            switch (op) {
                case "n" -> pag = Math.min(pag + 1, d.totalPaginas() - 1);
                case "p" -> pag = Math.max(pag - 1, 0);
                case ">" -> colIni = Math.min(colIni + COLS_VISIBLES, Math.max(0, d.totalColumnas() - 1));
                case "<" -> colIni = Math.max(colIni - COLS_VISIBLES, 0);
                case "g" -> {
                    try { pag = Math.max(0, Math.min(Integer.parseInt(pedir(in, "Página: ")) - 1, d.totalPaginas() - 1)); }
                    catch (NumberFormatException e) { System.out.println("Número inválido."); }
                }
                case "e" -> editar(in, d);
                case "b" -> {
                    int f = leerFila(in, d);
                    if (f >= 0) { d.eliminarFila(f); System.out.println("Fila eliminada."); pag = Math.min(pag, d.totalPaginas() - 1); }
                }
                case "f" -> reporteFaltantes(d);
                case "d" -> reporteDuplicados(d);
                case "x" -> { int n = d.eliminarDuplicados(); System.out.println("Duplicados eliminados: " + n); pag = Math.min(pag, d.totalPaginas() - 1); }
                case "s" -> {
                    String dest = pedir(in, "Guardar como (Enter = " + d.origen() + "): ");
                    d.guardar(dest.isBlank() ? d.origen() : Paths.get(dest));
                    System.out.println("Guardado.");
                }
                case "q" -> { return; }
                default -> System.out.println("Opción no válida.");
            }
        }
    }

    // ---------- Cuadrícula ----------
    private static void dibujar(Dataset d, int pag, int colIni) {
        int colFin = Math.min(colIni + COLS_VISIBLES, d.totalColumnas());
        List<String[]> filas = d.pagina(pag);
        int ini = pag * Dataset.TAM_PAGINA;

        int[] ancho = new int[colFin - colIni];
        for (int c = colIni; c < colFin; c++) {
            ancho[c - colIni] = Math.min(ANCHO_MAX, d.encabezado().get(c).length());
            for (String[] f : filas) ancho[c - colIni] = Math.max(ancho[c - colIni], Math.min(ANCHO_MAX, texto(f, c).length()));
        }
        StringBuilder sep = new StringBuilder("+-----");
        for (int w : ancho) sep.append("+-").append("-".repeat(w)).append("-");
        sep.append("+");

        System.out.printf("%nPágina %d/%d | filas %d-%d de %d | columnas %d-%d de %d%n",
                pag + 1, d.totalPaginas(), d.totalFilas() == 0 ? 0 : ini + 1, ini + filas.size(), d.totalFilas(),
                colIni + 1, colFin, d.totalColumnas());
        System.out.println(sep);
        System.out.printf("| %-3s ", "#");
        for (int c = colIni; c < colFin; c++) System.out.printf("| %-" + ancho[c - colIni] + "s ", cortar(d.encabezado().get(c)));
        System.out.println("|");
        System.out.println(sep);
        for (int i = 0; i < filas.size(); i++) {
            System.out.printf("| %-3d ", ini + i + 1);
            for (int c = colIni; c < colFin; c++) {
                String v = d.celdaFaltante(ini + i, c) ? "<FALTA>" : cortar(texto(filas.get(i), c));
                System.out.printf("| %-" + ancho[c - colIni] + "s ", v);
            }
            System.out.println("|");
        }
        System.out.println(sep);
    }

    private static String texto(String[] f, int c) { return c < f.length ? f[c].replace('\n', ' ') : ""; }
    private static String cortar(String s) { return s.length() > ANCHO_MAX ? s.substring(0, ANCHO_MAX - 1) + "…" : s; }

    // ---------- Acciones ----------
    private static void editar(Scanner in, Dataset d) {
        int f = leerFila(in, d);
        if (f < 0) return;
        int c = d.indiceColumna(pedir(in, "Columna (número o nombre): "));
        if (c < 0) { System.out.println("Columna no existe."); return; }
        System.out.println("Valor actual: " + d.fila(f)[c]);
        d.editar(f, c, pedir(in, "Nuevo valor: "));
        System.out.println("Celda actualizada (recuerda guardar con 's').");
    }

    private static int leerFila(Scanner in, Dataset d) {
        try {
            int n = Integer.parseInt(pedir(in, "Número de fila (#): "));
            if (n >= 1 && n <= d.totalFilas()) return n - 1;
        } catch (NumberFormatException ignorar) { }
        System.out.println("Fila no válida.");
        return -1;
    }

    private static void reporteFaltantes(Dataset d) {
        int[] fal = d.faltantesPorColumna();
        int total = 0;
        System.out.println("\nFaltantes por columna:");
        for (int c = 0; c < fal.length; c++) if (fal[c] > 0) { System.out.printf("  %-25s %d%n", d.encabezado().get(c), fal[c]); total += fal[c]; }
        System.out.println(total == 0 ? "  Sin datos faltantes." : "  Total: " + total);
    }

    private static void reporteDuplicados(Dataset d) {
        List<List<Integer>> grupos = d.duplicados();
        if (grupos.isEmpty()) { System.out.println("\nNo hay filas duplicadas."); return; }
        int sobran = 0;
        System.out.println("\nGrupos de filas duplicadas:");
        for (List<Integer> g : grupos) {
            StringBuilder sb = new StringBuilder();
            for (int i : g) sb.append(i + 1).append(' ');
            System.out.println("  filas: " + sb.toString().trim());
            sobran += g.size() - 1;
        }
        System.out.println("  Filas sobrantes: " + sobran);
    }

    private static String pedir(Scanner in, String msg) { System.out.print(msg); return in.hasNextLine() ? in.nextLine() : ""; }
}
