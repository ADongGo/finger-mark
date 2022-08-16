package com.adong.fingermark.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * @author ADong
 * @Description 配置工厂
 * @Date 2022-08-15 11:45 AM
 */
public class PropertyFactory {

    private static final Logger log = LoggerFactory.getLogger(PropertyFactory.class);

    private static final Properties prop = new Properties();
    static {
        try {
            prop.load(PropertyFactory.class.getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            log.error("Load Properties Ex", e);
        }
    }
    public static Properties getProperties() {
        return prop;
    }
}
