package estructuraPrincipal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CsvReader {
        String rutaArchivo = "C:\\Users\\Angel Alberto\\Desktop\\Proyecto_Final_EDD\\Csv_storage\\cumulative.csv";
        String linea;
        String separador = ",";
    public static void CsvReadera() {
        String rutaArchivo = "C:\\Users\\Angel Alberto\\Desktop\\Proyecto_Final_EDD\\Csv_storage\\cumulative.csv";
        String linea;
        String separador = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            while ((linea = br.readLine()) != null) {
                // Separa la línea usando la coma como delimitador
                String[] datos = linea.split(separador);
                
                // Imprime los elementos de la fila
                for (String dato : datos) {
                    System.out.print(dato + " | ");
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //para leer solo x cantidad de lineas
    }

    public static void CsvReaderaline(int cantidadDeLineas) {
    String rutaArchivo = "C:\\Users\\Angel Alberto\\Desktop\\Proyecto_Final_EDD\\Csv_storage\\cumulative.csv";
    String linea;
    String separador = ",";

    try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
        int lineCount = 0;
        while (lineCount < cantidadDeLineas && (linea = br.readLine()) != null) {
            if (linea.startsWith("#")) continue; // saltar comentarios

            String[] datos = linea.split(separador);
            for (String dato : datos) {
                System.out.print(dato + " | ");
            }
            System.out.println();
            lineCount++;
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}
