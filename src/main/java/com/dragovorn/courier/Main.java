package com.dragovorn.courier;

import com.dragovorn.courier.util.FileUtil;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JWindow;
import java.io.IOException;

public class Main {

    static JFrame parent;

    public static void main(String... args) {
        parent = new JFrame("Courier");
        parent.setUndecorated(true);
        parent.setSize(1, 1);
        try {
            parent.setIconImage(ImageIO.read(FileUtil.getResource("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        parent.setVisible(true);

        JWindow window = new JWindow();
        window.setBounds(500, 150, 300, 200);
        window.getContentPane().add(new JTextField("Starting Courier..."));
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        new Courier();

        window.dispose();
    }
}