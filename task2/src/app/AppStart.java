package app;

import control.Controller2D;
import view.Window;

import javax.swing.*;

public class AppStart {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Window window = new Window();
            new Controller2D(window.getPanel());
            window.setVisible(true);
        });
    }
}
