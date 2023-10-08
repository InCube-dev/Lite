package ru.incube.lite.battlepass.managers;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import ru.incube.lite.Main;

@Data
@SuppressWarnings({"deprecation", "ConstantConditions"})
public class BattlePassTester {
    private static final String HOST = Main.plugin.getConfig().getString("BPdatabase.host");
    private static final String PORT = Main.plugin.getConfig().getString("BPdatabase.port");
    private static final String DATABASE = Main.plugin.getConfig().getString("BPdatabase.database");
    private static final String USERNAME = Main.plugin.getConfig().getString("BPdatabase.username");
    private static final String PASSWORD = Main.plugin.getConfig().getString("BPdatabase.password");
    private static final String TABLE = Main.plugin.getConfig().getString("BPdatabase.table");
    private static final boolean useSSL = Main.plugin.getConfig().getBoolean("BPdatabase.useSSL");
    private static final boolean autoReconnect = Main.plugin.getConfig().getBoolean("BPdatabase.autoReconnect");

    public static void test(Player player) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        player.sendMessage(ChatColor.GOLD + "Параметры подключения к БД BattlePass'а:");
        player.sendMessage("Хост + Порт: " + ChatColor.GREEN + HOST + ":" + PORT);
        player.sendMessage("База данных: " + ChatColor.GREEN + DATABASE);
        player.sendMessage("Имя пользователя: " + ChatColor.GREEN + USERNAME);
        player.sendMessage("Пароль: " + ChatColor.GREEN + (PASSWORD.isEmpty() ? ChatColor.RED + "Не задан" : ChatColor.GREEN + "Задан"));
        player.sendMessage("Таблица: " + ChatColor.GREEN + TABLE);
        player.sendMessage("Использовать SSL: " + (useSSL ? ChatColor.GREEN + "Да" : ChatColor.RED + "Нет"));
        player.sendMessage("Автоматическое переподключение: " + (autoReconnect ? ChatColor.GREEN + "Да" : ChatColor.RED + "Нет"));
        player.sendMessage(ChatColor.GOLD + "Проверка взаимодействия с БД BattlePass'а:");
        player.sendMessage("Подключение: " + ChatColor.GREEN + (Main.plugin.getBattlePassDatabase().getConnection() != null ? ChatColor.GREEN + "Успешно" : ChatColor.RED + "Не удалось"));
        player.sendMessage("Проверка существования таблицы " + TABLE + ": " + (Main.plugin.getBattlePassDatabase().tableExists(TABLE).join() ? ChatColor.GREEN + "Успешно" : ChatColor.RED + "Не удалось"));
        player.sendMessage(ChatColor.GOLD + "Добавление игрока " + player.getName() + "...");
        Main.plugin.getBattlePassDatabase().addPlayer(player);
        scheduler.scheduleSyncDelayedTask(Main.plugin, () -> Main.plugin.getBattlePassDatabase().printPlayerData(player), 20L);
    }
}
