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
    public static boolean useAiTaunts = false;
    public static String aiTauntModel = "gpt-4o-mini";
    public static Integer aiRateLimit = 5;
    public static int aiCooldownMinutes = 5;

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
                    useAiTaunts = config.useAiTaunts;
                    aiTauntModel = config.aiTauntModel != null ? config.aiTauntModel : "gpt-4o-mini";
                    aiRateLimit = config.aiRateLimit;
                    aiCooldownMinutes = config.aiCooldownMinutes;
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

        Config defaultConfig = new Config(enableOneBlockDrops, tauntMessages, enableBellSound, useAiTaunts, aiTauntModel, aiRateLimit, aiCooldownMinutes);

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
        boolean useAiTaunts;
        String aiTauntModel;
        Integer aiRateLimit;
        public int aiCooldownMinutes;

        Config(boolean enableOneBlockDrops, List<String> tauntMessages, boolean enableBellSound, boolean useAiTaunts, String aiTauntModel, Integer aiRateLimit, int aiCooldownMinutes) {
            this.enableOneBlockDrops = enableOneBlockDrops;
            this.tauntMessages = tauntMessages != null ? tauntMessages : new ArrayList<>();
            this.enableBellSound = enableBellSound;
            this.useAiTaunts = useAiTaunts;
            this.aiTauntModel = aiTauntModel;
            this.aiRateLimit = aiRateLimit;
            this.aiCooldownMinutes = aiCooldownMinutes;
        }
    }
}
