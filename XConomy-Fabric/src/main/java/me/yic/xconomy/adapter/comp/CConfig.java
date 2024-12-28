package me.yic.xconomy.adapter.comp;

import me.yic.xconomy.adapter.iConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public class CConfig implements iConfig {
    private final Map<String, Object> config;
    private final File configFile;

    public CConfig(File file) {
        this.configFile = file;
        Map<String, Object> loadedConfig = loadConfig(file);
        this.config = loadedConfig != null ? loadedConfig : new LinkedHashMap<>();
    }

    public CConfig(URL url) {
        this.configFile = null;
        Map<String, Object> loadedConfig = loadConfigFromURL(url);
        this.config = loadedConfig != null ? loadedConfig : new LinkedHashMap<>();
    }

    public CConfig(String path, String subpath) {
        this.configFile = null;
        URL url = getClass().getResource(path + subpath);
        if (url == null) {
            url = getClass().getResource(path + "/english.yml");
        }
        Map<String, Object> loadedConfig = url != null ? loadConfigFromURL(url) : new LinkedHashMap<>();
        this.config = loadedConfig != null ? loadedConfig : new LinkedHashMap<>();
    }

    @Override
    public Object getConfig() {
        return config;
    }

    @Override
    public boolean contains(String path) {
        return getValueFromPath(path) != null;
    }

    @Override
    public void createSection(String path) {
        setValueAtPath(path, new LinkedHashMap<>());
    }

    @Override
    public void set(String path, Object value) {
        setValueAtPath(path, value);
    }

    @Override
    public void save() throws Exception {
        if (configFile != null) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                yaml.dump(config, writer);
            }
        } else {
            throw new Exception("The file is null");
        }
    }

    @Override
    public String getString(String path) {
        Object value = getValueFromPath(path);
        return value != null ? value.toString() : null;
    }

    @Override
    public Integer getInt(String path) {
        Object value = getValueFromPath(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean getBoolean(String path) {
        Object value = getValueFromPath(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    @Override
    public double getDouble(String path) {
        Object value = getValueFromPath(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(value.toString()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public long getLong(String path) {
        Object value = getValueFromPath(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value != null ? Long.parseLong(value.toString()) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public List<String> getStringList(String path) {
        Object value = getValueFromPath(path);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item != null ? item.toString() : null);
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public LinkedHashMap<BigDecimal, String> getConfigurationSectionSort(String path) {
        Object value = getValueFromPath(path);
        if (value instanceof Map) {
            LinkedHashMap<BigDecimal, String> result = new LinkedHashMap<>();
            Map<?, ?> section = (Map<?, ?>) value;
            
            // Convert all keys to BigDecimal and sort them
            List<BigDecimal> sortedKeys = new ArrayList<>();
            for (Object key : section.keySet()) {
                try {
                    sortedKeys.add(new BigDecimal(key.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
            Collections.sort(sortedKeys);

            // Build the result map with sorted keys
            for (BigDecimal key : sortedKeys) {
                Object sectionValue = section.get(key.toString());
                result.put(key, sectionValue != null ? sectionValue.toString() : null);
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> loadConfig(File file) {
        if (!file.exists()) {
            return new LinkedHashMap<>();
        }
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);
            try (InputStream inputStream = new FileInputStream(file)) {
                Map<String, Object> result = yaml.load(inputStream);
                return result != null ? result : new LinkedHashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> loadConfigFromURL(URL url) {
        if (url == null) {
            return new LinkedHashMap<>();
        }
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);
            try (InputStream inputStream = url.openStream()) {
                Map<String, Object> result = yaml.load(inputStream);
                return result != null ? result : new LinkedHashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    private Object getValueFromPath(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;
        
        for (int i = 0; i < parts.length - 1; i++) {
            Object value = current.get(parts[i]);
            if (!(value instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) value;
        }
        
        return current.get(parts[parts.length - 1]);
    }

    private void setValueAtPath(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;
        
        for (int i = 0; i < parts.length - 1; i++) {
            Object existing = current.get(parts[i]);
            if (!(existing instanceof Map)) {
                existing = new LinkedHashMap<>();
                current.put(parts[i], existing);
            }
            current = (Map<String, Object>) existing;
        }
        
        if (value == null) {
            current.remove(parts[parts.length - 1]);
        } else {
            current.put(parts[parts.length - 1], value);
        }
    }
} 