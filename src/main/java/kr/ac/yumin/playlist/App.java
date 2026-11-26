package kr.ac.yumin.playlist;

import kr.ac.yumin.playlist.ui.*;

import javax.swing.SwingUtilities;

public final class App {
    private App() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> {
                    Ui.theme();
                    new MainFrame().setVisible(true);
                });
    }
}
