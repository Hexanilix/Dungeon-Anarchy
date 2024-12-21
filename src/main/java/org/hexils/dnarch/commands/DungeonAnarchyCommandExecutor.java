package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hetils.jgl17.oodp.OODP;
import org.hetils.mpdl.NSK;
import org.hexils.dnarch.DA_item;
import org.hexils.dnarch.Main;
import org.hexils.dnarch.Manageable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hetils.mpdl.ItemUtil.newItemStack;
import static org.hexils.dnarch.Main.log;
import static org.hexils.dnarch.Manageable.ITEM_ACTION;

public final class DungeonAnarchyCommandExecutor implements CommandExecutor {
    public static final ChatColor A = ChatColor.AQUA;
    public static final String HELP_MSG = A +
            "/dungeon, /dg - The command to manage dungeons";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender console) {
            switch (args[0]) {
                case "dptest" -> {
                    OODP dp = Main.dp;
                    ItemStack it1 = newItemStack(Material.SPRUCE_WOOD, "wood haha", 847356, List.of("lad", "shsbavmbnla", "shalala"));
                    NSK.setNSK(it1, ITEM_ACTION, "lalalhggjnm.,kgala");
                    String dpd = dp.toOodp(it1);
                    ItemStack it2 = dp.map(dpd).as(ItemStack.class);
                    log(it1);
                    log(it2);
                    log(it1.equals(it2));
                }
                case "nsk" -> {
                    ItemStack i = new ItemStack(Material.COMPARATOR);
                    NSK[] nsks = new NSK[]{ITEM_ACTION, Manageable.MODIFIABLE};
                    Object[] vals = new Object[]{"yuh", false};
                    NSK.setNSK(i, nsks, vals);
                    log(i);
                }
            }
            return true;
        }
        Player p = (Player) sender;
        switch (args[0]) {
            case "help" -> {
                p.sendMessage(HELP_MSG);
            }
            case "name" -> {p.sendMessage(DA_item.get(p.getInventory().getItemInMainHand()).getName());}
            case "dc" -> {
                log("setb");
                p.performCommand("dungeon_anarchy:dc create dungeon");
            }
            case "testdp" -> {
                OODP dp = Main.dp;
                ItemStack it1 = p.getInventory().getItemInMainHand();
                log(it1);
                String dpd = dp.toOodp(it1);
                log(dpd);
                ItemStack it2 = dp.map(dpd).as(ItemStack.class);
                log(it2);
                log(it1.equals(it2));
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
                        return DungeonCreatorCommandExecutor.complete(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
                    }
                }
            } else s.addAll(List.of("dungeon_manager"));
            return s;
        }
    }
}
