package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.*;
import org.hexils.dnarch.dungeon.Dungeon;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.hexils.dnarch.objects.Trigger;
import org.hexils.dnarch.objects.actions.*;
import org.hexils.dnarch.objects.conditions.Distance;
import org.hexils.dnarch.EntitySpawn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hetils.mpdl.Block.block_materials;
import static org.hetils.mpdl.General.log;

public final class dc_cmd implements CommandExecutor {

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
                        dm.setSelectionA(p.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])).getLocation());
                    } else return ER + "Please provide x, y, z coordinates!";
                } else {
                    dm.setSelectionA(p.getLocation().getBlock().getLocation());
                }
                return OK + "Set 1st position to " + org.hetils.mpdl.Location.toReadableFormat(dm.getSelectionA());
            }
            case "pos2" -> {
                if (args.length > 1) {
                    if (args.length > 3) {
                        dm.setSelectionB(p.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])).getLocation());
                    } else return ER + "Please provide x, y, z coordinates!";
                } else {
                    dm.setSelectionB(p.getLocation().getBlock().getLocation());
                }
                return OK + "Set 2nd position to " + org.hetils.mpdl.Location.toReadableFormat(dm.getSelectionB());
            }
            case "create" -> {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("dungeon")) {
                        return Dungeon.commandNew(dm, Arrays.copyOfRange(args, 2, args.length));
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
                                    } else dm.give(Action.create(Type.get(args[2]), dm, Arrays.copyOfRange(args, 3, args.length)));
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
                if (DA_item.get(p.getInventory().getItemInMainHand()) instanceof Action a)
                    a.execute();
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
                    a.promptRename(p);
            }
            case "edit" -> {
                if (!dm.isEditing()) {
                    Dungeon d;
                    if (args.length == 2) {
                        d = Dungeon.get(args[1]);

                        if (d == null) {
                            return ER + "No dungeon named \"" + args[1] + "\"";
                        } else {
                            dm.setCurrent_dungeon(d);
                        }
                    } else {
                        d = Dungeon.get(p.getLocation());
                        if (d == null) {
                            return ER + "You're currently not in a dungeon, please go into the desired dungeon or specify one.";
                        } else {
                            dm.setCurrent_dungeon(d);
                            return OK + "Editing dungeon " + d.getName() + "...";
                        }
                    }
                } else p.sendMessage(W + "You're already editing dungeon \"" + dm.getCurrentDungeon().getName() + "\"!");
            }
            case "save" -> {
                if (!dm.isEditing()) return ER + "You must be currently editing a dungeon to save it";
                r = OK + "Saved dungeon " + dm.getCurrentDungeon().getName();
                dm.setCurrent_dungeon(null);
            }
            case "show" -> {
                if (dm.isEditing()) {
                    dm.getCurrentDungeon().displayDungeon(p);
                } else {
                    Dungeon d = Dungeon.get(p.getLocation());
                    if (d != null) d.displayDungeon(p);
                    else return W + "No dungeon to show.";
                }
            }
            case "hide" -> dm.hideSelections();
            case "manage" -> {
                if (!dm.isEditing()) return ER + "You must be editing a dungeon to manage stuff!";
                dm.getCurrentDungeon().manage(p);
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
            case "create" -> {
                if (args.length == 2) {
                    s.add("dungeon");
                    if (dm.isEditing()) s.addAll(List.of("section", "dungeon", "action", "trigger", "condition"));
                } else switch (args[1].toLowerCase()) {
                    case "action" -> {
                        if (args.length == 3) return Arrays.stream(Type.values()).map(e -> e.name().toLowerCase()).toList();
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
                        if (args.length == 3) return Arrays.stream(org.hexils.dnarch.objects.conditions.Type.values()).map(e -> e.name().toLowerCase()).toList();
                        else {}
                    }
                }
            }
            case "edit" -> {
                if (args.length == 2) Dungeon.dungeons.stream().filter(d -> d.getName().startsWith(args[1])).forEach(d -> s.add(d.getName()));
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
