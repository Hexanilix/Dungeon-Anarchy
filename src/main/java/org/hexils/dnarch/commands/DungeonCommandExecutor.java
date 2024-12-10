package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.actions.ModifyBlock;
import org.hexils.dnarch.objects.actions.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hetils.mpdl.GeneralUtil.log;

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
                                IF, di.name, di.difficulty, di.description
                        ));
                    } else p.sendMessage(W + "You're currently not in a dungeon. No info to show");
                }
            }
            return true;
        }
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        switch (args[0].toLowerCase()) {
            case "edit" -> {
                if (!dm.isEditing()) {
                    Dungeon d;
                    if (args.length == 2) {
                        d = Dungeon.get(args[1]);

                        if (d == null) {
                            p.sendMessage(ER + "No dungeon named \"" + args[1] + "\"");
                        } else {
                            dm.setCurrentDungeon(d);
                        }
                    } else {
                        d = Dungeon.get(p.getLocation());
                        if (d == null) {
                            p.sendMessage(ER + "You're currently not in a dungeon, please go into the desired dungeon or specify one.");
                        } else {
                            dm.setCurrentDungeon(d);
                            p.sendMessage(OK + "Editing dungeon " + d.getDungeonInfo().name);
                        }
                    }
                } else {
                    p.sendMessage(W + "You're already editing dungeon \"" + dm.getCurrentDungeon().getName() + "\"!");
                }
            }
            case "show" -> {
                if (dm.isEditing()) {
                    dm.getCurrentDungeon().displayDungeon(p);
                    p.sendMessage(OK + "Showing dungeon " + dm.getCurrentDungeon().getDungeonInfo().name);
                } else {
                    Dungeon d = Dungeon.get(p.getLocation());
                    if (d != null) d.displayDungeon(p);
                    else p.sendMessage(W + "No dungeon to show.");
                }
            }
            case "hide" -> {
                dm.hideSelections();
                p.sendMessage(IF + "Hid all selections");
            }
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
                        case "edit" -> {
                            if (!dm.isEditing() && args.length == 2)
                                Dungeon.dungeons.stream().filter(d -> d.getName().startsWith(args[1])).forEach(d -> s.add(d.getName()));
                        }
                    }
                }
            } else {

            }
            return s;
        }
    }
}
