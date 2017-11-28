package com.dragovorn.courier;

import com.dragovorn.courier.util.FileUtil;
import org.apache.commons.lang.SystemUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {

    static JFrame invisible;

    public static void main(String... args) {
        invisible = new JFrame();
        try {
            invisible.setIconImage(ImageIO.read(FileUtil.getResource("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Our discord-rpc library doesn't support macOS yet
        if (!SystemTray.isSupported() || !Desktop.isDesktopSupported() || SystemUtils.IS_OS_MAC_OSX) { // We do this because if there is no sys-tray icon users can't terminate the program, which would be bad
            JOptionPane.showMessageDialog(null, "Your platform isn't supported currently!", "Unsupported platform!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Courier();
    }
}