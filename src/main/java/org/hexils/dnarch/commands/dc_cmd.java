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
import org.hexils.dnarch.objects.actions.DestroyBlock;
import org.hexils.dnarch.objects.actions.ReplaceBlock;
import org.hexils.dnarch.objects.actions.Spawn;
import org.hexils.dnarch.objects.actions.Type;
import org.hexils.dnarch.objects.conditions.Distance;
import org.hexils.dnarch.EntitySpawn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
                        if (dm.hasAreaSelected()) {
                            Dungeon d;
                            if (args.length > 2) {
                                try {
                                    d = new Dungeon(p.getUniqueId(), args[2], dm.getSelectedArea());
                                } catch (Dungeon.DuplicateNameException ignore) {
                                    return ER + "Dungeon " + args[2] + " already exists.";
                                }
                            } else d = new Dungeon(p.getUniqueId(), dm.getSelectedArea());
                            dm.clearSelection();
                            dm.setCurrent_dungeon(d);
                            return OK + "Created new dungeon " + d.getName();
                        } else return ER + "You must select a section to create a dungeon";
                    } else {
                        if (dm.isEditing()) {
                            switch (args[1].toLowerCase()) {
                                case "section" -> {
                                    if (dm.hasAreaSelected()) {
                                        if (dm.isEditing()) {
                                            Dungeon d = dm.getCurrentDungeon();
                                            if (args.length > 2) {
                                                d.newSection(dm.getSelectedArea(), args[2]);
                                                dm.clearSelection();
                                                return OK + "Created '"+ d.getName() +"' section " + args[2];
                                            } else {
                                                d.newSection(dm.getSelectedArea());
                                                dm.clearSelection();
                                                return OK + "Created '"+ d.getName() +"' section";
                                            }
                                        } else return ER + "You must be currently editing a dungeon to create sections!";
                                    } else return ER + "You need to first select an area to create a section!";
                                }
                                case "action" -> {
                                    if (args.length == 2) {
                                        return ER + "Please specify the action type!";
                                    }
                                    DA_item i = null;
                                    switch (args[2]) {
                                        case "replace_block" -> {
                                            i = new ReplaceBlock(dm.getSelectedBlocks(), Material.getMaterial(args[3].toUpperCase()));
                                            dm.deselectBlocks();
                                        }
                                        case "destroy_block" -> {
                                            i = new DestroyBlock(dm.getSelectedBlocks());
                                            dm.deselectBlocks();
                                        }
                                        case "spawn" -> {
                                            i = new Spawn(new EntitySpawn(EntityType.WOLF), org.hetils.mpdl.Block.toLocations(dm.getSelectedBlocks()));
                                            dm.deselectBlocks();
                                        }
                                    }
                                    if (i != null) dm.give(i);
                                }
                                case "condition" -> {
                                    if (args.length == 2) {
                                        return ER + "Please specify the condition type!";
                                    }
                                    ItemStack item = null;
                                    switch (args[2]) {
                                        case "distance" -> {
                                            item = new Distance(p.getLocation()).getItem();
                                        }
                                    }
                                    if (item != null) p.getInventory().addItem(item);
                                }
                                case "trigger" -> p.getInventory().addItem(new Trigger().getItem());
                            }
                        } else p.sendMessage(ER + "You must be currently editing a dungeon to create elements");
                    }
                } else {
                    p.sendMessage(ER + "Please specify what to create!");
                }
            }
            case "remove" -> {}
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
                DA_item a = DA_item.get(p);
                if (a != null)
                    a.promptRename(p);
            }
            case "edit" -> {
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
                    else return ChatColor.YELLOW + "No dungeon to show.";
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

    public static @NotNull List<String> complete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> s = new ArrayList<>();
        if (args.length == 0) {

            return s;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return s;
        }
        Player p = (Player) sender;
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        if (args.length > 1) {
            switch (args[0]) {
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
                    if (dm.isEditing()) {
                        if (args.length > 2) {
                            switch (args[1]) {
                                case "action" -> {
                                    if (args.length > 3) {
                                        switch (args[2]) {
                                            case "replace_block" -> {
                                                return block_materials.stream().filter(m -> m.startsWith(args[3])).toList();
                                            }
                                        }
                                    } else {
                                        for (Type t : Type.values())
                                            s.add(t.name().toLowerCase());
                                    }
                                }
                                case "condition" -> {
                                    for (org.hexils.dnarch.objects.conditions.Type t : org.hexils.dnarch.objects.conditions.Type.values())
                                        s.add(t.name().toLowerCase());
                                }
                            }
                        } else {
                            s.addAll(List.of("section", "dungeon", "action", "trigger", "condition"));
                        }
                    } else {
                        if (args.length == 2) {
                            s.add("dungeon");
                        }
                    }
                }
                case "edit" -> Dungeon.dungeons.forEach(d -> {
                    if (d.getName().startsWith(args[1])) s.add(d.getName());
                });
            }
        } else {
            s.addAll(List.of("pos1", "pos2", "hide", "show", "create", "edit"));
            if (dm.isEditing()) s.addAll(List.of("run", "reset", "rename", "save"));
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
