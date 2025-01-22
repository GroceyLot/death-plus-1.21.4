package com.deathplus;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {

    private static final String CONFIG_FILE_PATH = "./config/deathplus.json";
    private static boolean isLoaded = false;

    // Configuration fields
    public static boolean enableOneBlockDrops = true;
    public static List<String> tauntMessages = new ArrayList<>();
    public static boolean enableBellSound = true;

    private static final Gson gson = new Gson();

    private ConfigLoader() {

    }

    public static void loadData() {
        if (isLoaded) {
            return; // Prevent re-loading if already loaded
        }

        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Type configType = new TypeToken<Config>() {}.getType();
                Config config = gson.fromJson(reader, configType);

                if (config != null) {
                    enableOneBlockDrops = config.enableOneBlockDrops;
                    tauntMessages = config.tauntMessages != null ? config.tauntMessages : new ArrayList<>();
                    enableBellSound = config.enableBellSound;
                }
            } catch (IOException | JsonSyntaxException e) {
                DeathPlus.LOGGER.error("Failed to load configuration: {}", e.getMessage());
            }
        } else {
            saveDefaultConfig();
        }

        isLoaded = true;
    }

    private static void saveDefaultConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        File configDir = configFile.getParentFile(); // Get the parent directory (./config)

        // Ensure the directory exists
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                DeathPlus.LOGGER.error("Failed to create directory: {}", configDir.getAbsolutePath());
                return;
            }
        }

        Config defaultConfig = new Config(enableOneBlockDrops, tauntMessages, enableBellSound);

        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(defaultConfig, writer);
            DeathPlus.LOGGER.info("Default configuration saved to " + CONFIG_FILE_PATH);
        } catch (IOException e) {
            DeathPlus.LOGGER.error("Failed to save default configuration: {}", e.getMessage());
        }
    }


    // Configuration data class
    private static class Config {
        boolean enableOneBlockDrops;
        List<String> tauntMessages;
        boolean enableBellSound;

        Config(boolean enableOneBlockDrops, List<String> tauntMessages, boolean enableBellSound) {
            this.enableOneBlockDrops = enableOneBlockDrops;
            this.tauntMessages = tauntMessages != null ? tauntMessages : new ArrayList<>();
            this.enableBellSound = enableBellSound;
        }
    }
}
