package mayton.web;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Year;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/main/resources/mime.properties"));

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);

        Yaml yaml = new Yaml(dumperOptions);
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();

        properties.stringPropertyNames().stream()
                .forEach(name -> {
                    linkedHashMap.put(properties.getProperty(name), new String[] {"1", "2"});
        });

        System.out.println(yaml.dump(linkedHashMap));

    }

}
