package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.*;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.Trigger;
import org.hexils.dnarch.objects.actions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.General.log;

public final class DungeonCreatorCommandExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String s = execute(sender, command, label, args);
        if (s != null) sender.sendMessage(s);
        return true;
    }

    public static ChatColor ER = ChatColor.RED;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;

    public static @Nullable String execute(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return null;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return null;
        }
        Player p = (Player) sender;
        if (!sender.isOp()) {
            return ER + "You cannot use this command!";
        }
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        String r = null;
        switch (args[0].toLowerCase()) {
            case "wand" -> p.getInventory().addItem(Main.wand);
            case "pos1" -> {
                if (args.length > 1) {
                    if (args.length > 3) {
                        dm.setSelectionA(p.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])));
                    } else return ER + "Please provide x, y, z coordinates!";
                } else {
                    dm.setSelectionA(p.getLocation().getBlock());
                }
                return OK + "Set 1st position to " + org.hetils.mpdl.Location.toReadableFormat(dm.getSelectionA());
            }
            case "pos2" -> {
                if (args.length > 1) {
                    if (args.length > 3) {
                        dm.setSelectionB(p.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])));
                    } else return ER + "Please provide x, y, z coordinates!";
                } else {
                    dm.setSelectionB(p.getLocation().getBlock());
                }
                return OK + "Set 2nd position to " + org.hetils.mpdl.Location.toReadableFormat(dm.getSelectionB());
            }
            case "create" -> {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("dungeon")) {
                    } else {
                        if (dm.isEditing()) {
                            switch (args[1].toLowerCase()) {
                                case "section" -> {
                                    return Dungeon.Section.commandNew(dm, Arrays.copyOfRange(args, 3, args.length));
                                }
                                case "action" -> {
                                    log(Arrays.toString(Arrays.copyOfRange(args, 3, args.length)));
                                    if (args.length == 2) {
                                        return ER + "Please specify the action type!";
                                    } else dm.give(Action.commandCreate(Type.get(args[2]), dm, Arrays.copyOfRange(args, 3, args.length)));
                                }
                                case "condition" -> {
                                    if (args.length == 2) {
                                        return ER + "Please specify the condition type!";
                                    } else dm.give(Condition.create(org.hexils.dnarch.objects.conditions.Type.get(args[3]), dm, Arrays.copyOfRange(args, 2, args.length-1)));
                                }
                                case "trigger" -> p.getInventory().addItem(new Trigger().getItem());
                            }
                        } else p.sendMessage(ER + "You must be currently editing a dungeon to create elements");
                    }
                } else {
                    p.sendMessage(ER + "Please specify what to create!");
                }
            }
            case "remove" -> {
            }
            case "run" -> {
                if (!dm.isEditing()) p.sendMessage(ER + "You must be currently editing a dungeon to run elements");
                if (DA_item.get(p.getInventory().getItemInMainHand()) instanceof Triggerable t)
                    t.trigger();
                else p.sendMessage(ER + "Please hold a executable item");
            }
            case "reset" -> {
                if (!dm.isEditing()) p.sendMessage(ER + "You must be currently editing a dungeon to reset elements");
                DA_item da = DA_item.get(p.getInventory().getItemInMainHand());
                if (da instanceof Action a)
                    a.reset();
                else if (da instanceof Trigger t)
                    t.actions.forEach(Action::reset);
                else p.sendMessage(ER + "Please hold a resetable item");
            }
            case "rename" -> {
                if (!dm.isEditing()) return ER + "You must be currently editing a dungeon to rename elements";
                DA_item a = DA_item.get(p.getInventory().getItemInMainHand());
                if (a != null)
                    a.rename(p);
            }

            case "start" -> {
                dm.getCurrentDungeon().start();
                log("Started dungeon");
            }
        }
        return r;
    }

    public interface TabCompleteRunnable { List<String> complete(); }
    public static final Map<Type, TabCompleteRunnable> acTabMap;
    static {
        acTabMap = new HashMap<>();
        acTabMap.put(Type.MODIFY_BLOCK, () -> Arrays.stream(ModifyBlock.ModType.values()).map(Enum::name).toList());
    }

    public static @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> s = new ArrayList<>();
        if (sender instanceof ConsoleCommandSender console) {
            return s;
        }
        Player p = (Player) sender;
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        if (args.length <= 1) {
            s.addAll(List.of("pos1", "pos2", "hide", "show", "create", "edit"));
            if (dm.isEditing()) s.addAll(List.of("run", "reset", "rename", "save"));
        } else switch (args[0].toLowerCase()) {
            case "pos1", "pos2" -> {
                Block b = p.getTargetBlockExact(50);
                Location l;
                if (b == null) l = p.getLocation();
                else l = b.getLocation();
                s.add(String.valueOf(switch (args.length) {
                    case 2 -> (int) l.getX();
                    case 3 -> (int) l.getY();
                    case 4 -> (int) l.getZ();
                    default -> "";
                }));
            }
        }
        return s;
    }

    public static final class tab implements TabCompleter {
        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            return complete(sender, command, label, args);
        }
    }
}
