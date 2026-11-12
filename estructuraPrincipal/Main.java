package estructuraPrincipal;

import javax.swing.SwingUtilities;

public class Main { // DATAFORGE
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Ventana(args.length > 0 ? args[0] : null).setVisible(true));
    }
}
