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

import static org.hetils.mpdl.General.log;

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
                    dm.setBuildMode(!dm.inBuildMode());
                    if (!dm.inBuildMode()) {
                        dm.getCurrentDungeon().save();
                        p.sendMessage(OK + "Saved dungeon");
                    }
                    p.sendMessage(IF + "You are " + (dm.inBuildMode() ? "now" : "no longer") + " in build mode.");
//                    p.sendMessage(W + "You're already editing dungeon \"" + dm.getCurrentDungeon().getName() + "\"!");
                }
            }
            case "save" -> {
                if (!dm.isEditing()) p.sendMessage(ER + "You must be currently editing a dungeon to save it");
                else {
                    dm.getCurrentDungeon().save();
                    dm.setCurrentDungeon(null);
                    p.sendMessage(OK + "Saved dungeon " + dm.getCurrentDungeon().getName());
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
            case "manage" -> {
                if (!dm.isEditing()) p.sendMessage(ER + "You must be editing a dungeon to manage stuff!");
                else dm.getCurrentDungeon().manage(p);
            }
            case "create" -> {
                if (dm.hasAreaSelected()) {
                    Dungeon d = null;
                    if (args.length > 1) {
                        //TODO FIX
//                        try {
//                            d = new Dungeon(dm.p.getUniqueId(), args[1], dm.getSelectedArea());
//                        } catch (Dungeon.DuplicateNameException ignore) {
//                            p.sendMessage(ER + "Dungeon " + args[1] + " already exists.");
//                            return true;
//                        }
                    } else d = new Dungeon(dm.p.getUniqueId(), dm.getSelectedArea());
                    dm.clearSelection();
                    dm.setCurrentDungeon(d);
                    p.sendMessage(OK + "Created new dungeon " + d.getName());
                } else p.sendMessage(ER + "You must select a section to create a dungeon");
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
                    s.addAll(List.of("show", "edit"));
                    if (!dm.isEditing()) s.addAll(List.of("create"));
                    else s.addAll(List.of("manage", "hide", "save"));
                } else {
                    switch (args[0]) {
                        case "create" -> {
                            if (dm.isEditing()) {
                                if (args.length == 2) {
                                    s.addAll(List.of("section", "action", "trigger", "condition"));
                                } else switch (args[1].toLowerCase()) {
                                    case "action" -> {
                                        if (args.length == 3)
                                            return Arrays.stream(Type.values()).map(e -> e.name().toLowerCase()).toList();
                                        else {
                                            Type at = Type.get(args[2]);
                                            switch (at) {
                                                case MODIFY_BLOCK -> {
                                                    return Arrays.stream(ModifyBlock.ModType.values()).map(e -> e.name().toLowerCase()).toList();
                                                }
                                            }
                                        }
                                    }
                                    case "condition" -> {
                                        if (args.length == 3)
                                            return Arrays.stream(org.hexils.dnarch.objects.conditions.Type.values()).map(e -> e.name().toLowerCase()).toList();
                                        else {
                                        }
                                    }
                                }
                            } else s.add("dungeon");
                        }
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
