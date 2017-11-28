package com.dragovorn.courier;

import com.dragovorn.courier.util.FileUtil;

import java.io.IOException;
import java.util.Properties;

public class Version {

    private static String version;

    static {
        Properties properties = new Properties();

        try {
            properties.load(FileUtil.getResource("version.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        version = properties.getProperty("version");
    }

    public static String getVersion() {
        return version;
    }
}