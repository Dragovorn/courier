package com.dragovorn.courier;

import com.dragovorn.courier.util.FileUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {

    public static JFrame invisible;

    public static void main(String... args) {
        invisible = new JFrame();
        try {
            invisible.setIconImage(ImageIO.read(FileUtil.getResource("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!SystemTray.isSupported() || !Desktop.isDesktopSupported()) { // We do this because if there is no sys-tray icon users can't terminate the program, which would be bad
            JOptionPane.showMessageDialog(null, "Your platform isn't supported currently!", "Unsupported platform!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Version();
        new Courier();
    }
}