package com.natan.barbeariabd;

import com.natan.barbeariabd.view.MainFrame;
import com.natan.barbeariabd.view.Theme;
import javax.swing.SwingUtilities;

public class BarbeariaBdApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame().setVisible(true);
        });
    }
}
