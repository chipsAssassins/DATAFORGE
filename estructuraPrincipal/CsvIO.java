package estructuraPrincipal;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/** Lee y escribe CSV respetando comillas, comas y saltos de línea dentro de celdas. */
public class CsvIO {

    public static List<String[]> leer(Path ruta) throws IOException {
        return parsear(decodificar(Files.readAllBytes(ruta)));
    }

    // Intenta UTF-8  si el archivo no lo es (ej. Superstore), cae a Windows-1252.
    private static String decodificar(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    static List<String[]> parsear(String t) {
        List<String[]> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean entreComillas = false;
        if (!t.isEmpty() && t.charAt(0) == '\uFEFF') t = t.substring(1); // BOM

        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < t.length() && t.charAt(i + 1) == '"') { celda.append('"'); i++; }
                    else entreComillas = false;
                } else celda.append(c);
            } else if (c == '"') entreComillas = true;
            else if (c == ',') { fila.add(celda.toString()); celda.setLength(0); }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < t.length() && t.charAt(i + 1) == '\n') i++;
                fila.add(celda.toString()); celda.setLength(0);
                agregar(filas, fila);
                fila = new ArrayList<>();
            } else celda.append(c);
        }
        if (celda.length() > 0 || !fila.isEmpty()) { fila.add(celda.toString()); agregar(filas, fila); }
        return filas;
    }

    private static void agregar(List<String[]> filas, List<String> fila) {
        boolean vacia = fila.size() == 1 && fila.get(0).isBlank();
        if (vacia || (!fila.isEmpty() && fila.get(0).startsWith("#"))) return; // vacías y comentarios
        filas.add(fila.toArray(new String[0]));
    }

    public static void escribir(Path ruta, List<String[]> filas) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(ruta, StandardCharsets.UTF_8)) {
            for (String[] f : filas) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < f.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(escapar(f[i]));
                }
                w.write(sb.toString());
                w.newLine();
            }
        }
    }

    private static String escapar(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
