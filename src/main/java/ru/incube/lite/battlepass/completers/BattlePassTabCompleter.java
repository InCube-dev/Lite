package ru.incube.lite.battlepass.completers;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BattlePassTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ArrayList<String> adminCommands = new ArrayList<>();
        ArrayList<String> playerCommands = new ArrayList<>();
        adminCommands.add("help");
        adminCommands.add("test");
        adminCommands.add("reload");
        playerCommands.add("help");

        if (args.length == 1) {
            if (sender.isOp()) {
                return adminCommands;
            } else {
                return playerCommands;
            }
        }

        return playerCommands;
    }
}
