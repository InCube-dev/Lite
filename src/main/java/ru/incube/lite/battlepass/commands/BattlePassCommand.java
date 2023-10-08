package ru.incube.lite.battlepass.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.incube.lite.Main;
import ru.incube.lite.battlepass.managers.BattlePassTester;

@SuppressWarnings("deprecation")
public class BattlePassCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("incube.battlepass.use")) {
                if (args.length != 0) {
                    switch (args[0].toLowerCase()) {
                        case "help":
                            if (player.hasPermission("incube.battlepass.help")) {
                                help(player);
                            } else {
                                error(player, "У вас нет прав на использование этой команды");
                                break;
                            }
                            break;
                        case "test":
                            if (player.hasPermission("incube.battlepass.test")) {
                                BattlePassTester.test(player);
                            } else {
                                error(player, "У вас нет прав на использование этой команды");
                            }
                            break;
                        case "reload":
                            if (player.hasPermission("incube.battlepass.reload")) {
                                // Стоит рассчитывать на то, что возможно пользователь изменит конфигурацию
                                // Поэтому перезагружаем конфигурацию и переподключаемся к БД
                                Main.plugin.reloadConfig();
                                Main.plugin.getBattlePassDatabase().reconnect();
                                player.sendMessage(ChatColor.GREEN + "Конфиг и БД перезагружены");
                            } else {
                                error(player, "У вас нет прав на использование этой команды");
                            }
                            break;
                        default:
                            error(player, "Команда не найдена");
                            break;
                    }
                } else {
                    help(player);
                }
            }
        }

        return true;
    }

    private void error(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    private void help(CommandSender sender) {
        if (sender.isOp()) {
            sender.sendMessage(ChatColor.GOLD + "InCubePass v" + Main.plugin.getVersion());
            sender.sendMessage("/pass help - " + ChatColor.GREEN + "Показать помощь по командам");
            sender.sendMessage("/pass test - " + ChatColor.GREEN + "Проверить работоспособность БД");
            sender.sendMessage("/pass reload - " + ChatColor.GREEN + "Перезагрузить плагин");
        }
    }
}
