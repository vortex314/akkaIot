package be.limero.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Cfg {
    public static Properties toProperties(String s) {
        Config config = ConfigFactory.parseString(s);
        Properties p = new Properties();
        Set<Map.Entry<String, ConfigValue>> set = config.entrySet();
        for (Map.Entry<String, ConfigValue> entry : set) {
            p.setProperty(entry.getKey(), entry.getValue().unwrapped().toString());
        }
        return p;
    }

    public static Properties toProperties(Config config) {
        Properties p = new Properties();
        Set<Map.Entry<String, ConfigValue>> set = config.entrySet();
        for (Map.Entry<String, ConfigValue> entry : set) {
            p.setProperty(entry.getKey(), entry.getValue().unwrapped().toString());
        }
        return p;
    }

    public static Config toConfig(String s) {
        return ConfigFactory.parseString(s);
    }
}
