package org.hexils.dnarch.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.hexils.dnarch.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

import static org.hexils.dnarch.Main.log;

public final class DungeonAnarchyCommandExecutor implements CommandExecutor {
    public static ChatColor IF = ChatColor.AQUA;
    public static ChatColor OK = ChatColor.GREEN;
    public static ChatColor W = ChatColor.YELLOW;
    public static ChatColor ER = ChatColor.RED;

    public static final String HELP_MSG = IF +
            "/dungeon, /dg - The command to manage dungeons";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return true;
        }
        Player p = sender instanceof Player ? (Player) sender : null;
        DungeonMaster dm = p != null ? DungeonMaster.get(p) : null;
        switch (args[0]) {
            //TODO VERY glitchy, especially with manageables
            case "reload" -> {
                if (args.length == 1) {
                    log(Level.INFO, "Reloading...");
                    if (dm != null) dm.sendMessage(IF + "Reloading...");
                    FileManager.loadData();
                    log(Level.INFO, "Done.");
                    if (dm != null) dm.sendMessage(OK + "Done.");
                } else switch (args[1]) {
                    case "from" -> {
                        if (args.length == 2) {

                        } else switch (args[2]) {
                            case "file" -> {
                                if (args.length == 3) {

                                } else {
                                    Dungeon d = Dungeon.get(args[3]);
                                    if (d != null)
                                        FileManager.reloadDungeon(d);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (sender instanceof ConsoleCommandSender console) {
//            switch (args[0]) {
//                case "dptest" -> {
//                    OODP dp = Main.dp;
//                    ItemStack it1 = newItemStack(Material.SPRUCE_WOOD, "wood haha", 847356, List.of("lad", "shsbavahddhfahdfambnla", "shalala"));
//                    NSK.setNSK(it1, Manageable.ITEM_ACTION_NSK, "lalalhggjnm.,kgala");
//                    String dpd = dp.toOodp(it1);
//                    ItemStack it2 = dp.map(dpd).as(ItemStack.class);
//                    log(it1);
//                    log(it2);
//                    log(it1.equals(it2));
//                }
//                case "nsk" -> {
//                    ItemStack i = new ItemStack(Material.COMPARATOR);
//                    NSK[] nsks = new NSK[]{Manageable.nsk(Manageable.NSKType.ITEM_ACTION), Manageable.nsk(Manageable.NSKType.MODIFIABLE)};
//                    Object[] vals = new Object[]{"yuh", false};
//                    NSK.setNSK(i, nsks, vals);
//                    log(i);
//                }
//            }
            return true;
        }
        if (dm != null) switch (args[0]) {
            case "help" -> dm.sendMessage(HELP_MSG);
            case "load" -> {
                if (args.length == 1) {
                    FileManager.loadData();
                } else switch (args[1]) {
                    case "config" -> FileManager.loadConfig();
                    case "dungeon" -> FileManager.loadDungeons();
                    case "permissions" -> FileManager.loadPermittedPlayers();
                }
            }
            //TODO make a permissions plugin
            case "permitted_players" -> {
                dm.sendMessage(ChatColor.AQUA + "Permitted players (excluding operators):\n- " + String.join("\n- ", DungeonMaster.permittedPlayers.stream().map(u -> {
                    Player pl = Bukkit.getPlayer(u);
                    String n = u.toString();
                    if (pl != null) n = p.getName();
                    return n;
                }).toList()));
            }
            case "file_manager" -> {
            }
            case "config" -> {
                //TODO config
            }
            case "permit" -> {
                if (p.isOp()) {
                    if (args.length > 1) {
                        Player pp = Bukkit.getPlayer(args[1]);
                        if (pp != null) {
                            UUID id = pp.getUniqueId();
                            boolean a = args.length == 2 || args[2].equalsIgnoreCase("true");
                            if (a) {
                                if (!DungeonMaster.permittedPlayers.contains(id)) {
                                    DungeonMaster.permittedPlayers.add(id);
                                    dm.sendInfo("Permitted " + pp.getName() + " to manage Dungeon Anarchy");
                                } else dm.sendInfo("Player " + pp.getName() + " is already a permitted admin");
                            } else {
                                DungeonMaster.permittedPlayers.remove(id);
                                dm.sendError("Denied " + pp.getName() + " permission to manage Dungeon Anarchy");
                            }
                        }
                    }
                }
            }
            case "debug" -> {
                if (DungeonMaster.permittedPlayers.contains(p.getUniqueId())) {
                    if (args.length > 1) switch (args[1]) {
                        case "logOodp" -> {
                            if (args.length > 2) {
                                if (args[2].equalsIgnoreCase("true")) {
                                    Main.debug.logConvertingProcess(true);
                                    dm.sendInfo(DungeonMaster.Sender.DEBUG, "Enabled logging of the dungeon saving process");
                                } else if (args[2].equalsIgnoreCase("false")) {
                                    Main.debug.logConvertingProcess(false);
                                    dm.sendInfo(DungeonMaster.Sender.DEBUG, "Disabled logging of the dungeon saving process");
                                }
                            }
                        }
                        case "item" -> {
                            if (args.length > 2) {
                                try {
                                    DAItem da = DAItem.get(UUID.fromString(args[2]));
                                    if (da != null) {
                                        dm.giveItem(da);
                                        dm.sendInfo(DungeonMaster.Sender.DEBUG, "Found " + da.getName());
                                    }
                                    else dm.sendError(DungeonMaster.Sender.DEBUG,"No item with id " + args[2]);
                                } catch (Exception e) {
                                    dm.sendError(DungeonMaster.Sender.DEBUG, "Invalid UUID");
                                }
                            } else dm.sendWarning(DungeonMaster.Sender.DEBUG, "Provide a UUID");
                        }
                    }
                } else dm.sendMessage(ER + "No permission.");
            }
        }
        return true;
    }

    public static final class Tab implements TabCompleter {

        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            List<String> s = new ArrayList<>();
            Player p = (Player) sender;
            if (args.length == 1) {
                s.addAll(List.of("dungeon_manager"));
                if (DungeonMaster.isPermitted(p)) s.addAll(List.of("debug", "permit", "reload"));
            } else {
                switch (args[0]) {
                    case "dungeon_manager" -> {
                        return DungeonCreatorCommandExecutor.complete(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
                    }
                    case "reload" -> {
                        if (args.length == 2) {
                            s.addAll(List.of("from"));
                        } else switch (args[1]) {
                            case "from" -> {
                                if (args.length == 3) {
                                    s.addAll(List.of("file"));
                                } else switch (args[2]) {
                                    case "file" -> DungeonCreatorCommandExecutor.getDungeonNames();
                                }
                            }
                        }
                    }
                    case "debug" -> {
                        if (args.length == 2) return List.of("loc_dungeon_saving_process");
                        else switch (args[1]) {
                            case "loc_dungeon_saving_process" -> { return List.of("true", "false"); }
                        }
                    }
                    case "permit" -> {
                        if (DungeonMaster.isPermitted(p)) { if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(); }
                    }
                    case "file_manager" -> {
                        if (args.length == 2) return List.of("");
                        else switch (args[1]) {
                            case "dungeon" -> { if (args.length == 3) return Arrays.stream(FileManager.dungeon_dir.listFiles()).map(File::getName).toList(); }
                        }
                    }
                }
            }
            return s;
        }
    }
}
