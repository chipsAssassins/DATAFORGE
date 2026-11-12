package estructuraPrincipal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Dataset {
    public static final int TAM_PAGINA = 10;

    private final List<String> encabezado = new ArrayList<>();
    private final List<String[]> filas = new ArrayList<>();
    private Path origen;

    public static Dataset cargar(Path ruta) throws IOException {
        List<String[]> todo = CsvIO.leer(ruta);
        if (todo.isEmpty()) throw new IOException("El archivo está vacío");
        Dataset d = new Dataset();
        d.origen = ruta;
        d.encabezado.addAll(Arrays.asList(todo.get(0)));
        int n = d.encabezado.size();
        for (int i = 1; i < todo.size(); i++)
            d.filas.add(Arrays.copyOf(todo.get(i), Math.max(n, todo.get(i).length))); // rellena con null si faltan celdas
        for (String[] f : d.filas) for (int c = 0; c < f.length; c++) if (f[c] == null) f[c] = "";
        return d;
    }

    public int totalFilas()    { return filas.size(); }
    public int totalColumnas() { return encabezado.size(); }
    public int totalPaginas()  { return Math.max(1, (filas.size() + TAM_PAGINA - 1) / TAM_PAGINA); }
    public List<String> encabezado() { return encabezado; }
    public String[] fila(int i) { return filas.get(i); }

    /** Filas de la página p (0-based), de 10 en 10. */
    public List<String[]> pagina(int p) {
        int ini = p * TAM_PAGINA;
        return filas.subList(Math.min(ini, filas.size()), Math.min(ini + TAM_PAGINA, filas.size()));
    }

    public static boolean esVacio(String s) {
        if (s == null) return true;
        String t = s.trim();
        return t.isEmpty() || t.equalsIgnoreCase("null") || t.equalsIgnoreCase("n/a")
                || t.equalsIgnoreCase("nan") || t.equals("-");
    }

    public boolean celdaFaltante(int fila, int col) {
        String[] f = filas.get(fila);
        return col >= f.length || esVacio(f[col]);
    }

    /** Cuántos faltantes tiene cada columna. */
    public int[] faltantesPorColumna() {
        int[] r = new int[encabezado.size()];
        for (int i = 0; i < filas.size(); i++)
            for (int c = 0; c < r.length; c++) if (celdaFaltante(i, c)) r[c]++;
        return r;
    }

    /** Grupos de filas idénticas (índices). Usa HashMap: O(n) en vez de comparar todas contra todas. */
    public List<List<Integer>> duplicados() {
        Map<String, List<Integer>> mapa = new LinkedHashMap<>();
        for (int i = 0; i < filas.size(); i++)
            mapa.computeIfAbsent(String.join("\u0001", filas.get(i)), k -> new ArrayList<>()).add(i);
        List<List<Integer>> grupos = new ArrayList<>();
        for (List<Integer> g : mapa.values()) if (g.size() > 1) grupos.add(g);
        return grupos;
    }

    /** Elimina duplicados dejando la primera aparición. Devuelve cuántas borró. */
    public int eliminarDuplicados() {
        Set<String> vistos = new HashSet<>();
        int antes = filas.size();
        filas.removeIf(f -> !vistos.add(String.join("\u0001", f)));
        return antes - filas.size();
    }

    public void editar(int fila, int col, String valor) { filas.get(fila)[col] = valor; }
    public void eliminarFila(int fila) { filas.remove(fila); }

    public int indiceColumna(String token) {
        try {
            int n = Integer.parseInt(token.trim());
            if (n >= 1 && n <= encabezado.size()) return n - 1;
        } catch (NumberFormatException ignorar) { }
        for (int i = 0; i < encabezado.size(); i++)
            if (encabezado.get(i).equalsIgnoreCase(token.trim())) return i;
        return -1;
    }

    public void guardar(Path ruta) throws IOException {
        List<String[]> todo = new ArrayList<>();
        todo.add(encabezado.toArray(new String[0]));
        todo.addAll(filas);
        CsvIO.escribir(ruta, todo);
    }

    public Path origen() { return origen; }
}
