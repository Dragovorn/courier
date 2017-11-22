package com.dragovorn.courier;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String... args) {
        if (!SystemTray.isSupported() || !Desktop.isDesktopSupported()) { // We do this because if there is no sys-tray icon users can't terminate the program, which would be bad
            JOptionPane.showMessageDialog(null, "Your platform isn't supported currently!", "Unsupported platform!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Version();
        new Courier();
    }
}