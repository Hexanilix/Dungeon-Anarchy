package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.Dungeon;
import org.hexils.dnarch.DungeonMaster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class DungeonCommandExecutor implements CommandExecutor {
    public static ChatColor IF = ChatColor.AQUA;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;
    public static ChatColor ER = ChatColor.RED;
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return true;
        }
        DungeonMaster dm = DungeonMaster.get((Player) sender);
        if (!sender.isOp()) {
            //TODO dungeon cmd briefing
            if (args.length == 1) return true;
            else switch (args[0].toLowerCase()) {
                case "info" -> {
                    Dungeon d = Dungeon.get(dm);
                    if (d != null) {
                        Dungeon.DungeonInfo di = d.getDungeonInfo();
                        dm.sendMessage(String.format(
                                "%sName: %s\nDifficulty: %s\nDescription: %s",
                                IF, di.display_name, di.difficulty, di.description
                        ));
                    } else dm.sendMessage(W + "You're currently not in a dungeon. No info to show");
                }
            }
        } else {
            //TODO dungeon cmd briefing
            if (args.length == 1) return true;
            else switch (args[0].toLowerCase()) {
                case "info" -> {
                    Dungeon d = Dungeon.get(dm);
                    if (d != null) {
                        Dungeon.DungeonInfo di = d.getDungeonInfo();
                        dm.sendMessage(String.format(
                                "%sName: %s\nDifficulty: %s\nDescription: %s",
                                IF, di.display_name, di.difficulty, di.description
                        ));
                    } else dm.sendMessage(W + "You're currently not in a dungeon. No info to show");
                }
                default -> {
                    Dungeon d = Dungeon.get(args[0]);
                    if (d != null) {
                        switch (args[1]) {
                            case "set" -> {
                                if (args.length < 3) {

                                } else switch (args[2].toLowerCase()) {
                                    case "open" -> {
                                        if (args[3].equalsIgnoreCase("true")) {
                                            d.setOpen(true);
                                            if (!d.getEditors().isEmpty()) dm.sendMessage(W + "Couldn't open dungeon since there are still other people editing it:\n- " + String.join("\n- ", d.getEditors().stream().map(DungeonMaster::getDisplayName).toList()));
                                            else dm.sendMessage(IF + "Opened dungeon " + d.getName());
                                        } else if (args[3].equalsIgnoreCase("false")) {
                                            d.setOpen(false);
                                            dm.sendMessage(IF + "Closed dungeon " + d.getName());
                                        }
                                    }
                                }
                            }
                        }
                    } else dm.sendMessage(ER + "Unknown argument " + args[0]);
                }
            }
        }
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
            DungeonMaster dm = DungeonMaster.get(p);
            if (sender.isOp()) {
                if (args.length == 1) {
                    s.addAll(DungeonCreatorCommandExecutor.getDungeonNames());
                    s.addAll(List.of("info"));
                    return s;
                } else if (args.length == 2) s.addAll(List.of("set"));
                else switch (args[1]) {
                    case "set" -> {
                        if (args.length < 4) return List.of("open");
                        else switch (args[2].toLowerCase()) {
                            case "open" -> { return List.of("true", "false"); }
                        }
                    }
                }
            } else {

            }
            return s;
        }
    }
}
