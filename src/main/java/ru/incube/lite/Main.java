package ru.incube.lite;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.incube.lite.battlepass.commands.BattlePassCommand;
import ru.incube.lite.battlepass.completers.BattlePassTabCompleter;
import ru.incube.lite.battlepass.database.BattlePassDatabase;
import ru.incube.lite.configuration.ConfigurationManager;

import java.util.logging.Logger;

@Getter
@SuppressWarnings({"deprecation", "ConstantConditions"})
public final class Main extends JavaPlugin {
    public static final Logger log = Logger.getLogger("InСube");
    public static Main plugin;
    private final String version = this.getDescription().getVersion();
    private BattlePassDatabase battlePassDatabase;

    @Override
    public void onEnable() {
        plugin = this;

        // Уведомление о запуске плагина
        log.info(String.format("Lite v%s запущен", version));

        // Создание папки для конфигурации
        ConfigurationManager.createFolder();

        // Создание файла конфигурации
        ConfigurationManager.createConfig();

        // Подключение к БД BattlePass'а
        battlePassDatabase = new BattlePassDatabase();

        // Регистрация команд
        getCommand("incubepass").setExecutor(new BattlePassCommand());

        // Регистрация таб-комплитеров
        getCommand("incubepass").setTabCompleter(new BattlePassTabCompleter());
    }

    @Override
    public void onDisable() {
        // Уведомление о выключении плагина
        log.info(String.format("Lite v%s выключен", version));

        // Отключение от БД BattlePass'а
        if (battlePassDatabase != null) {
            battlePassDatabase.close();
        }
    }
}
