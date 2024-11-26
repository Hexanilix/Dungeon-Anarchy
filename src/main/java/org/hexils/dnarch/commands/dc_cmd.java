package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.MainListener;
import org.hexils.dnarch.da.*;
import org.hexils.dnarch.da.actions.ReplaceBlock;
import org.hexils.dnarch.da.conditions.Distance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hexils.dnarch.Main.log;

public final class dc_cmd implements CommandExecutor {
    private Map<Player, Dungeon> pdmap = new HashMap<>();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return true;
        }
        Player p = (Player) sender;
        switch (args[0].toLowerCase()) {
            case "wand" -> p.getInventory().addItem(Main.wand);
            case "create" -> {
                if (args.length > 1) {
                    switch (args[1].toLowerCase()) {
                        case "action" -> {
                            if (args.length == 2) {
                                p.sendMessage(ChatColor.RED + "Please specify the action type!");
                                return true;
                            }
                            switch (args[2]) {
                                case "replace" -> {
                                    DM dm = DM.getOrNew(p);
                                    Action a = new ReplaceBlock(dm.slb, Material.getMaterial(args[3].toUpperCase()));
                                    p.getInventory().addItem(a.getItem());
                                    dm.slb.clear();
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
                                case "distance" -> item = new Distance(p.getLocation()).getItem();
                            }
                            if (item != null) p.getInventory().addItem(item);
                        }
                        case "trigger" -> p.getInventory().addItem(new Trigger().getItem());
                        case "dungeon" -> {
                            pdmap.putIfAbsent(p, null);
                            pdmap.put(p, new Dungeon(new Dungeon.Sector(MainListener.getSelection(p))));
                        }
                    }
                } else {
                    p.sendMessage(ChatColor.RED + "Please specify what to create!");
                }
            }
            case "run" -> {
                    if (DA_item.get(p.getInventory().getItemInMainHand()) instanceof Action a)
                        a.execute();
                    else p.sendMessage(ChatColor.RED + "Please hold a executable item");
            }
            case "reset" -> {
                DA_item da = DA_item.get(p.getInventory().getItemInMainHand());
                if (da instanceof Action a)
                    a.reset();
                else if (da instanceof Trigger t)
                    t.actions.forEach(Action::reset);
                else p.sendMessage(ChatColor.RED + "Please hold a resetable item");
            }
            case "rename" -> {
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
                    }
                } else {
                    d = pdmap.get(p);
                    if (d == null) {
                        p.sendMessage(ChatColor.RED + "No dungeon currently selected");
                    }
                }
            }

            case "liltest" -> {
                ItemStack i = new ItemStack(Material.ANVIL);
                ItemMeta m = i.getItemMeta();
                m.getPersistentDataContainer().set(GUI.ITEM_RENAME.key(), GUI.ITEM_RENAME.type(), true);
            }
        }
        return true;
    }

    public static final List<String> materials = new ArrayList<>();
    static {
        for (Material m : Material.values())
            materials.add(m.name().toLowerCase());
    }

    public static final class tab implements TabCompleter {

        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            List<String> s = new ArrayList<>();
            if (args.length == 0) {

                return s;
            }
            if (sender instanceof ConsoleCommandSender console) {
                return s;
            }
            Player p = (Player) sender;
            if (args.length > 1) {
                switch (args[0]) {
                    case "create" -> {
                        if (args.length > 2) {
                            switch (args[1]) {
                                case "action" -> {
                                    if (args.length > 3) {
                                        switch (args[2]) {
                                            case "replace" -> {
                                                return materials;
                                            }
                                        }
                                    } else {
                                        s.add("replace");
                                    }
                                }
                                case "condition" -> {
                                    for (Condition.Type t : Condition.Type.values())
                                        s.add(t.name().toLowerCase());
                                }
                            }
                        } else {
                            s.add("action");
                            s.add("condition");
                            s.add("trigger");
                        }
                    }
                }
            } else {
                s.add("create");
                s.add("wand");
                s.add("run");
                s.add("reset");
            }
            return s;
        }
    }
}
