package ru.incube.lite.configuration;

import ru.incube.lite.Main;

public class ConfigurationManager {
    /**
     * Создает файл конфигурации
     */
    public static void createConfig() {
        Main.plugin.getConfig().options().copyDefaults(true);
        Main.plugin.saveConfig();
    }

    /**
     * Создает папку для конфигурации
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createFolder() {
        if (!Main.plugin.getDataFolder().exists()) {
            Main.plugin.getDataFolder().mkdir();
        }
    }
}
