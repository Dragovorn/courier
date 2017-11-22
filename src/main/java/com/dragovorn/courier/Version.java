package com.dragovorn.courier;

import com.dragovorn.courier.util.FileUtil;

import java.io.IOException;
import java.util.Properties;

public class Version {

    private String version;

    Version() {
        Properties properties = new Properties();

        try {
            properties.load(FileUtil.getResource("project.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.version = properties.getProperty("version");
    }

    public String getVersion() {
        return this.version;
    }
}