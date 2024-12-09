package org.hexils.dnarch.commands;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.hetils.jgl17.Pair;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.GUI;
import org.hexils.dnarch.dungeon.DungeonMaster;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hetils.mpdl.General.log;

public final class da_cmd implements CommandExecutor {
    public static boolean withinBounds(Pair<Location, Location> bound, Location l) {
        return l != null && (Math.max(bound.key().getX(), bound.value().getX()) > l.getX() && l.getX() > Math.min(bound.key().getX(), bound.value().getX()))&&
                (Math.max(bound.key().getY(), bound.value().getY()) > l.getY() && l.getY() > Math.min(bound.key().getY(), bound.value().getY()))&&
                (Math.max(bound.key().getZ(), bound.value().getZ()) > l.getZ() && l.getZ() > Math.min(bound.key().getZ(), bound.value().getZ()));
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender console) {
            return true;
        }
        Player p = (Player) sender;
        switch (args[0]) {
            case "set" -> {
                NSK.setNSK(p, GUI.ITEM_FIELD_VALUE, "nigga");
            }
            case "get" -> {
                p.sendMessage((String) NSK.getNSK(p, GUI.ITEM_FIELD_VALUE));
            }
            case "selb" -> {

                log(DungeonMaster.dms.size() + "," + DungeonMaster.getOrNew(p).getSelectionBlocks());
            }
            case "dungeon_manager" -> {
                String s = dc_cmd.execute(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
                if (s != null) p.sendMessage(s);
            }
            case "inspect" -> {
                ItemStack i = p.getInventory().getItemInMainHand();
                p.sendMessage(i.toString());
            }
            case "parti" -> {
                org.hetils.mpdl.Particle.displayAllParticles(p);
            }
            case "part" -> {
                org.hetils.mpdl.Particle.dispao(p);
            }
        }
        return true;
    }


    public static final class tab implements TabCompleter {

        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            List<String> s = new ArrayList<>();
            if (args.length > 1) {
                switch (args[0]) {
                    case "dungeon_manager" -> {
                        return dc_cmd.complete(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
                    }
                }
            } else s.addAll(List.of("dungeon_manager"));
            return s;
        }
    }
}
