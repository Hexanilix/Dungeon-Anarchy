package org.hexils.dnarch.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.hetils.jgl17.oodp.OODP;
import org.hetils.mpdl.NSK;
import org.hetils.mpdl.VectorUtil;
import org.hexils.dnarch.*;
import org.hexils.dnarch.items.actions.ReplaceBlock;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

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
                    ItemStack it1 = newItemStack(Material.SPRUCE_WOOD, "wood haha", 847356, List.of("lad", "shsbavahddhfahdfambnla", "shalala"));
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
        DungeonMaster dm = DungeonMaster.getOrNew(p);
        switch (args[0]) {
            case "help" -> {
                dm.sendMessage(HELP_MSG);
            }
            case "rb" -> {
                ReplaceBlock rp = (ReplaceBlock) DAItem.get(p.getInventory().getItemInMainHand());
                log(rp.getAffectedBlocks());
            }
            case "name" -> {dm.sendMessage(DAItem.get(p.getInventory().getItemInMainHand()).getName());}
            case "dc" -> {
                log("sek,awtfgylghzddkghdhkgdhkg,./ m.fbzdfbdfblilgjkhhgljytb");
                dm.performCommand("dungeon_anarchy:dc create dungeon");
            }
            case "fireball" -> {
                int amnt = Integer.parseInt(args[1]);
                Random r = new Random();
                new BukkitRunnable() {
                    int a = 0;
                    @Override
                    public void run() {
                        a++;
                        Location loc = p.getEyeLocation().clone().add(r.nextDouble(), r.nextDouble(), r.nextDouble());
                        SmallFireball f = (SmallFireball) p.getWorld().spawnEntity(loc, EntityType.SMALL_FIREBALL);
                        f.setVelocity(VectorUtil.genVec(f.getLocation(), p.getTargetBlockExact(100).getLocation()).normalize().multiply(.2));
                        if (a == amnt) cancel();
                    }
                }.runTaskTimer(Main.plugin, 0, 0);
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
            case "load" -> {
                FileManager.loadDungeons();
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
            case "save" -> {
                DAItem da = DAItem.get(p.getInventory().getItemInMainHand());
                if (da != null) {
                    log(Main.dp.toOodp(da));
                } else dm.sendMessage("it aint a da item niga");
            }
            case "file_manager" -> {
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
                                    dm.sendMessage("Permitted " + pp.getName() + " to manage Dungeon Anarchy");
                                } else dm.sendMessage("Player " + pp.getName() + " is already a permitted admin");
                            } else {
                                DungeonMaster.permittedPlayers.remove(id);
                                dm.sendMessage("Denied " + pp.getName() + " permission to manage Dungeon Anarchy");
                            }
                        }
                    }
                }
            }
            case "id" -> {
                dm.sendMessage(p.getUniqueId().toString());
            }
            case "debug" -> {
                if (DungeonMaster.permittedPlayers.contains(p.getUniqueId())) {
                    if (args.length > 1) {
                        switch (args[1]) {
                            case "loc_dungeon_saving_process" -> {
                                if (args.length > 2) {
                                    if (args[2].equalsIgnoreCase("true")) {
                                        Main.dp.logConversions(true);
                                        dm.sendMessage("Enabled logging of the dungeon saving process");
                                    }
                                    else if (args[2].equalsIgnoreCase("false")) {
                                        Main.dp.logConversions(false);
                                        dm.sendMessage("Disabled logging of the dungeon saving process");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }


    public static final class tab implements TabCompleter {

        @Override
        public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
            List<String> s = new ArrayList<>();
            Player p = (Player) sender;
            if (args.length > 1) {
                switch (args[0]) {
                    case "dungeon_manager" -> {
                        return DungeonCreatorCommandExecutor.complete(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
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
            } else {
                s.addAll(List.of("dungeon_manager"));
                if (DungeonMaster.isPermitted(p)) s.addAll(List.of("debug", "permit"));
            }
            return s;
        }
    }
}
