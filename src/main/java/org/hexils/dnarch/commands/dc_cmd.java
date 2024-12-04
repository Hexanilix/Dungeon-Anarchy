package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.Pair;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.da.*;
import org.hexils.dnarch.da.actions.ReplaceBlock;
import org.hexils.dnarch.da.actions.Spawn;
import org.hexils.dnarch.da.conditions.Distance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.hexils.dnarch.Main.log;

public final class dc_cmd implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return true;
        }
        Player p = (Player) sender;
        DM dm = DM.getOrNew(p);
        switch (args[0].toLowerCase()) {
            case "wand" -> p.getInventory().addItem(Main.wand);
            case "create" -> {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("dungeon")) {
                        if (dm.hasAreaSelected()) {
                            Pair<Location, Location> sl = dm.getSelectedArea();
                            Dungeon d;
                            if (args.length > 2) {
                                try {
                                    d = new Dungeon(p.getUniqueId(), args[2], dm.getSelectedArea());
                                } catch (Dungeon.DuplicateNameException ignore) {
                                    p.sendMessage(ChatColor.RED + "Dungeon " + args[2] + " already exists.");
                                    return true;
                                }
                            } else d = new Dungeon(p.getUniqueId(), dm.getSelectedArea());
                            dm.clearSelection();
                            dm.setCurrent_dungeon(d);
                            p.sendMessage(ChatColor.GREEN + "Created new dungeon " + d.getName());
                        } else p.sendMessage(ChatColor.RED + "You must select a section to create a dungeon");
                    } else {
                        if (dm.isEditing()) {
                            switch (args[1].toLowerCase()) {
                                case "section" -> {
                                    if (dm.hasAreaSelected()) {
                                        if (dm.isEditing()) {
                                            Dungeon d = dm.getCurrent_dungeon();
                                            if (args.length > 2) {
                                                d.newSection(dm.getSelectedArea(), args[2]);
                                                p.sendMessage(ChatColor.GREEN + "Created '"+ d.getName() +"' section " + args[2]);
                                            } else {
                                                d.newSection(dm.getSelectedArea());
                                                p.sendMessage(ChatColor.GREEN + "Created '"+ d.getName() +"' section");
                                            }
                                            dm.clearSelection();
                                        } else p.sendMessage(ChatColor.RED + "You must be currently editing a dungeon to create sections!");
                                    } else p.sendMessage(ChatColor.RED + "You need to first select an area to create a section!");
                                }
                                case "action" -> {
                                    if (args.length == 2) {
                                        p.sendMessage(ChatColor.RED + "Please specify the action type!");
                                        return true;
                                    }
                                    switch (args[2]) {
                                        case "replace" -> {
                                            Action a = new ReplaceBlock(dm.getSelectedBlocks(), Material.getMaterial(args[3].toUpperCase()));
                                            dm.give(a);
                                            dm.deselectBlocks();
                                        }
                                        case "spawn" -> {
                                            Spawn s = new Spawn(new EntitySpawn(EntityType.WOLF), toLocations(dm.getSelectedBlocks()));
                                            dm.give(s);
                                            dm.deselectBlocks();
                                        }
                                    }
                                }
                                case "condition" -> {
                                    if (args.length == 2) {
                                        p.sendMessage(ChatColor.RED + "Please specify the condition type!");
                                        return true;
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
                        } else p.sendMessage(ChatColor.RED + "You must be currently editing a dungeon to create elements");
                    }
                } else {
                    p.sendMessage(ChatColor.RED + "Please specify what to create!");
                }
            }
            case "run" -> {
                if (!dm.isEditing()) p.sendMessage(ChatColor.RED + "You must be currently editing a dungeon to run elements");
                if (DA_item.get(p.getInventory().getItemInMainHand()) instanceof Action a)
                    a.execute();
                else p.sendMessage(ChatColor.RED + "Please hold a executable item");
            }
            case "reset" -> {
                if (!dm.isEditing()) p.sendMessage(ChatColor.RED + "You must be currently editing a dungeon to reset elements");
                DA_item da = DA_item.get(p.getInventory().getItemInMainHand());
                if (da instanceof Action a)
                    a.reset();
                else if (da instanceof Trigger t)
                    t.actions.forEach(Action::reset);
                else p.sendMessage(ChatColor.RED + "Please hold a resetable item");
            }
            case "rename" -> {
                if (!dm.isEditing()) p.sendMessage(ChatColor.RED + "You must be currently editing a dungeon to rename elements");
                DA_item a = DA_item.get(p);
                if (a != null)
                    a.promptRename(p);
            }
            case "edit" -> {
                Dungeon d;
                if (args.length == 2) {
                    d = Dungeon.get(args[1]);
                    if (d == null) {
                        p.sendMessage(ChatColor.RED + "No dungeon named \"" + args[1] + "\"");
                    } else {
                        dm.setCurrent_dungeon(d);
                    }
                } else {
                    d = Dungeon.get(p.getLocation());
                    if (d == null) {
                        p.sendMessage(ChatColor.RED + "You're currently not in a dungeon, please go into the desired dungeon or specify one.");
                    } else {
                        dm.setCurrent_dungeon(d);
                        p.sendMessage(ChatColor.GREEN + "Editing dungeon " + d.getName() + "...");
                    }
                }
            }
            case "save" -> {
                if (!dm.isEditing()) p.sendMessage(ChatColor.RED + "You must be currently editing a dungeon to save it");
                p.sendMessage(ChatColor.GREEN + "Saved dungeon " + dm.getCurrent_dungeon().getName());
                dm.setCurrent_dungeon(null);
            }
            case "show" -> {
                if (dm.isEditing()) {
                    dm.getCurrent_dungeon().displaySectors(p);
                } else {
                    Dungeon d = Dungeon.get(p.getLocation());
                    if (d != null) d.displaySectors(p);
                    else p.sendMessage(ChatColor.YELLOW + "No dungeon to show.");
                }
            }
            case "hide" -> dm.hideSelections();
        }
        return true;
    }

    private @NotNull List<Location> toLocations(List<Block> slb) {
        List<Location> l = new ArrayList<>();
        if (slb != null)
            for (Block b : slb)
                if (b != null)
                    l.add(b.getLocation().add(.5, .5, .5));
        return l;
    }

    public static final List<String> blockmats = new ArrayList<>();
    static {
        for (Material m : Material.values())
            if (m.isBlock()) blockmats.add(m.name().toLowerCase());
    }


    public static final class tab implements TabCompleter {
        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            List<String> s = new ArrayList<>();
            if (args.length == 0) {

                return s;
            }
            if (sender instanceof ConsoleCommandSender console) {
                return s;
            }
            Player p = (Player) sender;
            DM dm = DM.getOrNew(p);
            if (args.length > 1) {
                switch (args[0]) {
                    case "create" -> {
                        if (dm.isEditing()) {
                            if (args.length > 2) {
                                switch (args[1]) {
                                    case "action" -> {
                                        if (args.length > 3) {
                                            switch (args[2]) {
                                                case "replace_block" -> {
                                                    return blockmats.stream().filter(m -> m.startsWith(args[3])).toList();
                                                }
                                            }
                                        } else {
                                            for (org.hexils.dnarch.da.actions.Type t : org.hexils.dnarch.da.actions.Type.values())
                                                s.add(t.name().toLowerCase());
                                        }
                                    }
                                    case "condition" -> {
                                        for (org.hexils.dnarch.da.conditions.Type t : org.hexils.dnarch.da.conditions.Type.values())
                                            s.add(t.name().toLowerCase());
                                    }
                                }
                            } else {
                                s.addAll(List.of("dungeon", "action", "trigger", "condition"));
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
                s.addAll(List.of("hide", "show", "create", "edit"));
                if (dm.isEditing()) s.addAll(List.of("run", "reset", "rename", "save"));
            }
            return s;
        }
    }
}
