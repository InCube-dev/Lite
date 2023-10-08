package ru.incube.lite;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.incube.lite.battlepass.database.BattlePassDatabase;

import java.util.logging.Logger;

@Getter
@SuppressWarnings({"deprecation", "ConstantConditions"})
public final class Main extends JavaPlugin {
    public static final Logger log = Logger.getLogger("litePlugin");
    public static Main plugin;
    private final String version = this.getDescription().getVersion();
    private BattlePassDatabase battlePassDatabase;

    @Override
    public void onEnable() {
        plugin = this;

        // Уведомление о запуске плагина
        log.info(String.format("[%s] InСube Lite v%s запущен", plugin.getName(), version));

        // Подключение к БД BattlePass'а
        battlePassDatabase = new BattlePassDatabase();
    }

    @Override
    public void onDisable() {
        // Уведомление о выключении плагина
        log.info(String.format("[%s] InСube Lite v%s выключен", plugin.getName(), version));

        // Отключение от БД BattlePass'а
        if (battlePassDatabase != null) {
            battlePassDatabase.close();
        }
    }
}
