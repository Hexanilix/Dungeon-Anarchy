package org.hexils.dnarch.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.DA_item;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            return true;
        }
        Player p = (Player) sender;
        switch (args[0]) {
            case "help" -> {
                p.sendMessage(HELP_MSG);
            }
            case "name" -> {p.sendMessage(DA_item.get(p.getInventory().getItemInMainHand()).getName());}
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
