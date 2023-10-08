package ru.incube.lite;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

@Getter
@SuppressWarnings({"deprecation", "ConstantConditions"})
public final class Main extends JavaPlugin {
    public static final Logger log = Logger.getLogger("Minecraft");
    public static Main plugin;
    private final String version = this.getDescription().getVersion();

    @Override
    public void onEnable() {
        plugin = this;

        // Уведомление о запуске плагина
        log.info(String.format("[%s] InСube Lite v%s запущен", plugin.getName(), version));
    }

    @Override
    public void onDisable() {
        // Уведомление о выключении плагина
        log.info(String.format("[%s] InСube Lite v%s выключен", plugin.getName(), version));
    }
}
