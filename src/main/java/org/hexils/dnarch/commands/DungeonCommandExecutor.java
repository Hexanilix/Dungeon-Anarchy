package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;



public class DungeonCommandExecutor implements CommandExecutor {
    public static ChatColor ER = ChatColor.RED;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;
    public static ChatColor IF = ChatColor.AQUA;
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return true;
        }
        Player p = (Player) sender;
        if (!sender.isOp()) {
            switch (args[0].toLowerCase()) {
                case "info" -> {
                    Dungeon d = Dungeon.get(p);
                    if (d != null) {
                        Dungeon.DungeonInfo di = d.getDungeonInfo();
                        p.sendMessage(String.format(
                                "%sName: %s\nDifficulty: %s\nDescription: %s",
                                IF, di.display_name, di.difficulty, di.description
                        ));
                    } else p.sendMessage(W + "You're currently not in a dungeon. No info to show");
                }
            }
            return true;
        }
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        switch (args[0].toLowerCase()) {

        }
        return true;
    }

    public static class Tab implements TabCompleter {

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            final List<String> s = new ArrayList<>();
            if (sender instanceof ConsoleCommandSender console) {
                return s;
            }
            Player p = (Player) sender;
            DungeonMaster dm = DungeonMaster.getOrNew(p);
            if (sender.isOp()) {
                if (args.length == 1) {
                    s.addAll(List.of("show", "edit", "hide"));
                } else {
                    switch (args[0]) {

                    }
                }
            } else {

            }
            return s;
        }
    }
}
